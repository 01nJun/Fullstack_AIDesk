package com.desk.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * [AI 파일조회 결과 1건]
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AIFileResultDTO {
    private String uuid;          // 저장 파일명(다운로드 key)
    private String fileName;      // 원본 파일명
    private Long fileSize;
    private LocalDateTime createdAt;

    private Long tno;
    private String ticketTitle;

    private String writerEmail;
    private String receiverEmail;
}



