package com.desk.dto.chat;

import com.desk.domain.ChatRoomType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 채팅방 정보 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomDTO {
    
    private Long id;
    private ChatRoomType roomType;
    private String pairKey;
    private String name;
    private String lastMsgContent;
    private LocalDateTime lastMsgAt;
    private Long lastMsgSeq;
    private LocalDateTime createdAt;
    
    // 참여자 정보
    private List<ChatParticipantDTO> participants;
    
    // 안 읽은 메시지 개수 (클라이언트에서 계산하거나 서버에서 제공)
    private Long unreadCount;
}


