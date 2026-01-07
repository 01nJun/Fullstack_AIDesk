package com.desk.dto;

import com.desk.domain.TicketGrade;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TicketPinResponseDTO {
    private Long pino;
    private Long tno;
    private String title;
    private TicketGrade grade;
}