package com.desk.dto.chat;

import lombok.*;

import java.time.LocalDateTime;

/**
 * 채팅 파일 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatFileDTO {
    private String uuid; // PK (파일 저장명과 동일)
    private String fileName; // 실제 파일명
    private Long fileSize;
    private String mimeType;
    private String ext;
    private LocalDateTime createdAt;
    private String downloadUrl;
    private String viewUrl;
}

