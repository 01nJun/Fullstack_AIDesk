package com.desk.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "ticket_file")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@ToString(exclude = "ticket")
public class TicketFile {

    @Id
    private String uuid; // UUID를 PK로 사용 (파일 저장명이 됨)

    @Column(name = "file_name")
    private String fileName; // 실제 파일명 (예: 보고서.pdf)
    @Column(name = "file_size")
    private Long fileSize;
    private int ord; // 저장 순서

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    private String writer; // 업로더
    private String receiver; // 수신자 (티켓의 수신자 정보 복사)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tno")
    private Ticket ticket;

    public void setTicket(Ticket ticket) {
        this.ticket = ticket;
    }
}