package com.desk.domain;

import jakarta.persistence.*;
import lombok.*;

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

    private String tTitle;
    private String tContent;
    private String tPurpose;
    private String tRequirement;

    @Enumerated(EnumType.STRING)
    private TicketGrade tGrade;

    private LocalDateTime tBirth;
    private LocalDateTime tDeadline;

    private String tWriter; // 추후 member table과 연결

    // 동일 티켓을 여러 사람이 수신 가능(참조...)
    // 각 수신인마다 읽었는지, 진행상태 어떤지가 다르므로 --> TicketPersonal로
    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TicketPersonal> personalList = new ArrayList<>();

    // 생성일 현재로
    @PrePersist
    public void prePersist() {
        if (this.tBirth == null) {
            this.tBirth = LocalDateTime.now();
        }
    }

    // 수정용 메서드
    public void changeTitle(String title) { this.tTitle = title; }
    public void changeContent(String content) { this.tContent = content; }
    public void changePurpose(String purpose) { this.tPurpose = purpose; }
    public void changeRequirement(String requirement) { this.tRequirement = requirement; }
    public void changeGrade(TicketGrade grade) { this.tGrade = grade; }
    public void changeDeadline(LocalDateTime deadline) { this.tDeadline = deadline; }

    // personalList에 person 추가
    // 동시에 setTicket(this)로 연결
    public void addPersonal(TicketPersonal personal) {
        personal.setTicket(this);
        personalList.add(personal);
    }

    // 개별 수신인 제거
    public void removePersonal(TicketPersonal personal) {
        if (personal == null) return;
        boolean removed = this.personalList.remove(personal);
        if (removed) {
            personal.setTicket(null);
        }
    }

    // 수신인 전체 삭제
    public void clearPersonalList() {
        for (TicketPersonal p : personalList) {
            p.setTicket(null);
        }
        personalList.clear();
    }

}
