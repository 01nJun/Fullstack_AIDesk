package com.desk.service.chat;

import com.desk.dto.PageRequestDTO;
import com.desk.dto.PageResponseDTO;
import com.desk.dto.chat.ChatMessageCreateDTO;
import com.desk.dto.chat.ChatMessageDTO;
import com.desk.dto.chat.ChatReadUpdateDTO;

/**
 * 채팅 메시지 관련 비즈니스 로직 인터페이스
 */
public interface ChatMessageService {
    
    /**
     * 채팅방 메시지 목록 조회 (페이징)
     */
    PageResponseDTO<ChatMessageDTO> getMessages(Long roomId, String userId, PageRequestDTO pageRequestDTO);
    
    /**
     * 메시지 전송 (REST API용)
     */
    ChatMessageDTO sendMessage(Long roomId, ChatMessageCreateDTO createDTO, String senderId);
    
    /**
     * 읽음 처리
     */
    void markAsRead(Long roomId, ChatReadUpdateDTO readDTO, String userId);
    
    /**
     * 시스템 메시지 생성 (입장/퇴장/초대 등)
     */
    ChatMessageDTO createSystemMessage(Long roomId, String content, String actorId);
}


