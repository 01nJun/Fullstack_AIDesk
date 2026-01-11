package com.desk.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 채팅 파일 첨부
 *
 * - 기존 ticket_file과 컬럼 네이밍/구조를 최대한 동일하게 유지
 * - 권한/파일함/AI파일조회 필터를 위해 chat_room_id, message_seq 추가
 */
@Entity
@Table(name = "chat_file")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@ToString(exclude = "chatRoom")
public class ChatFile {

    /**
     * UUID를 PK로 사용 (실제 저장 파일명: uuid.확장자)
     * - CustomFileUtil.saveFile()가 반환하는 savedName을 그대로 저장
     */
    @Id
    private String uuid;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_size")
    private Long fileSize;

    private int ord;

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * 업로더 (Member.email)
     */
    private String writer;

    /**
     * 수신자 표시용(파일함 SENT/RECEIVED 탭과 동일 UX 유지 목적)
     * - DIRECT: 상대 이메일 저장
     * - GROUP: null (수신자는 chat_participant 기반으로 판단)
     */
    private String receiver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    /**
     * 첨부가 연결된 메시지 시퀀스(입장 이후만 노출 필터링 및 메시지-파일 연결 용도)
     */
    @Column(name = "message_seq")
    private Long messageSeq;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}





