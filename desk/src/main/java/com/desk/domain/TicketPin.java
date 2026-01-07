package com.desk.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ticket_pin", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"member_email", "ticket_tno"}) // 중복 찜 방지
})
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = {"member", "ticket"})
public class TicketPin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long pino;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_email", referencedColumnName = "email")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_tno")
    private Ticket ticket;

    @Column(name = "pinned_at")
    private LocalDateTime pinnedAt;

    @PrePersist
    public void prePersist() {
        this.pinnedAt = LocalDateTime.now();
    }
}