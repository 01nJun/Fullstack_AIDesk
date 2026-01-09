package com.desk.service.chat;

import com.desk.domain.*;
import com.desk.dto.PageRequestDTO;
import com.desk.dto.PageResponseDTO;
import com.desk.dto.TicketFileDTO;
import com.desk.dto.chat.ChatMessageCreateDTO;
import com.desk.dto.chat.ChatMessageDTO;
import com.desk.dto.chat.ChatReadUpdateDTO;
import com.desk.repository.MemberRepository;
import com.desk.repository.chat.ChatMessageRepository;
import com.desk.repository.chat.ChatFileRepository;
import com.desk.repository.chat.ChatParticipantRepository;
import com.desk.repository.chat.ChatRoomRepository;
import com.desk.service.chat.ai.AiChatWordGuard;
import com.desk.service.chat.ai.AiMessageProcessor;
import com.desk.util.CustomFileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
@Transactional
public class ChatMessageServiceImpl implements ChatMessageService {
    
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final ChatFileRepository chatFileRepository;
    private final MemberRepository memberRepository;
    private final AiMessageProcessor aiMessageProcessor;
    private final AiChatWordGuard aiChatWordGuard;
    private final CustomFileUtil fileUtil;

    /**
     * [TEST MODE]
     * - true이면 욕설 감지 케이스에서 "느린 AI 정제" 대신 테스트 대본(JSON)으로 즉시 치환
     * - 운영에서는 false 유지 권장
     */
    @Value("${aichat.testMode:false}")
    private boolean aiChatTestMode;
    
    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<ChatMessageDTO> getMessages(Long roomId, String userId, PageRequestDTO pageRequestDTO) {
        // 참여자 확인
        if (!chatParticipantRepository.existsByChatRoomIdAndUserIdAndActive(roomId, userId)) {
            throw new IllegalArgumentException("User is not a participant of this room");
        }
        
        // 페이징 설정 (messageSeq 기준 내림차순)
        Pageable pageable = pageRequestDTO.getPageable("messageSeq");
        
        // 메시지 조회
        Page<ChatMessage> result = chatMessageRepository.findByChatRoomIdOrderByMessageSeqDesc(roomId, pageable);
        
        // DTO 변환
        List<ChatMessage> messages = result.getContent();
        List<Long> seqs = messages.stream()
                .map(ChatMessage::getMessageSeq)
                .filter(s -> s != null && s > 0)
                .collect(Collectors.toList());

        Map<Long, List<TicketFileDTO>> filesBySeq = new HashMap<>();
        if (!seqs.isEmpty()) {
            List<ChatFile> chatFiles = chatFileRepository.findByRoomIdAndMessageSeqIn(roomId, seqs);
            for (ChatFile f : chatFiles) {
                Long seq = f.getMessageSeq();
                if (seq == null) continue;
                filesBySeq.computeIfAbsent(seq, k -> new ArrayList<>()).add(chatFileToTicketFileDTO(f));
            }
        }

        List<ChatMessageDTO> dtoList = messages.stream()
                .map(m -> {
                    ChatMessageDTO dto = toChatMessageDTO(m);
                    List<TicketFileDTO> files = filesBySeq.get(m.getMessageSeq());
                    if (files != null) dto.setFiles(files);
                    return dto;
                })
                .collect(Collectors.toList());
        
        return PageResponseDTO.<ChatMessageDTO>withAll()
                .dtoList(dtoList)
                .pageRequestDTO(pageRequestDTO)
                .totalCount(result.getTotalElements())
                .build();
    }

