package com.desk.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "chat_file",
    indexes = {
        @Index(name = "idx_chat_file_room_id", columnList = "chat_room_id"),
        @Index(name = "idx_chat_file_message_id", columnList = "chat_message_id"),
        @Index(name = "idx_chat_file_status", columnList = "status")
    }
)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@ToString(exclude = "chatMessage")
public class ChatFile {

    @Id
    private String uuid; // UUID를 PK로 사용 (파일 저장명이 됨)

    @Column(name = "chat_room_id", nullable = false)
    private Long chatRoomId; // 채팅방 ID (바인딩 전에도 필요)

    @Column(name = "uploader_id", nullable = false)
    private String uploaderId; // 업로더 ID (Member.email)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_message_id")
    private ChatMessage chatMessage; // nullable (TEMP 상태일 때는 null)

    @Column(name = "file_name", nullable = false)
    private String fileName; // 실제 파일명 (예: 보고서.pdf)

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "ext", length = 10)
    private String ext;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ChatFileStatus status = ChatFileStatus.TEMP;

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt; // 소프트 삭제 시간

    @PrePersist
    void onCreate() {
        if (this.status == null) {
            this.status = ChatFileStatus.TEMP;
        }
        // Auditing이 누락된 환경에서도 createdAt이 null로 남지 않도록 안전장치
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    /**
     * 소프트 삭제 처리
     */
    public void markAsDeleted() {
        this.status = ChatFileStatus.DELETED;
        this.deletedAt = LocalDateTime.now();
    }

    public void setChatMessage(ChatMessage chatMessage) {
        this.chatMessage = chatMessage;
    }

    /**
     * 메시지에 바인딩 (status를 BOUND로 변경)
     */
    public void bindToMessage(ChatMessage chatMessage) {
        this.chatMessage = chatMessage;
        this.status = ChatFileStatus.BOUND;
    }
}

