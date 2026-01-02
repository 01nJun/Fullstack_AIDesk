package com.desk.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

// RequestDTO 안에 있는 AITicketInfo 재사용
import com.desk.dto.AITicketRequestDTO.AITicketInfo;

/**
 * [AI 티켓 전용 응답 DTO]
 * 
 * [수정 내역]
 * @JsonProperty 어노테이션을 모두 제거했습니다.
 * 이유: 프론트엔드(React)가 자바 표준 변수명(camelCase)인 'aiMessage', 'updatedTicket'을 
 *       기다리고 있기 때문에, 백엔드도 변수명 그대로 JSON을 보내야 합니다.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AITicketResponseDTO {

    // JSON 키: "conversationId" 로 전송됨
    private String conversationId;

    // JSON 키: "aiMessage" 로 전송됨 (프론트엔드와 일치!)
    private String aiMessage;

    // JSON 키: "identifiedTargetDept" 로 전송됨
    private String identifiedTargetDept;

    // JSON 키: "updatedTicket" 로 전송됨 (프론트엔드와 일치!)
    private AITicketInfo updatedTicket;

    // JSON 키: "isCompleted" 로 전송됨 (Lombok이 boolean 필드는 isCompleted로 만듦)
    private boolean isCompleted;

    // JSON 키: "nextAction" 로 전송됨
    private String nextAction;

    // JSON 키: "missingInfoList" 로 전송됨
    @Builder.Default
    private List<String> missingInfoList = new ArrayList<>();
}