    @Override
    public ChatMessageDTO sendMessage(Long roomId, ChatMessageCreateDTO createDTO, String senderId) {
        // 채팅방 확인
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Chat room not found: " + roomId));

        // 참여자 확인
        if (!chatParticipantRepository.existsByChatRoomIdAndUserIdAndActive(roomId, senderId)) {
            throw new IllegalArgumentException("User is not a participant of this room");
        }

        // ============================================================
        // 시연용 AI 필터링 (하드코딩) - 실제 AI 호출 없이 특정 입력만 변환
        // ============================================================
        String originalContent = createDTO.getContent();
        String filteredContent = originalContent;
        
        // 매핑 1
        if (originalContent.contains("아 김도현 진짜 채팅 화면 파일 첨부 아이콘 위치 이거 뭐냐?") 
            && originalContent.contains("방금 확인했는데 시안이랑 완전 다르잖아... 하 이걸 왜 마음대로 바꿔?")) {
            filteredContent = "채팅 화면 파일 첨부 아이콘 위치가 시안과 다르게 적용된 것 같습니다.\n사전 공유 없이 변경된 이유를 알고 싶어요.";
        }
        // 매핑 2
        else if (originalContent.contains("일단 기능부터 돌아가게 한 거고...ㅋㅋ") 
                 && originalContent.contains("지금 구조상으로는 그게 최선이야;")) {
            filteredContent = "기능 안정성을 우선으로 판단해 적용했습니다.\n구조적인 제약이 있어 그렇게 결정했습니다.";
        }
        // 매핑 3
        else if (originalContent.contains("아 김도현 또 디자인은 그냥 무시하고 개발 편한 대로 하네") 
                 && originalContent.contains("진짜 개답답하다 말을 해주던가")) {
            filteredContent = "디자인 기준이 충분히 반영되지 않은 것 같아 아쉽습니다.\n다음부터는 변경 전 공유가 필요할 것 같아요.";
        }
        // 매핑 4
        else if (originalContent.contains("그럼 디자인 쪽에서 일정 좀 지켜주시든가~~~") 
                 && originalContent.contains("구현해달라고 매일 재촉하는데 내가 뭘 어떻게 하라고 ㅋㅋㅋㅋㅋ")) {
            filteredContent = "일정 이슈로 공유가 늦어진 점은 제 실수입니다.\n다만 당시 상황에서는 빠른 구현이 필요했습니다.";
        }
        // 매핑 5
        else if (originalContent.contains("일정은 내 알 바 아니고 디자인 시안은 지켜야지")) {
            filteredContent = "일정 압박은 이해하지만,\n디자인 의도가 계속 반영되지 않는 느낌을 받았습니다.";
        }
        // 매핑 6
        else if (originalContent.contains("아 개열받네 그래 내가 오늘까지 준다 줘")) {
            filteredContent = "의사소통이 부족했던 점 인정합니다.\n시안 기준으로 다시 조정하겠습니다.\nPC와 모바일 모두 수정 후 오늘 중으로 공유드리겠습니다.";
        }
        // ============================================================
        // 시연용 AI 필터링 끝
        // ============================================================

        // 금칙어 감지 (공백/특수문자/줄바꿈 무시)
        boolean profanityDetected = aiChatWordGuard.containsProfanity(originalContent);
        
        // effectiveAiEnabled 결정: 사용자 토글 ON OR 금칙어 감지됨
        boolean effectiveAiEnabled = (createDTO.getAiEnabled() != null && createDTO.getAiEnabled())
                || profanityDetected;

        if (profanityDetected) {
            log.warn("[Chat] 금칙어 감지 | roomId={} | senderId={} | 언어 순화 처리 시작", roomId, senderId);
        }

        // AI 메시지 처리 (사용자 ON 또는 금칙어 감지 시)
        String finalContent = filteredContent;
        boolean ticketTrigger = false;

        // ✅ 욕설 감지 시 항상 즉시 치환 (testMode 관계없이)
        if (profanityDetected) {
            String testFiltered = aiChatWordGuard.applyTestFilter(filteredContent);
            if (testFiltered != null && !testFiltered.isBlank()) {
                finalContent = testFiltered;
                log.info("[Chat] 욕설 감지 → 치환 적용 | roomId={} | senderId={} | before='{}' | after='{}'", 
                        roomId, senderId, originalContent, testFiltered);
            } else {
                // 매칭 없으면 안전하게 기본값으로 치환 (욕이 그대로 저장/전파되는 것 방지)
                finalContent = "ㅎㅎ";
                log.info("[Chat] 욕설 감지 → 기본값 치환 | roomId={} | senderId={}", roomId, senderId);
            }
        } else if (effectiveAiEnabled) {
            // ===========================
            // [NORMAL] AI 정제 로직 (욕설 아닌 경우 + 사용자가 AI ON)
            // ===========================
            AiMessageProcessor.ProcessResult aiResult = aiMessageProcessor.processMessage(
                    originalContent,
                    true
            );
            finalContent = aiResult.getProcessedContent();
            ticketTrigger = aiResult.isTicketTrigger();
        }

        // messageSeq 생성
        Long maxSeq = chatMessageRepository.findMaxMessageSeqByChatRoomId(roomId);
        Long newSeq = maxSeq + 1;

        // 메시지 생성
        ChatMessage message = ChatMessage.builder()
                .chatRoom(room)
                .messageSeq(newSeq)
                .senderId(senderId)
                .messageType(createDTO.getMessageType() != null ? createDTO.getMessageType() : ChatMessageType.TEXT)
                .content(finalContent)
                .ticketId(createDTO.getTicketId())
                .build();

        message = chatMessageRepository.save(message);

        // 채팅방의 lastMsg 업데이트
        room.updateLastMessage(newSeq, finalContent);

        // 발신자의 lastReadSeq 업데이트
        ChatParticipant senderParticipant = chatParticipantRepository
                .findByChatRoomIdAndUserId(roomId, senderId)
                .orElse(null);

        if (senderParticipant != null) {
            senderParticipant.markRead(newSeq);
            log.info("[Chat] 발신자 자동 읽음 처리 | roomId={} | senderId={} | messageSeq={}",
                    roomId, senderId, newSeq);
        }

        // DTO 생성 시 ticketTrigger, profanityDetected 포함
        ChatMessageDTO dto = toChatMessageDTO(message);
        dto.setTicketTrigger(ticketTrigger);
        dto.setProfanityDetected(profanityDetected);

        return dto;
    }

