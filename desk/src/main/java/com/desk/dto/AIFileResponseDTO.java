package com.desk.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * [AI 파일조회 응답 DTO]
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AIFileResponseDTO {
    private String conversationId;
    private String aiMessage;

    @Builder.Default
    private List<AIFileResultDTO> results = new ArrayList<>();
}



