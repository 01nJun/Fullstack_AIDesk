package com.desk.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ticket_personal")
@Getter
@ToString(exclude = "ticket")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TicketPersonal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long pno;

    @ManyToOne(fetch = FetchType.LAZY) // 필요할 때만 내용 조회
    @JoinColumn(name = "tp_tno") // 외래키 매핑
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_email", referencedColumnName = "email")
    private Member receiver; // Member의 email을 외래키로 사용

    @Builder.Default
    private boolean isread = false;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TicketState state = TicketState.NEW;

    // 읽음확인
    public void changeRead(boolean read) {
        this.isread = read;
    }

    // 상태 변경
    public void changeState(TicketState state) {
        this.state = state;
    }

    public void setTicket(Ticket ticket) {
        this.ticket = ticket;
    }
}
