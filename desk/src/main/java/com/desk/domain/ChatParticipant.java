package com.desk.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString(exclude = "chatRoom")
public class ChatParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private ChatRoom chatRoom;

    // Member.email
    private String userId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ChatStatus status = ChatStatus.ACTIVE;

    // 안 읽음 / 1 표시 핵심
    @Builder.Default
    private Long lastReadSeq = 0L;

    private LocalDateTime joinedAt;

    private LocalDateTime leftAt;

    @PrePersist
    void onCreate() {
        this.joinedAt = LocalDateTime.now();
    }

    public void leave() {
        this.status = ChatStatus.LEFT;
        this.leftAt = LocalDateTime.now();
    }

    public void markRead(Long seq) {
        if (seq != null && seq > this.lastReadSeq) {
            this.lastReadSeq = seq;
        }
    }
}
