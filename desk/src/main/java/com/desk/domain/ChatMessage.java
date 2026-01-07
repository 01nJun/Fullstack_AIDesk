package com.desk.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_message")
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
    @JoinColumn(name = "chat_room_id")
    private ChatRoom chatRoom;

    // Redis 등에서 발급
    @Column(name = "message_seq")
    private Long messageSeq;

    // Member.email
    @Column(name = "sender_id")
    private String senderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type")
    private ChatMessageType messageType;

    @Column(columnDefinition = "TEXT")
    private String content;

    // TICKET_PREVIEW일 때만 사용
    @Column(name = "ticket_id")
    private Long ticketId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.messageType == null) {
            this.messageType = ChatMessageType.TEXT;
        }
    }
}