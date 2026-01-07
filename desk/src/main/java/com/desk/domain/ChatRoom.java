package com.desk.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chat_room")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString(exclude = "participants")
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "room_type")
    private ChatRoomType roomType;

    // DIRECT 방 중복 방지용
    @Column(name = "pair_key")
    private String pairKey;

    // GROUP 방 이름
    private String name;

    // 채팅 목록용 캐시
    @Column(columnDefinition = "TEXT", name = "last_msg_content")
    private String lastMsgContent;

    @Column(name = "last_msg_at")
    private LocalDateTime lastMsgAt;

    @Column(name = "last_msg_seq")
    private Long lastMsgSeq;

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChatParticipant> participants = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.roomType == null) {
            this.roomType = ChatRoomType.DIRECT;
        }
    }

    public void updateLastMessage(Long seq, String content) {
        this.lastMsgSeq = seq;
        this.lastMsgContent = content;
        this.lastMsgAt = LocalDateTime.now();
    }
}
