package com.desk.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tbl_reply", indexes = {
        @Index(name = "idx_reply_board_bno", columnList = "board_bno")
})
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
// [주의] 부모/자식 관계가 추가되면 ToString에서 제외해야 무한 루프(StackOverflow)를 방지합니다.
@ToString(exclude = {"board", "parent", "children"})
public class Reply extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long rno;

    @ManyToOne(fetch = FetchType.LAZY)
    private Board board;

    @Column(name = "reply_text")
    private String replyText;
    private String replyer;

    // --- [대댓글을 위한 핵심 추가 필드] ---

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_rno") // 부모 댓글 번호를 저장할 컬럼
    private Reply parent; // 어떤 댓글의 답글인지 (null이면 최상위 댓글)

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default // 빌더 사용 시 기본값 유지
    private List<Reply> children = new ArrayList<>(); // 이 댓글에 달린 답글들

    // --- [도메인 로직] ---

    public void changeText(String replyText) {
        this.replyText = replyText;
    }

    public void changeReplyer(String replyer) {
        this.replyer = replyer;
    }

    /**
     * 부모 댓글을 설정하는 메서드 (대댓글 등록 시 사용)
     */
    public void setParentReply(Reply parent) {
        this.parent = parent;
    }
}