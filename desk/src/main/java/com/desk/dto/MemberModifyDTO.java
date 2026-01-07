package com.desk.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class MemberModifyDTO {
    private String email;
    private String pw;
    private String nickname;
    private String department;
}