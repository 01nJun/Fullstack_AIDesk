package com.desk.dto;

import com.desk.domain.TicketGrade;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TicketCreateDTO {

    private String title;
    private String content;
    private String purpose;
    private String requirement;

    private TicketGrade grade;

    // 날짜 포맷
    // 나중에 포매터로 뺄 수 있지 않을까...
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime deadline;

    // 수신인들
    @Builder.Default
    private List<String> receivers = new ArrayList<>();
}
