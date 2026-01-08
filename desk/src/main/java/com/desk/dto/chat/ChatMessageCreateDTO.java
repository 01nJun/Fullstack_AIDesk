package com.desk.dto.chat;

import com.desk.domain.ChatMessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 채팅 메시지 생성 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageCreateDTO {
    
    private ChatMessageType messageType; // TEXT, TICKET_PREVIEW, SYSTEM, FILE
    private String content;
    private Long ticketId; // TICKET_PREVIEW 타입일 때만 사용
    private Boolean aiEnabled; // AI 메시지 처리 ON/OFF (프론트엔드에서 전달)
    
    // 파일 첨부용 (FILE 타입 메시지일 때 사용)
    @Builder.Default
    private List<String> fileUuids = new ArrayList<>();
}

