package com.desk.dto;

import com.desk.domain.TicketGrade;
import com.desk.domain.TicketState;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonalTicketDTO {

    // TicketPersonal
    private Long tpno;
    private String tpReceiver;
    private boolean tpRead;
    private TicketState tpState;

    // Ticket
    private Long tno;
    private String tTitle;
    private String tContent;
    private String tPurpose;
    private String tRequirement;
    private TicketGrade tGrade;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime tBirth;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime tDeadline;

    private String tWriter;
}
