package com.desk.dto.chat;

import com.desk.domain.ChatMessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 채팅 메시지 생성 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageCreateDTO {
    
    private ChatMessageType messageType; // TEXT, TICKET_PREVIEW, SYSTEM
    private String content;
    private Long ticketId; // TICKET_PREVIEW 타입일 때만 사용
    private Boolean aiEnabled; // AI 메시지 처리 ON/OFF (프론트엔드에서 전달)
}

