package com.desk.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MeetingMinutesDTO {

    private String title;       // 제목 (Ticket Title)
    private List<String> attendees;   // 참석자 -> 담당자 (Receivers)
    private String overview;    // 개요 -> 목적 (Purpose)
    private String details;     // 상세 -> 상세 (Requirement)
    private String conclusion;  // 결론 (PDF용)

    private String shortSummary; // 요약 (Ticket Content)

    // [추가] 마감일 (YYYY-MM-DD 형태 추출 시도)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate deadline;

    // (getAttendeesString 메서드는 기존 그대로 유지)
    @JsonIgnore
    public String getAttendeesString() {
        // ... (기존 코드 유지) ...
        return attendees != null ? attendees.toString() : "";
    }
}