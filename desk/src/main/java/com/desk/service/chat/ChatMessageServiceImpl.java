package com.desk.service.chat;

import com.desk.domain.*;
import com.desk.dto.PageRequestDTO;
import com.desk.dto.PageResponseDTO;
import com.desk.dto.chat.ChatMessageCreateDTO;
import com.desk.dto.chat.ChatMessageDTO;
import com.desk.dto.chat.ChatReadUpdateDTO;
import com.desk.repository.MemberRepository;
import com.desk.repository.chat.ChatMessageRepository;
import com.desk.repository.chat.ChatParticipantRepository;
import com.desk.repository.chat.ChatRoomRepository;
import com.desk.service.chat.ai.AiChatWordGuard;
import com.desk.service.chat.ai.AiMessageProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
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
    private final MemberRepository memberRepository;
    private final AiMessageProcessor aiMessageProcessor;
    private final AiChatWordGuard aiChatWordGuard;

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

        Pageable pageable = pageRequestDTO.getPageable("messageSeq");
        Page<ChatMessage> result = chatMessageRepository.findByChatRoomIdOrderByMessageSeqDesc(roomId, pageable);

        // N+1 방지용 룸/참여자 조회
        ChatRoom room = chatRoomRepository.findById(roomId).orElse(null);
        List<ChatParticipant> participants = room != null
                ? chatParticipantRepository.findByChatRoomIdAndStatus(roomId, ChatStatus.ACTIVE)
                : Collections.emptyList();

        Map<String, Long> lastReadSeqMap = participants.stream()
                .collect(Collectors.toMap(
                        ChatParticipant::getUserId,
                        ChatParticipant::getLastReadSeq,
                        (a, b) -> a
                ));

        List<ChatMessageDTO> dtoList = result.getContent().stream()
                .map(msg -> toChatMessageDTOOptimized(msg, userId, room, lastReadSeqMap))
                .collect(Collectors.toList());

        return PageResponseDTO.<ChatMessageDTO>withAll()
                .dtoList(dtoList)
                .pageRequestDTO(pageRequestDTO)
                .totalCount(result.getTotalElements())
                .build();
    }

    @Override
    public ChatMessageDTO sendMessage(Long roomId, ChatMessageCreateDTO createDTO, String senderId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Chat room not found: " + roomId));

        if (!chatParticipantRepository.existsByChatRoomIdAndUserIdAndActive(roomId, senderId)) {
            throw new IllegalArgumentException("User is not a participant of this room");
        }

        String originalContent = createDTO.getContent();
        String filteredContent = originalContent;

        // 티켓 미리보기 메시지(TICKET_PREVIEW)나 ticketId가 있는 메시지는 티켓 트리거 체크 제외
        boolean isTicketPreview = createDTO.getMessageType() == ChatMessageType.TICKET_PREVIEW
                || createDTO.getTicketId() != null;

        // ============================================================
        // (A) 시연용 하드코딩 필터링
        // ============================================================
        if (originalContent.contains("아 김도현 진짜 채팅 화면 파일 첨부 아이콘 위치 이거 뭐냐?")
                && originalContent.contains("방금 확인했는데 시안이랑 완전 다르잖아... 하 이걸 왜 마음대로 바꿔?")) {
            filteredContent = "채팅 화면 파일 첨부 아이콘 위치가 시안과 다르게 적용된 것 같습니다.\n사전 공유 없이 변경된 이유를 알고 싶어요.";
        } else if (originalContent.contains("일단 기능부터 돌아가게 한 거고...ㅋㅋ")
                && originalContent.contains("지금 구조상으로는 그게 최선이야;")) {
            filteredContent = "기능 안정성을 우선으로 판단해 적용했습니다.\n구조적인 제약이 있어 그렇게 결정했습니다.";
        } else if (originalContent.contains("아 김도현 또 디자인은 그냥 무시하고 개발 편한 대로 하네")
                && originalContent.contains("진짜 개답답하다 말을 해주던가")) {
            filteredContent = "디자인 기준이 충분히 반영되지 않은 것 같아 아쉽습니다.\n다음부터는 변경 전 공유가 필요할 것 같아요.";
        } else if (originalContent.contains("그럼 디자인 쪽에서 일정 좀 지켜주시든가~~~")
                && originalContent.contains("구현해달라고 매일 재촉하는데 내가 뭘 어떻게 하라고 ㅋㅋㅋㅋㅋ")) {
            filteredContent = "일정 이슈로 공유가 늦어진 점은 제 실수입니다.\n다만 당시 상황에서는 빠른 구현이 필요했습니다.";
        } else if (originalContent.contains("일정은 내 알 바 아니고 디자인 시안은 지켜야지")) {
            filteredContent = "일정 압박은 이해하지만,\n디자인 의도가 계속 반영되지 않는 느낌을 받았습니다.";
        } else if (originalContent.contains("아 개열받네 그래 내가 오늘까지 준다 줘")) {
            filteredContent = "의사소통이 부족했던 점 인정합니다.\n시안 기준으로 다시 조정하겠습니다.\nPC와 모바일 모두 수정 후 오늘 중으로 공유드리겠습니다.";
        }

        // ============================================================
        // (B) 금칙어 감지: 원문 기준
        // ============================================================
        boolean profanityDetected = aiChatWordGuard.containsProfanity(originalContent);

        boolean userAiEnabled = (createDTO.getAiEnabled() != null && createDTO.getAiEnabled());
        boolean effectiveAiEnabled = userAiEnabled || profanityDetected;

        if (profanityDetected) {
            log.warn("[Chat] 금칙어 감지 | roomId={} | senderId={} | 정제 처리 시작", roomId, senderId);
        }

        // ============================================================
        // AI/TEST 처리 결과
        // ============================================================
        String finalContent = filteredContent;
        boolean ticketTrigger = false; // 최종 트리거

        // (A) 하드코딩 키워드 트리거(티켓 미리보기면 제외)
        boolean keywordTicketTrigger = false;
        if (!isTicketPreview) {
            String lower = originalContent == null ? "" : originalContent.toLowerCase();
            if (lower.contains("티켓") || lower.contains("업무")) {
                keywordTicketTrigger = true;
            }
        }

        if (!isTicketPreview && effectiveAiEnabled) {
            // TEST MODE: 욕설 감지 시 빠른 치환
            if (aiChatTestMode && profanityDetected) {
                String testFiltered = aiChatWordGuard.applyTestFilter(filteredContent);
                if (testFiltered != null && !testFiltered.isBlank()) {
                    finalContent = testFiltered;
                    log.info("[Chat][TEST] 욕설 감지 → 테스트 대본 치환 적용 | roomId={} | senderId={}", roomId, senderId);
                } else {
                    // 욕이 그대로 저장/전파되는 것 방지용 안전치
                    finalContent = "ㅎㅎ";
                    log.info("[Chat][TEST] 욕설 감지 → 테스트 대본 미매칭(기본값 적용) | roomId={} | senderId={}", roomId, senderId);
                }
                // 테스트 모드에서는 AI 결과 트리거가 없으니 키워드 트리거만 반영
                ticketTrigger = keywordTicketTrigger;
            } else {
                // NORMAL: AI 처리
                AiMessageProcessor.ProcessResult aiResult = aiMessageProcessor.processMessage(
                        originalContent,
                        true
                );
                finalContent = aiResult.getProcessedContent();
                ticketTrigger = keywordTicketTrigger || aiResult.isTicketTrigger();
            }
        } else {
            // AI 비활성(또는 티켓 미리보기) 상태: 키워드 트리거만 반영
            ticketTrigger = keywordTicketTrigger;
        }

        // ============================================================
        // (A) 티켓 트리거 감지 시: 메시지 저장 건너뜀(상대방에게 전달 안 되게)
        // ============================================================
        if (ticketTrigger && !isTicketPreview) {
            log.info("[Chat] 티켓 트리거 감지 - 메시지 저장 건너뜀 | roomId={} | senderId={}", roomId, senderId);

            String nickname = memberRepository.findById(senderId)
                    .map(m -> m.getNickname())
                    .orElse(senderId);

            ChatMessageDTO dto = ChatMessageDTO.builder()
                    .id(null)
                    .chatRoomId(roomId)
                    .messageSeq(null)
                    .senderId(senderId)
                    .senderNickname(nickname)
                    .messageType(createDTO.getMessageType() != null ? createDTO.getMessageType() : ChatMessageType.TEXT)
                    .content(finalContent)
                    .ticketId(null)
                    .createdAt(null)
                    .ticketTrigger(true)
                    .unreadCount(null)
                    .isRead(null)
                    .profanityDetected(profanityDetected)
                    .build();

            return dto;
        }

        // ============================================================
        // 저장 로직
        // ============================================================
        Long maxSeq = chatMessageRepository.findMaxMessageSeqByChatRoomId(roomId);
        Long newSeq = maxSeq + 1;

        ChatMessage message = ChatMessage.builder()
                .chatRoom(room)
                .messageSeq(newSeq)
                .senderId(senderId)
                .messageType(createDTO.getMessageType() != null ? createDTO.getMessageType() : ChatMessageType.TEXT)
                .content(finalContent)
                .ticketId(createDTO.getTicketId())
                .build();

        message = chatMessageRepository.save(message);

        room.updateLastMessage(newSeq, finalContent);

        ChatParticipant senderParticipant = chatParticipantRepository
                .findByChatRoomIdAndUserId(roomId, senderId)
                .orElse(null);

        if (senderParticipant != null) {
            senderParticipant.markRead(newSeq);
            log.info("[Chat] 발신자 자동 읽음 처리 | roomId={} | senderId={} | messageSeq={}",
                    roomId, senderId, newSeq);
        }

        // unreadCount/isRead 계산용 참여자 맵
        List<ChatParticipant> participants = chatParticipantRepository.findByChatRoomIdAndStatus(roomId, ChatStatus.ACTIVE);
        Map<String, Long> lastReadSeqMap = participants.stream()
                .collect(Collectors.toMap(
                        ChatParticipant::getUserId,
                        ChatParticipant::getLastReadSeq,
                        (a, b) -> a
                ));

        ChatMessageDTO dto = toChatMessageDTOOptimized(message, senderId, room, lastReadSeqMap);
        dto.setTicketTrigger(ticketTrigger);
        dto.setProfanityDetected(profanityDetected);

        return dto;
    }

    @Override
    public void markAsRead(Long roomId, ChatReadUpdateDTO readDTO, String userId) {
        ChatParticipant participant = chatParticipantRepository
                .findByChatRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new IllegalArgumentException("User is not a participant of this room"));

        participant.markRead(readDTO.getMessageSeq());
    }

    @Override
    public ChatMessageDTO createSystemMessage(Long roomId, String content, String actorId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Chat room not found: " + roomId));

        Long maxSeq = chatMessageRepository.findMaxMessageSeqByChatRoomId(roomId);
        Long newSeq = maxSeq + 1;

        ChatMessage message = ChatMessage.builder()
                .chatRoom(room)
                .messageSeq(newSeq)
                .senderId(actorId != null ? actorId : "SYSTEM")
                .messageType(ChatMessageType.SYSTEM)
                .content(content)
                .build();

        message = chatMessageRepository.save(message);
        room.updateLastMessage(newSeq, content);

        // system 메시지도 unreadCount/isRead 계산 가능하게 처리
        List<ChatParticipant> participants = chatParticipantRepository.findByChatRoomIdAndStatus(roomId, ChatStatus.ACTIVE);
        Map<String, Long> lastReadSeqMap = participants.stream()
                .collect(Collectors.toMap(
                        ChatParticipant::getUserId,
                        ChatParticipant::getLastReadSeq,
                        (a, b) -> a
                ));

        ChatMessageDTO dto = toChatMessageDTOOptimized(message, actorId != null ? actorId : "SYSTEM", room, lastReadSeqMap);
        // system은 profanity/ticketTrigger 기본 false
        dto.setTicketTrigger(false);
        dto.setProfanityDetected(false);
        return dto;
    }

    private ChatMessageDTO toChatMessageDTOOptimized(
            ChatMessage message,
            String currentUserId,
            ChatRoom room,
            Map<String, Long> lastReadSeqMap
    ) {
        String nickname = memberRepository.findById(message.getSenderId())
                .map(m -> m.getNickname())
                .orElse(message.getSenderId());

        Integer unreadCount = null;
        Boolean isRead = null;

        Long messageSeq = message.getMessageSeq();
        Long currentUserLastReadSeq = lastReadSeqMap.getOrDefault(currentUserId, 0L);

        if (message.getSenderId().equals(currentUserId)) {
            if (room != null && room.getRoomType() == ChatRoomType.DIRECT) {
                String otherUserId = lastReadSeqMap.keySet().stream()
                        .filter(id -> !id.equals(currentUserId))
                        .findFirst()
                        .orElse(null);

                if (otherUserId != null) {
                    Long otherLastReadSeq = lastReadSeqMap.getOrDefault(otherUserId, 0L);
                    unreadCount = (otherLastReadSeq < messageSeq) ? 1 : 0;
                } else {
                    unreadCount = 0;
                }
            } else {
                long unreadCountLong = lastReadSeqMap.entrySet().stream()
                        .filter(entry -> !entry.getKey().equals(currentUserId))
                        .filter(entry -> entry.getValue() < messageSeq)
                        .count();
                unreadCount = (int) unreadCountLong;
            }
        } else {
            isRead = currentUserLastReadSeq >= messageSeq;
        }

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
                .ticketTrigger(false)
                .unreadCount(unreadCount)
                .isRead(isRead)
                .profanityDetected(false)
                .build();
    }
}
