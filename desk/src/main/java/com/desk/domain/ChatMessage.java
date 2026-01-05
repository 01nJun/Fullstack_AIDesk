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
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private ChatRoom chatRoom;

    // Redis 등에서 발급
    private Long messageSeq;

    // Member.email
    private String senderId;

    @Enumerated(EnumType.STRING)
    private ChatMessageType messageType;

    @Column(columnDefinition = "TEXT")
    private String content;

    // TICKET_PREVIEW일 때만 사용
    private Long ticketId;

    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.messageType == null) {
            this.messageType = ChatMessageType.TEXT;
        }
    }
}