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
import com.desk.service.chat.ai.AiMessageProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
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
        
        // 채팅방 정보 및 참여자 정보 한 번에 조회 (N+1 방지)
        ChatRoom room = chatRoomRepository.findById(roomId).orElse(null);
        List<ChatParticipant> participants = room != null 
                ? chatParticipantRepository.findByChatRoomIdAndStatus(roomId, ChatStatus.ACTIVE)
                : Collections.emptyList();
        
        // 각 참여자의 lastReadSeq를 Map으로 저장 (조회 최적화)
        Map<String, Long> lastReadSeqMap = participants.stream()
                .collect(Collectors.toMap(
                        ChatParticipant::getUserId,
                        ChatParticipant::getLastReadSeq,
                        (a, b) -> a
                ));
        
        // DTO 변환 (메모리에서 계산)
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
        boolean ticketTrigger = false;
        
        // 티켓 미리보기 메시지(TICKET_PREVIEW)나 이미 ticketId가 있는 메시지는 티켓 트리거 체크를 건너뜀
        boolean isTicketPreview = createDTO.getMessageType() == ChatMessageType.TICKET_PREVIEW 
                || createDTO.getTicketId() != null;
        
        // 티켓 생성 트리거 체크 (하드코딩) - 티켓 미리보기가 아닌 경우에만
        if (!isTicketPreview) {
            String lowerContent = originalContent.toLowerCase();
            if (lowerContent.contains("티켓") || lowerContent.contains("업무")) {
                ticketTrigger = true;
            }
        }
        
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

        // AI 메시지 처리 (선택적) - 티켓 미리보기가 아닌 경우에만
        String finalContent = filteredContent;

        if (!isTicketPreview && createDTO.getAiEnabled() != null && createDTO.getAiEnabled()) {
            AiMessageProcessor.ProcessResult aiResult = aiMessageProcessor.processMessage(
                    createDTO.getContent(),
                    createDTO.getAiEnabled()
            );
            finalContent = aiResult.getProcessedContent();
            // 하드코딩된 트리거 또는 AI 트리거 중 하나라도 true면 티켓 트리거
            ticketTrigger = ticketTrigger || aiResult.isTicketTrigger();
        }

        // 티켓 트리거가 감지되고 티켓 미리보기가 아닌 경우에만 메시지를 DB에 저장하지 않고 티켓 트리거만 반환
        if (ticketTrigger && !isTicketPreview) {
            log.info("[Chat] 티켓 트리거 감지 - 메시지 저장 건너뜀 | roomId={} | senderId={}", roomId, senderId);
            
            // 채팅방 정보 및 참여자 정보 조회 (unreadCount 계산용)
            List<ChatParticipant> participants = chatParticipantRepository.findByChatRoomIdAndStatus(roomId, ChatStatus.ACTIVE);
            Map<String, Long> lastReadSeqMap = participants.stream()
                    .collect(Collectors.toMap(
                            ChatParticipant::getUserId,
                            ChatParticipant::getLastReadSeq,
                            (a, b) -> a
                    ));
            
            // 티켓 트리거만 포함한 DTO 생성 (실제 메시지는 저장하지 않음)
            String nickname = memberRepository.findById(senderId)
                    .map(m -> m.getNickname())
                    .orElse(senderId);
            
            ChatMessageDTO dto = ChatMessageDTO.builder()
                    .id(null) // DB에 저장되지 않았으므로 null
                    .chatRoomId(roomId)
                    .messageSeq(null) // DB에 저장되지 않았으므로 null
                    .senderId(senderId)
                    .senderNickname(nickname)
                    .messageType(createDTO.getMessageType() != null ? createDTO.getMessageType() : ChatMessageType.TEXT)
                    .content(finalContent) // 필터링된 내용은 포함하되 DB에는 저장하지 않음
                    .ticketId(null)
                    .createdAt(null) // DB에 저장되지 않았으므로 null
                    .ticketTrigger(true) // 티켓 트리거 설정
                    .unreadCount(null)
                    .isRead(null)
                    .build();

            return dto;
        }

        // 티켓 트리거가 아닌 경우에만 메시지 저장
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

        // 채팅방 정보 및 참여자 정보 조회 (unreadCount 계산용)
        List<ChatParticipant> participants = chatParticipantRepository.findByChatRoomIdAndStatus(roomId, ChatStatus.ACTIVE);
        Map<String, Long> lastReadSeqMap = participants.stream()
                .collect(Collectors.toMap(
                        ChatParticipant::getUserId,
                        ChatParticipant::getLastReadSeq,
                        (a, b) -> a
                ));
        
        // DTO 생성 시 ticketTrigger 포함
        ChatMessageDTO dto = toChatMessageDTOOptimized(message, senderId, room, lastReadSeqMap);
        dto.setTicketTrigger(ticketTrigger);

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
        
        // 참여자 정보 조회 (unreadCount 계산용)
        List<ChatParticipant> participants = chatParticipantRepository.findByChatRoomIdAndStatus(roomId, ChatStatus.ACTIVE);
        Map<String, Long> lastReadSeqMap = participants.stream()
                .collect(Collectors.toMap(
                        ChatParticipant::getUserId,
                        ChatParticipant::getLastReadSeq,
                        (a, b) -> a
                ));
        
        return toChatMessageDTOOptimized(message, actorId != null ? actorId : "SYSTEM", room, lastReadSeqMap);
    }
    
    /**
     * ChatMessage를 DTO로 변환 (unreadCount 미포함, 호환성을 위해 유지)
     */
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
                .unreadCount(null) // unreadCount는 별도 계산 필요
                .build();
    }
    
    /**
     * ChatMessage를 DTO로 변환 (unreadCount 포함, 최적화된 버전)
     * @param message 변환할 메시지
     * @param currentUserId 현재 사용자 ID
     * @param room 채팅방 정보
     * @param lastReadSeqMap 참여자별 lastReadSeq 맵 (N+1 방지용)
     */
    private ChatMessageDTO toChatMessageDTOOptimized(
            ChatMessage message, 
            String currentUserId, 
            ChatRoom room,
            Map<String, Long> lastReadSeqMap
    ) {
        // Member 정보에서 nickname 가져오기
        String nickname = memberRepository.findById(message.getSenderId())
                .map(m -> m.getNickname())
                .orElse(message.getSenderId());
        
        Integer unreadCount = null;
        Boolean isRead = null;
        
        Long messageSeq = message.getMessageSeq();
        Long currentUserLastReadSeq = lastReadSeqMap.getOrDefault(currentUserId, 0L);
        
        // 내가 보낸 메시지인 경우: unreadCount 계산
        if (message.getSenderId().equals(currentUserId)) {
            if (room.getRoomType() == ChatRoomType.DIRECT) {
                // 1:1 채팅: 상대방이 읽지 않았으면 1, 읽었으면 0
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
                // 그룹 채팅: 읽지 않은 참여자 수 계산 (발신자 제외)
                long unreadCountLong = lastReadSeqMap.entrySet().stream()
                        .filter(entry -> !entry.getKey().equals(currentUserId))
                        .filter(entry -> entry.getValue() < messageSeq)
                        .count();
                unreadCount = (int) unreadCountLong;
            }
        } else {
            // 받은 메시지인 경우: 내가 읽었는지 여부 계산
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
                .ticketTrigger(false) // 기본값은 false, sendMessage에서 설정됨
                .unreadCount(unreadCount)
                .isRead(isRead)
                .build();
    }
}

