package com.desk.dto.chat;

import com.desk.domain.ChatStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 채팅 참여자 정보 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatParticipantDTO {
    
    private Long id;
    private Long chatRoomId;
    private String userId;
    private String nickname; // Member 정보에서 가져옴
    private ChatStatus status;
    private Long lastReadSeq;
    private LocalDateTime joinedAt;
    private LocalDateTime leftAt;
}


