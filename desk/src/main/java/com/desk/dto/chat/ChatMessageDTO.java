package com.desk.dto.chat;

import com.desk.domain.ChatMessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 채팅 메시지 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDTO {
    
    private Long id;
    private Long chatRoomId;
    private Long messageSeq;
    private String senderId;
    private String senderNickname; // Member 정보에서 가져옴
    private ChatMessageType messageType;
    private String content;
    private Long ticketId; // TICKET_PREVIEW 타입일 때만 사용
    private LocalDateTime createdAt;
    private Boolean ticketTrigger; // 티켓 생성 문맥 감지 여부 (AI 처리 시)
    private Boolean profanityDetected; // 금칙어 감지 여부 (자동 AI 적용)
    private Integer unreadCount; // 해당 메시지를 읽지 않은 참여자 수 (1:1 채팅은 0 또는 1, 그룹 채팅은 0 이상)
    private Boolean isRead; // 현재 사용자가 해당 메시지를 읽었는지 여부 (받은 메시지에만 해당)
}

