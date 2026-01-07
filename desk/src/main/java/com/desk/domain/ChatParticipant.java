package com.desk.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_participant")
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
    @JoinColumn(name = "chat_room_id")
    private ChatRoom chatRoom;

    // Member.email
    @Column(name = "user_id")
    private String userId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ChatStatus status = ChatStatus.ACTIVE;

    // 안 읽음 / 1 표시 핵심
    @Builder.Default
    @Column(name = "last_read_seq")
    private Long lastReadSeq = 0L;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    @PrePersist
    @Column(name = "on_create")
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
