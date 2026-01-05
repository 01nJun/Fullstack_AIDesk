package com.desk.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 읽음 처리 요청 DTO
 * messageSeq까지 읽음 처리
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatReadUpdateDTO {
    
    private Long messageSeq; // 읽은 메시지의 messageSeq
}


