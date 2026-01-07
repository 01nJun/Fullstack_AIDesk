package com.desk.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 그룹 채팅방 생성 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomCreateDTO {
    
    private String name; // 그룹 채팅방 이름
    private List<String> userIds; // 초대할 사용자 이메일 목록
}


