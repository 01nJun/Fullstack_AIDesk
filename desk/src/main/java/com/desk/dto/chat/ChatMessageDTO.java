package com.desk.dto.chat;

import com.desk.domain.ChatMessageType;
import com.desk.dto.TicketFileDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    /**
     * 채팅 파일 첨부 목록
     * - 파일함(ticket_file)과 동일 DTO 재사용(uuid/fileName/fileSize/createdAt/writer/receiver)
     */
    @Builder.Default
    private List<TicketFileDTO> files = new ArrayList<>();
}

