package com.desk.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_ticket")
@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tno;

    @Column(name = "t_title", nullable = false)
    private String tTitle;

    @Column(name = "t_content", nullable = false)
    private String tContent;

    @Column(name = "t_purpose", nullable = false)
    private String tPurpose;

    @Column(name = "t_requirement", nullable = false)
    private String tRequirement;

    @Column(name = "t_grade")
    private String tGrade;

    @Column(name = "t_birth", nullable = false)
    private LocalDateTime tBirth;

    @Column(name = "t_deadline")
    private LocalDateTime tDeadline;

    @Column(name = "t_writer", nullable = false)
    private String tWriter;

    @PrePersist
    public void prePersist() {
        if (this.tBirth == null) {
            this.tBirth = LocalDateTime.now();
        }
    }

    // PUT 업데이트용(Setter 대신)
    public void updateFromDto(String tTitle, String tContent, String tPurpose,
                              String tRequirement, String tGrade, LocalDateTime tDeadline) {
        this.tTitle = tTitle;
        this.tContent = tContent;
        this.tPurpose = tPurpose;
        this.tRequirement = tRequirement;
        this.tGrade = tGrade;
        this.tDeadline = tDeadline;
    }

    public void changeTitle(String title) { this.tTitle = title; }
    public void changeContent(String content) { this.tContent = content; }
    public void changePurpose(String purpose) { this.tPurpose = purpose; }
    public void changeRequirement(String requirement) { this.tRequirement = requirement; }
    public void changeGrade(String grade) { this.tGrade = grade; }
    public void changeDeadline(LocalDateTime deadline) { this.tDeadline = deadline; }

}
