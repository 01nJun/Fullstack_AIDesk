package com.desk.dto;

import java.time.LocalDateTime;

public record TicketDTO(
        Long tno,
        String tTitle,
        String tContent,
        String tPurpose,
        String tRequirement,
        String tGrade,
        LocalDateTime tBirth,
        LocalDateTime tDeadline,
        String tWriter
) {}
