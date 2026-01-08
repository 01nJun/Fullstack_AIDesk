package com.desk.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chat_message")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString(exclude = {"chatRoom", "chatFiles"})
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
    @Column(name = "message_type", length = 20)
    private ChatMessageType messageType;

    @Column(columnDefinition = "TEXT")
    private String content;

    // TICKET_PREVIEW일 때만 사용
    @Column(name = "ticket_id")
    private Long ticketId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "chatMessage", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)  // N+1 방지
    @Builder.Default
    private List<ChatFile> chatFiles = new ArrayList<>();

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.messageType == null) {
            this.messageType = ChatMessageType.TEXT;
        }
    }

    /**
     * 파일 추가 (양방향 관계 동기화)
     */
    public void addFile(ChatFile file) {
        file.bindToMessage(this);
        this.chatFiles.add(file);
    }
}