package com.desk.domain;

import java.time.LocalDateTime;
import java.util.ArrayList; // 추가
import java.util.List;      // 추가
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tbl_board")
@Getter
@ToString(exclude = "replyList") // 중요: 순환 참조 방지를 위해 replyList는 제외
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Board {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bno;

    private String title;
    private String content;
    private String writer;
    private String category;

    // --- 댓글 연관관계 추가 시작 ---
    // mappedBy = "board"는 Reply 엔티티 안에 있는 Board 필드 변수명과 일치해야 합니다.
    @OneToMany(mappedBy = "board",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    @Builder.Default // Builder 사용 시 초기화 보장
    private List<Reply> replyList = new ArrayList<>();
    // --- 댓글 연관관계 추가 끝 ---

    @Column(name = "reg_date", updatable = false)
    private LocalDateTime regDate;

    @Column(name = "mod_date")
    private LocalDateTime modDate;

    @PrePersist
    public void prePersist() {
        this.regDate = LocalDateTime.now();
        this.modDate = LocalDateTime.now();
    }

    public void changeTitle(String title){
        this.title = title;
    }

    public void changeCategory(String category){
        this.category = category;
    }

    public void changeContent(String content){
        this.content = content;
    }

    public void changeModDate(LocalDateTime modDate){
        this.modDate = modDate;
    }
}
