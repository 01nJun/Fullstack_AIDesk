package com.desk.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString(exclude = "memberRoleList")
public class Member {

    @Id
    private String email;

    private String pw;
    private String nickname;
    private boolean social;

    // 추가된 필드들
    @Enumerated(EnumType.STRING)
    private Department department; // 부서

    @Builder.Default
    private boolean isDeleted = false; // 삭제 여부

    @Builder.Default
    private boolean isApproved = false; // 승인 여부 (소셜 로그인이 아니면 기본 false)

    @ElementCollection(fetch = FetchType.LAZY)
    @Builder.Default
    private List<MemberRole> roleList = new ArrayList<>();

    public void addRole(MemberRole memberRole){
        roleList.add(memberRole);
    }

    public void changePw(String pw) { this.pw = pw; }
    public void changeNickname(String nickname) { this.nickname = nickname; }
    public void changeSocial(boolean social) { this.social = social; }
    public void changeDepartment(Department department) { this.department = (department != null) ? department : Department.DEVELOPMENT; }


    // 관리자 기능용 메서드
    public void changeDeleted(boolean isDeleted) { this.isDeleted = isDeleted; }
    public void changeApproved(boolean isApproved) { this.isApproved = isApproved; }
}