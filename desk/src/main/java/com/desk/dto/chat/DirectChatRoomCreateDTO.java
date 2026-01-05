package com.desk.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 1:1 채팅방 생성 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DirectChatRoomCreateDTO {
    
    private String targetUserId; // 상대방 이메일
}


