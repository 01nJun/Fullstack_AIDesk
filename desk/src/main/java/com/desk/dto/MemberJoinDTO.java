package com.desk.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class MemberJoinDTO {
    private String email;
    private String pw;
    private String nickname;
    private String department; // 회원가입 시 선택한 부서
}