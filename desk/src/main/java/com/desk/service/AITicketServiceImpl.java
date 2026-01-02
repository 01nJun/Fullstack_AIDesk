package com.desk.service;

import com.desk.domain.Member;
import com.desk.dto.AITicketRequestDTO;
import com.desk.dto.AITicketRequestDTO.AITicketInfo;
import com.desk.dto.AITicketResponseDTO;
import com.desk.repository.MemberRepository;
import com.desk.util.AITicketPromptUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional; // [중요] java.util 패키지 선택

@Service
@Log4j2
@RequiredArgsConstructor
public class AITicketServiceImpl implements AITicketService {

    private final AITicketClientService aiClient; // Ollama 통신
    private final AITicketRAGService ragService;  // 지식 검색
    private final MemberRepository memberRepository; // 담당자 DB 조회
    private final ObjectMapper objectMapper;      // JSON 파싱

    @Override
    public AITicketResponseDTO processRequest(AITicketRequestDTO request) {
        String userInput = request.getUserInput();
        AITicketInfo currentTicket = request.getCurrentTicket();
        String targetDept = request.getTargetDept();

        log.info("[AI Logic] Processing... User Input: {}", userInput);

        // ------------------------------------------------------------------
        // [Step 1] 부서 라우팅 (Routing)
        // ------------------------------------------------------------------
        if (targetDept == null || targetDept.isEmpty()) {
            log.info("[Step 1] Routing Start");
            
            String prompt = AITicketPromptUtil.getRoutingPrompt(userInput);
            String aiResult = aiClient.generateText(prompt).trim();

            // "QUESTION:" 으로 시작하는 경우 (AI가 되묻는 경우)
            if (aiResult.contains("QUESTION:")) {
                String question = aiResult.substring(aiResult.indexOf("QUESTION:") + 9).trim();
                return createResponse(request, question, null, false);
            }

            // 부서명 추출
            String identifiedDept = extractDeptName(aiResult);
            if (identifiedDept == null) {
                return createResponse(request, "죄송합니다. 말씀하신 내용만으로는 어느 부서 업무인지 파악하기 어렵습니다. 조금 더 구체적으로 말씀해 주시겠어요?", null, false);
            }

            // 정상 라우팅 완료
            String msg = String.format("네, 말씀하신 내용은 **[%s]** 부서 업무로 확인됩니다.\n\n혹시 해당 부서에 지정해서 요청하실 **담당자**분이 계신가요?\n(없으시면 '없음'이라고 말씀해 주세요.)", identifiedDept);
            return createResponse(request, msg, identifiedDept, false);
        }

        // ------------------------------------------------------------------
        // [Step 2] 담당자 확인 (Assignee Check)
        // ------------------------------------------------------------------
        if (currentTicket.getReceivers() == null || currentTicket.getReceivers().isEmpty()) {
            log.info("[Step 2] Assignee Check for {}", targetDept);

            String prompt = AITicketPromptUtil.getAssigneePrompt(userInput);
            // 따옴표 및 공백 제거
            String extractedName = aiClient.generateText(prompt).trim().replace("\"", "").replace("'", "");

            List<String> finalReceivers = new ArrayList<>();
            String aiMsg = "";

            // 1. 공통/없음/모름 -> 부서 공통 계정 또는 프론트엔드 처리 유도
            if (isCommonTeam(extractedName)) {
                aiMsg = String.format("네, 특정 담당자가 지정되지 않아 **[%s]** 부서원 전체에게 알림을 보낼 예정입니다.", targetDept);
                finalReceivers.add("TEAM_" + targetDept); 
            } 
            // 2. 특정 이름(닉네임) -> 실제 DB 조회
            else {
                // MemberRepository에 추가한 findByNickname 호출
                Optional<Member> foundMember = memberRepository.findByNickname(extractedName);
                
                if (foundMember.isPresent()) {
                    String email = foundMember.get().getEmail();
                    aiMsg = String.format("네, 담당자 **%s**님(%s)을 지정했습니다.", extractedName, email);
                    finalReceivers.add(email); // 이메일 저장
                } else {
                    aiMsg = String.format("죄송합니다. **%s**님을 찾을 수 없습니다. 정확한 이름을 다시 말씀해 주시거나, 없으면 '없음'이라고 해주세요.", extractedName);
                    // 리스트를 비워두어 다시 질문하도록 유도
                }
            }

            currentTicket.setReceivers(finalReceivers);
            
            // 만약 담당자를 못 찾았으면 여기서 바로 리턴
            if (finalReceivers.isEmpty()) {
                return createResponse(request, aiMsg, targetDept, false);
            }

            aiMsg += "\n\n이제 요청하실 업무 내용을 구체적으로 말씀해 주세요.";
            
            AITicketResponseDTO resp = createResponse(request, aiMsg, targetDept, false);
            resp.setUpdatedTicket(currentTicket);
            return resp;
        }

        // ------------------------------------------------------------------
        // [Step 3] 심층 인터뷰 (Interview & RAG)
        // ------------------------------------------------------------------
        log.info("[Step 3] Interview for {}", targetDept);

        String ragContext = ragService.searchContext(targetDept, userInput);
        List<String> missingFields = validateTicket(currentTicket);
        
        String missingInfoInstruction = missingFields.isEmpty() 
                ? "모든 필수 정보가 입력되었습니다. 티켓 내용을 최종 정리하고 사용자가 확인할 수 있게 하십시오."
                : String.format("현재 %s 정보가 누락되었습니다. 이 내용을 반드시 질문하십시오.", String.join(", ", missingFields));

        try {
            // 현재 상태 JSON 변환
            String ticketJson = objectMapper.writeValueAsString(currentTicket);
            
            String prompt = AITicketPromptUtil.getInterviewPrompt(
                    targetDept, missingInfoInstruction, ragContext, ticketJson, userInput
            );

            // AI 호출 (JSON 응답 요청)
            String jsonResult = aiClient.generateJson(prompt);
            
            // AI 응답 파싱
            JsonNode rootNode = objectMapper.readTree(jsonResult);
            
            String responseToUser = rootNode.path("responseToUser").asText(); // PromptUtil에서 수정한 CamelCase 키 사용
            JsonNode updatedTicketNode = rootNode.path("updatedTicket");

            // JSON -> DTO 변환
            AITicketInfo finalTicket = objectMapper.treeToValue(updatedTicketNode, AITicketInfo.class);
            
            // 기존 담당자 정보 보존 (AI가 가끔 담당자를 누락하는 경우 방지)
            if ((finalTicket.getReceivers() == null || finalTicket.getReceivers().isEmpty()) 
                    && (currentTicket.getReceivers() != null && !currentTicket.getReceivers().isEmpty())) {
                finalTicket.setReceivers(currentTicket.getReceivers());
            }

            // 최종 완료 여부 재검사
            List<String> finalMissing = validateTicket(finalTicket);
            boolean isReallyCompleted = finalMissing.isEmpty();
            
            if (isReallyCompleted) {
                finalTicket.setCompletionRate(100);
                responseToUser += "\n\n✅ **필수 정보가 모두 확인되었습니다. 우측의 [업무 티켓 전송] 버튼을 눌러주세요.**";
            } else {
                if (finalTicket.getCompletionRate() >= 100) finalTicket.setCompletionRate(90);
            }

            AITicketResponseDTO response = createResponse(request, responseToUser, targetDept, isReallyCompleted);
            response.setUpdatedTicket(finalTicket);
            response.setMissingInfoList(finalMissing);
            response.setNextAction(isReallyCompleted ? "suggest_submit" : "continue_chat");
            
            return response;

        } catch (JsonProcessingException e) {
            log.error("JSON Parsing Error during Interview: {}", e.getMessage());
            // 파싱 실패 시 사용자에게 재시도 요청
            return createResponse(request, "죄송합니다. AI 응답을 처리하는 중 기술적인 오류가 발생했습니다. 다시 한번 말씀해 주시겠어요?", targetDept, false);
        }
    }

