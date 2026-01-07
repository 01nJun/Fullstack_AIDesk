package com.desk.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TicketFileDTO {
    private String uuid;
    private String fileName;
    private Long fileSize;
    private int ord;
    private LocalDateTime createdAt;
    private String writer;
    private String receiver;
}