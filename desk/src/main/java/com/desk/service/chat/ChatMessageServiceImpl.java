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
        
        // AI 메시지 처리 (선택적)
        String finalContent = createDTO.getContent();
        boolean ticketTrigger = false;
        
        // AI 처리 수행 (서버 설정 및 프론트엔드 요청에 따라)
        if (createDTO.getAiEnabled() != null && createDTO.getAiEnabled()) {
            AiMessageProcessor.ProcessResult aiResult = aiMessageProcessor.processMessage(
                    createDTO.getContent(), 
                    createDTO.getAiEnabled()
            );
            finalContent = aiResult.getProcessedContent();
            ticketTrigger = aiResult.isTicketTrigger();
            
            // 원문 메시지 참조 제거 (가비지 컬렉션 대상)
            // finalContent는 이미 처리된 메시지이므로 원문과 분리됨
        }
        
        // messageSeq 생성
        Long maxSeq = chatMessageRepository.findMaxMessageSeqByChatRoomId(roomId);
        Long newSeq = maxSeq + 1;
        
        // 메시지 생성 (AI 처리된 content 사용)
        ChatMessage message = ChatMessage.builder()
                .chatRoom(room)
                .messageSeq(newSeq)
                .senderId(senderId)
                .messageType(createDTO.getMessageType() != null ? createDTO.getMessageType() : ChatMessageType.TEXT)
                .content(finalContent) // AI 처리된 메시지 또는 원문
                .ticketId(createDTO.getTicketId())
                .build();
        
        message = chatMessageRepository.save(message);
        
        // 채팅방의 lastMsg 업데이트 (AI 처리된 메시지 사용)
        room.updateLastMessage(newSeq, finalContent);
        
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