    // [Helper] 응답 생성기
    private AITicketResponseDTO createResponse(AITicketRequestDTO req, String msg, String dept, boolean isCompleted) {
        return AITicketResponseDTO.builder()
                .conversationId(req.getConversationId())
                .aiMessage(msg)
                .updatedTicket(req.getCurrentTicket())
                .identifiedTargetDept(dept != null ? dept : req.getTargetDept())
                .isCompleted(isCompleted)
                .nextAction("continue_chat")
                .missingInfoList(new ArrayList<>())
                .build();
    }

    // [Helper] 부서명 추출
    private String extractDeptName(String text) {
        if (text == null) return null;
        String upper = text.toUpperCase();
        if (upper.contains("DEVELOPMENT")) return "DEVELOPMENT";
        if (upper.contains("DESIGN")) return "DESIGN";
        if (upper.contains("SALES")) return "SALES";
        if (upper.contains("HR")) return "HR";
        if (upper.contains("FINANCE")) return "FINANCE";
        if (upper.contains("PLANNING")) return "PLANNING";
        return null;
    }

    // [Helper] 공통 팀 여부 확인
    private boolean isCommonTeam(String name) {
        if (name == null) return true;
        String upper = name.toUpperCase();
        return upper.contains("TEAM_COMMON") || upper.contains("없음") || upper.contains("모름") || upper.contains("NONE") || upper.contains("상관없음");
    }

    // [Helper] 티켓 필수값 검증
    private List<String> validateTicket(AITicketInfo t) {
        List<String> missing = new ArrayList<>();
        if (t.getTitle() == null || t.getTitle().length() < 2) missing.add("제목");
        if (t.getContent() == null || t.getContent().length() < 2) missing.add("요약");
        // 상세내용(requirement)은 필수 아님 (필요 시 주석 해제)
        // if (t.getRequirement() == null || t.getRequirement().length() < 2) missing.add("상세내용");
        if (t.getDeadline() == null || t.getDeadline().isEmpty()) missing.add("마감일");
        return missing;
    }
}