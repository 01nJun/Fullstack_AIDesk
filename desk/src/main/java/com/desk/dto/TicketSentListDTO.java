package com.desk.dto;

import com.desk.domain.TicketGrade;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketSentListDTO {

    private Long tno;

    private String title;
    private String content;
    private String purpose;
    private String requirement;

    private TicketGrade grade;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime birth;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime deadline;

    private String writer; //email

    @Builder.Default
    private List<TicketStateDTO> personals = new ArrayList<>();

    //첨부 파일 목록
    @Builder.Default
    private List<TicketFileDTO> files = new ArrayList<>();
}