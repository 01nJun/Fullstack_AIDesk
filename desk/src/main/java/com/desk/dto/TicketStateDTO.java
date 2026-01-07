package com.desk.dto;

import com.desk.domain.TicketState;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Setter
public class TicketStateDTO {

    private Long pno;
    private String receiver;
    private boolean isread;
    private TicketState state;
}
