package com.desk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * [AI 공통 DTO]
 * Ollama API 통신을 위한 표준 Request/Response 규격입니다.
 * 티켓뿐만 아니라 다른 AI 기능에서도 공통으로 사용됩니다.
 */
public class OllamaDTO {

    // [요청] 우리가 Ollama에게 보낼 때
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Request {
        private String model;           // 사용할 모델명 (예: qwen3:8b)
        private List<Message> messages; // 대화 내역
        private boolean stream;         // 스트리밍 여부 (false 권장)
        
        // JSON 모드 사용 시 "json" 입력 (구조화된 출력 필요할 때)
        // 일반 대화 시에는 null
        private String format; 
        
        private Options options;        // 온도(temperature) 등 상세 설정

        @Data
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        public static class Options {
            private double temperature; // 0.0 ~ 1.0 (창의성 조절)
        }
    }

    // [응답] Ollama가 우리에게 줄 때
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Response {
        private String model;
        
        @JsonProperty("created_at")
        private String createdAt;
        
        private Message message; // AI의 답변 내용이 여기 들어있음
        
        private boolean done;
    }

    // [공통] 대화 메시지 객체 (User, Assistant, System)
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Message {
        private String role;    // "system", "user", "assistant"
        private String content; // 대화 내용
    }
}