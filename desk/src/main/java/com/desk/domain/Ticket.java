package com.desk.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ticket")
@Getter
@ToString(exclude = "personalList")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tno;

    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    @Column(columnDefinition = "TEXT")
    private String purpose;
    
    @Column(columnDefinition = "TEXT")
    private String requirement;

    @Enumerated(EnumType.STRING)
    private TicketGrade grade;

    private LocalDateTime birth;
    private LocalDateTime deadline;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "writer_email", referencedColumnName = "email")
    private Member writer; // Member의 email을 외래키로 사용

    // 동일 티켓을 여러 사람이 수신 가능(참조...)
    // 각 수신인마다 읽었는지, 진행상태 어떤지가 다르므로 --> TicketPersonal로
    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 100) // N+1 방지 위한 batchSize
    @Builder.Default
    private List<TicketPersonal> personalList = new ArrayList<>();

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 100)
    @Builder.Default
    private List<TicketFile> fileList = new ArrayList<>();

    // Ticket ↔ TicketFile 양방향 연관관계를 동기화하여 FK와 컬렉션을 동시에 맞춘다
    // DB를 위한 FK, JPA 동작을 위한 객체 관계, 둘은 별개라서 둘 다 맞춰야 한다.
    public void addFile(TicketFile file) {
        file.setTicket(this);
        this.fileList.add(file);
    }

    // 생성일 현재로
    @PrePersist
    public void prePersist() {
        if (this.birth == null) {
            this.birth = LocalDateTime.now();
        }
    }

    // 수정용 메서드
    public void changeTitle(String title) { this.title = title; }
    public void changeContent(String content) { this.content = content; }
    public void changePurpose(String purpose) { this.purpose = purpose; }
    public void changeRequirement(String requirement) { this.requirement = requirement; }
    public void changeGrade(TicketGrade grade) { this.grade = grade; }
    public void changeDeadline(LocalDateTime deadline) { this.deadline = deadline; }

    // personalList에 person 추가
    // 동시에 setTicket(this)로 연결
    public void addPersonal(TicketPersonal personal) {
        personal.setTicket(this);
        personalList.add(personal);
    }

    // 개별 수신인 제거 (이거 쓰일까요..?)
    public void removePersonal(TicketPersonal personal) {
        if (personal == null) return;
        boolean removed = this.personalList.remove(personal);
        if (removed) {
            personal.setTicket(null);
        }
    }

    // 수신인 전체 삭제 (이것도 쓰일지는 모르겠지만 일단...)
    public void clearPersonalList() {
        for (TicketPersonal p : personalList) {
            p.setTicket(null);
        }
        personalList.clear();
    }

}
