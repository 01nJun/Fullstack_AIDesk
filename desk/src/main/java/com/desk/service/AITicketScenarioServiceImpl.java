package com.desk.service;

import com.desk.dto.AITicketRequestDTO;
import com.desk.dto.AITicketRequestDTO.AITicketInfo;
import com.desk.dto.AITicketResponseDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * [시연용 데모 시나리오]
 * - ai.demo-mode.enabled=true 일 때만 동작
 * - resources/data/design_scenario.json 기반으로 user_input을 매칭하여 응답을 즉시 반환
 */
@Service
@Log4j2
@RequiredArgsConstructor
public class AITicketScenarioServiceImpl implements AITicketScenarioService {

    private final ObjectMapper objectMapper;

    @Value("${ai.demo-mode.scenario-path:data/design_scenario.json}")
    private String scenarioPath;

    private JsonNode root;

    /**
     * 대화 진행 상태 저장 (conversationId 단위)
     */
    private final Map<String, ScenarioSession> sessions = new ConcurrentHashMap<>();

    private static class ScenarioSession {
        String stage = "DEPT";      // DEPT -> ASSIGNEE -> TASK -> REQUIREMENT -> DEADLINE
        String targetDept;          // DESIGN 등
        String assigneeName;        // {ASSIGNEE} 치환용
        String lastPromptTemplate;  // 현재 단계에서 다시 물어볼 질문(템플릿)
    }

    @PostConstruct
    public void load() {
        try {
            String path = (scenarioPath == null || scenarioPath.isBlank())
                    ? "data/design_scenario.json"
                    : scenarioPath;
            scenarioPath = path;

            Resource res = new ClassPathResource(Objects.requireNonNull(path));
            if (!res.exists()) {
                log.warn("[AI Scenario] scenario file not found: {}", scenarioPath);
                root = objectMapper.createObjectNode().putArray("scenarios");
                return;
            }
            try (InputStream is = res.getInputStream()) {
                root = objectMapper.readTree(is);
            }
            log.info("[AI Scenario] loaded: {}", scenarioPath);
        } catch (Exception e) {
            log.error("[AI Scenario] load failed: {}", e.getMessage());
            root = objectMapper.createObjectNode().putArray("scenarios");
        }
    }

    @Override
    public AITicketResponseDTO tryHandleScenario(AITicketRequestDTO request) {
        if (request == null) return null;
        if (root == null) return null;

        String convId = request.getConversationId();
        if (convId == null || convId.isBlank()) return null;

        String userInput = request.getUserInput() == null ? "" : request.getUserInput();
        String normalizedUserInput = normalize(userInput);
        if (normalizedUserInput.isEmpty()) return null;

        JsonNode scenarios = root.path("scenarios");
        if (!scenarios.isArray()) return null;

        ScenarioSession session = sessions.computeIfAbsent(convId, k -> new ScenarioSession());

        // stage 보정 (프론트에서 targetDept/receivers가 이미 넘어오면)
        if ((session.targetDept == null || session.targetDept.isBlank())
                && request.getTargetDept() != null && !request.getTargetDept().isBlank()) {
            session.targetDept = request.getTargetDept();
        }
        if ("DEPT".equalsIgnoreCase(session.stage)
                && session.targetDept != null && !session.targetDept.isBlank()) {
            session.stage = "ASSIGNEE";
        }
        if ("ASSIGNEE".equalsIgnoreCase(session.stage)
                && request.getCurrentTicket() != null
                && request.getCurrentTicket().getReceivers() != null
                && !request.getCurrentTicket().getReceivers().isEmpty()) {
            session.stage = "TASK";
        }

        // 1) 현재 단계(stage)에서만 먼저 찾는다 (공백 무시 + 완전일치)
        JsonNode matchedInStage = findMatch(scenarios, session.stage, session.targetDept, normalizedUserInput);

        // 2) 현재 단계에 없으면, 다른 단계라도 매칭되는지(out-of-order) 확인
        //    - 단, DEPT 단계에서는 out-of-order를 허용하지 않는다.
        JsonNode matchedAny = null;
        if (matchedInStage == null && !"DEPT".equalsIgnoreCase(session.stage)) {
            matchedAny = findMatchAnyStage(scenarios, session.targetDept, normalizedUserInput);
        }

        JsonNode selected = matchedInStage != null ? matchedInStage : matchedAny;
        if (selected == null) return null; // 시나리오에 없으면 AI로 넘어가게(null)

        // ticketUpdate merge
        AITicketInfo merged = mergeTicket(request.getCurrentTicket(), selected.path("ticketUpdate"));

        // 부서 세팅(부서 배치 단계)
        String identifiedDept = selected.path("identifiedTargetDept").asText(null);
        if (identifiedDept != null && !identifiedDept.isBlank()) {
            session.targetDept = identifiedDept;
        }

        // 담당자명 세팅(치환용)
        String assigneeName = selected.path("assigneeName").asText(null);
        if (assigneeName != null && !assigneeName.isBlank()) {
            session.assigneeName = assigneeName;
        }

        // 단계 이동은 "현재 단계 매칭"일 때만 적용
        if (matchedInStage != null) {
            String nextStage = selected.path("nextStage").asText(null);
            if (nextStage != null && !nextStage.isBlank()) {
                session.stage = nextStage;
            }
        }

        String aiMessage = selected.path("aiMessage").asText("시나리오 응답입니다.");
        aiMessage = applyTemplate(aiMessage, session);

        // out-of-order 매칭이면: 현재 단계 질문을 다시 한다(저장된 lastPromptTemplate)
        if (matchedInStage == null && matchedAny != null
                && session.lastPromptTemplate != null && !session.lastPromptTemplate.isBlank()) {
            aiMessage = aiMessage + "\n\n" + applyTemplate(session.lastPromptTemplate, session);
        }

        // lastPromptTemplate 업데이트 (현재 단계에서 다시 물어볼 문구)
        String setPrompt = selected.path("setLastPrompt").asText(null);
        if (setPrompt != null && !setPrompt.isBlank()) {
            session.lastPromptTemplate = setPrompt;
        }

        boolean isCompleted = selected.path("isCompleted").asBoolean(false);
        String nextAction = selected.path("nextAction").asText(isCompleted ? "suggest_submit" : "continue_chat");
        
        // 완료 시 요약란(content) 자동 생성 (제목, 마감기한, 중요도, 요청사항 포함, 목적 제외)
        if (isCompleted && merged != null) {
            String summary = buildSummary(merged);
            if (summary != null && !summary.isBlank()) {
                merged.setContent(summary);
            }
        }

        AITicketResponseDTO resp = AITicketResponseDTO.builder()
                .conversationId(convId)
                .aiMessage(aiMessage)
                .identifiedTargetDept(session.targetDept != null ? session.targetDept : request.getTargetDept())
                .isCompleted(isCompleted)
                .nextAction(nextAction)
                .missingInfoList(new ArrayList<>())
                .build();
        resp.setUpdatedTicket(merged);
        return resp;
    }

