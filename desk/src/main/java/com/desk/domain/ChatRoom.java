package com.desk.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
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
    private ChatRoomType roomType;

    // DIRECT 방 중복 방지용
    private String pairKey;

    // GROUP 방 이름
    private String name;

    // 채팅 목록용 캐시
    @Column(columnDefinition = "TEXT")
    private String lastMsgContent;

    private LocalDateTime lastMsgAt;

    private Long lastMsgSeq;

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChatParticipant> participants = new ArrayList<>();

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