    @Override
    public ChatMessageDTO sendMessageWithFiles(Long roomId, ChatMessageCreateDTO createDTO, List<MultipartFile> files, String senderId) {
        // 1) 먼저 기존 sendMessage 로직 그대로로 메시지 저장/AI 처리/시퀀스 생성
        ChatMessageDTO dto = sendMessage(roomId, createDTO, senderId);

        if (files == null || files.isEmpty()) {
            return dto;
        }

        // 2) 참여자 확인(추가 안전)
        if (!chatParticipantRepository.existsByChatRoomIdAndUserIdAndActive(roomId, senderId)) {
            throw new IllegalArgumentException("User is not a participant of this room");
        }

        // 3) room 타입 기반 receiver 문자열 결정 (ticket_file과 동일한 writer/receiver UX 유지 목적)
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Chat room not found: " + roomId));

        String receiver = null;
        if (room.getRoomType() == ChatRoomType.DIRECT) {
            // DIRECT: 나를 제외한 참여자 1명의 email을 receiver로 저장
            receiver = room.getParticipants().stream()
                    .map(ChatParticipant::getUserId)
                    .filter(u -> u != null && !u.equals(senderId))
                    .findFirst()
                    .orElse(null);
        }

        // 4) 파일 저장 + chat_file 기록
        List<TicketFileDTO> attached = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            if (file == null || file.isEmpty()) continue;

            String savedFileName = fileUtil.saveFile(file); // 물리 저장 (uuid.확장자)

            ChatFile chatFile = ChatFile.builder()
                    .uuid(savedFileName)
                    .fileName(file.getOriginalFilename())
                    .fileSize(file.getSize())
                    .ord(i)
                    .writer(senderId)
                    .receiver(receiver)
                    .chatRoom(room)
                    .messageSeq(dto.getMessageSeq())
                    .build();

            chatFileRepository.save(chatFile);
            attached.add(chatFileToTicketFileDTO(chatFile));
        }

        dto.setFiles(attached);

        // ✅ 파일만 보낸 메시지(텍스트 없음)인 경우: 채팅방 목록 프리뷰에 파일명이 보이도록 lastMsgContent 갱신
        // - UI 요구사항: "파일일 경우 파일명.확장자"가 최근 대화 내용으로 표시되어야 함
        String content = createDTO != null ? createDTO.getContent() : null;
        boolean hasText = content != null && !content.trim().isEmpty();
        if (!hasText && !attached.isEmpty()) {
            try {
                // 첫 번째 첨부 파일명을 프리뷰로 사용
                room.updateLastMessage(dto.getMessageSeq(), attached.get(0).getFileName());
            } catch (Exception ignore) {
                // 프리뷰 갱신 실패는 첨부 자체를 막지 않음
            }
        }

        return dto;
    }
    
    @Override
    public void markAsRead(Long roomId, ChatReadUpdateDTO readDTO, String userId) {
        // 참여자 확인
        ChatParticipant participant = chatParticipantRepository
                .findByChatRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new IllegalArgumentException("User is not a participant of this room"));
        
        // 읽음 처리
        participant.markRead(readDTO.getMessageSeq());
    }
    
    @Override
    public ChatMessageDTO createSystemMessage(Long roomId, String content, String actorId) {
        // 채팅방 확인
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Chat room not found: " + roomId));
        
        // messageSeq 생성
        Long maxSeq = chatMessageRepository.findMaxMessageSeqByChatRoomId(roomId);
        Long newSeq = maxSeq + 1;
        
        // 시스템 메시지 생성
        ChatMessage message = ChatMessage.builder()
                .chatRoom(room)
                .messageSeq(newSeq)
                .senderId(actorId != null ? actorId : "SYSTEM")
                .messageType(ChatMessageType.SYSTEM)
                .content(content)
                .build();
        
        message = chatMessageRepository.save(message);
        
        // 채팅방의 lastMsg 업데이트
        room.updateLastMessage(newSeq, content);
        
        return toChatMessageDTO(message);
    }
    
    private ChatMessageDTO toChatMessageDTO(ChatMessage message) {
        // Member 정보에서 nickname 가져오기
        String nickname = memberRepository.findById(message.getSenderId())
                .map(m -> m.getNickname())
                .orElse(message.getSenderId());
        
        return ChatMessageDTO.builder()
                .id(message.getId())
                .chatRoomId(message.getChatRoom().getId())
                .messageSeq(message.getMessageSeq())
                .senderId(message.getSenderId())
                .senderNickname(nickname)
                .messageType(message.getMessageType())
                .content(message.getContent())
                .ticketId(message.getTicketId())
                .createdAt(message.getCreatedAt())
                .ticketTrigger(false) // 기본값은 false, sendMessage에서 설정됨
                .profanityDetected(false) // 기본값은 false, sendMessage에서 설정됨
                .build();
    }

    private TicketFileDTO chatFileToTicketFileDTO(ChatFile f) {
        return TicketFileDTO.builder()
                .uuid(f.getUuid())
                .fileName(f.getFileName())
                .fileSize(f.getFileSize())
                .ord(f.getOrd())
                .createdAt(f.getCreatedAt())
                .writer(f.getWriter())
                .receiver(f.getReceiver())
                .build();
    }
}

