package com.desk.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * [AI 티켓 전용 요청 DTO]
 * 프론트엔드(React)에서 전송하는 데이터 구조와 1:1 매핑됩니다.
 * 기존 Python Backend의 MediationRequest 모델을 대체합니다.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AITicketRequestDTO {

    @JsonProperty("conversation_id")
    private String conversationId;

    @JsonProperty("sender_dept")
    private String senderDept;

    @JsonProperty("target_dept")
    private String targetDept;

    @JsonProperty("user_input")
    private String userInput;

    @JsonProperty("chat_history")
    @Builder.Default
    private List<AITicketMessage> chatHistory = new ArrayList<>();

    @JsonProperty("current_ticket")
    private AITicketInfo currentTicket;

    // --------------------------------------------------------
    // 내부 클래스 1: 대화 메시지 구조
    // --------------------------------------------------------
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AITicketMessage {
        private String role;    // "user", "assistant", "system"
        private String content; // 메시지 본문
        
        // 값이 없으면 JSON 전송 시 제외
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String timestamp;
    }

    // --------------------------------------------------------
    // 내부 클래스 2: 티켓 데이터 구조
    // --------------------------------------------------------
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AITicketInfo {
        
        @Builder.Default
        private String title = "";

        @Builder.Default
        private String content = "";

        @Builder.Default
        private String purpose = "";

        @Builder.Default
        private String requirement = "";

        @Builder.Default
        private String deadline = "";

        @Builder.Default
        private String grade = "MIDDLE"; // 기본값 MIDDLE 유지

        @JsonProperty("receivers")
        @Builder.Default
        private List<String> receivers = new ArrayList<>();

        @JsonProperty("completion_rate")
        @Builder.Default
        private int completionRate = 0;
    }
}