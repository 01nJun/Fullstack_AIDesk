package com.desk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * [AI 파일조회 전용 요청 DTO]
 * 프론트에서 conversation_id / user_input 형태로 전송
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AIFileRequestDTO {

    @JsonProperty("conversation_id")
    private String conversationId;

    @JsonProperty("user_input")
    private String userInput;
}