    private JsonNode findMatch(JsonNode scenarios, String stage, String dept, String normalizedUserInput) {
        for (JsonNode sc : scenarios) {
            String scStage = sc.path("stage").asText(null);
            if (scStage == null || !scStage.equalsIgnoreCase(stage)) continue;

            String scDept = sc.path("dept").asText(null);
            if (scDept != null && !scDept.isBlank()) {
                if (dept == null || dept.isBlank()) continue;
                if (!scDept.equalsIgnoreCase(dept)) continue;
            }

            String trig = sc.path("trigger").asText(null);
            if (trig == null) continue;
            if (normalize(trig).equals(normalizedUserInput)) return sc;
        }
        return null;
    }

    private JsonNode findMatchAnyStage(JsonNode scenarios, String dept, String normalizedUserInput) {
        for (JsonNode sc : scenarios) {
            String scDept = sc.path("dept").asText(null);
            if (scDept != null && !scDept.isBlank()) {
                if (dept == null || dept.isBlank()) continue;
                if (!scDept.equalsIgnoreCase(dept)) continue;
            }

            String trig = sc.path("trigger").asText(null);
            if (trig == null) continue;
            if (normalize(trig).equals(normalizedUserInput)) return sc;
        }
        return null;
    }

    /**
     * 트리거 키 매칭 규칙:
     * - 공백/개행/탭 제거
     * - 영문 대비 소문자
     */
    private String normalize(String s) {
        if (s == null) return "";
        return s.replaceAll("\\s+", "").toLowerCase();
    }

    private String applyTemplate(String msg, ScenarioSession session) {
        if (msg == null) return null;
        String assignee = (session.assigneeName == null || session.assigneeName.isBlank())
                ? "담당자"
                : session.assigneeName;
        return msg.replace("{ASSIGNEE}", assignee);
    }

    private AITicketInfo mergeTicket(AITicketInfo base, JsonNode ticketUpdateNode) {
        AITicketInfo safeBase = base != null ? base : AITicketInfo.builder().build();
        if (ticketUpdateNode == null || ticketUpdateNode.isMissingNode() || ticketUpdateNode.isNull() || !ticketUpdateNode.isObject()) {
            return safeBase;
        }
        ObjectNode baseNode = objectMapper.valueToTree(safeBase);
        ticketUpdateNode.fields().forEachRemaining(e -> baseNode.set(e.getKey(), e.getValue()));
        return objectMapper.convertValue(baseNode, AITicketInfo.class);
    }
    
    /**
     * 요약란 생성: 제목, 마감기한, 중요도, 요청사항 포함 (목적 제외)
     */
    private String buildSummary(AITicketInfo ticket) {
        if (ticket == null) return null;
        StringBuilder sb = new StringBuilder();
        
        // 제목
        if (ticket.getTitle() != null && !ticket.getTitle().isBlank()) {
            sb.append("**").append(ticket.getTitle()).append("**\n\n");
        }
        
        // 요청사항
        if (ticket.getRequirement() != null && !ticket.getRequirement().isBlank()) {
            sb.append("[요청사항]\n").append(ticket.getRequirement()).append("\n\n");
        }
        
        // 마감기한
        if (ticket.getDeadline() != null && !ticket.getDeadline().isBlank()) {
            sb.append("[마감기한]\n").append(ticket.getDeadline()).append(" 까지 필히 완료 요망\n\n");
        }
        
        // 중요도
        if (ticket.getGrade() != null) {
            String gradeText = switch (ticket.getGrade().toString()) {
                case "URGENT" -> "긴급";
                case "HIGH" -> "높음";
                case "MIDDLE" -> "보통";
                case "LOW" -> "낮음";
                default -> ticket.getGrade().toString();
            };
            sb.append("[중요도]\n").append(gradeText);
        }
        
        return sb.toString().trim();
    }
}


