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

import java.util.List;
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
        
        // DTO 변환
        List<ChatMessageDTO> dtoList = result.getContent().stream()
                .map(this::toChatMessageDTO)
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

        // AI 메시지 처리 (선택적)
        String finalContent = filteredContent;
        boolean ticketTrigger = false;

        if (createDTO.getAiEnabled() != null && createDTO.getAiEnabled()) {
            AiMessageProcessor.ProcessResult aiResult = aiMessageProcessor.processMessage(
                    createDTO.getContent(),
                    createDTO.getAiEnabled()
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

        // DTO 생성 시 ticketTrigger 포함
        ChatMessageDTO dto = toChatMessageDTO(message);
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
                .build();
    }
}

