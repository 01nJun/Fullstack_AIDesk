package com.desk.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@Setter
@ToString
// User → Spring Security에서 제공하는 기본 인증 객체 (UserDetails 구현)
// MemberDTO → DB 회원 정보 + Security 인증 정보 + JWT claims를 한 번에 담는 DTO
public class MemberDTO extends User {

    private String email;
    private String pw;
    private String nickname;
    private boolean social;
    private String department; // 부서 정보 (String으로 처리)
    private boolean approved;
    private List<String> roleNames = new ArrayList<>();
    private boolean faceEnabled;

    public MemberDTO(String email, String pw, String nickname, boolean social, String department, boolean approved, List<String> roleNames, boolean faceEnabled) {
        super(
                email,
                pw, // 권한을 SimpleGrantedAuthority로 변환 (ROLE_ 접두사 붙임)
                roleNames.stream().map(str -> new SimpleGrantedAuthority("ROLE_"+str)).collect(Collectors.toList()));

        this.email = email;
        this.pw = pw;
        this.nickname = nickname;
        this.social = social;
        this.department = department;
        this.approved = approved;
        this.roleNames = roleNames;
        this.faceEnabled = faceEnabled;
    }

    // JWT에 들어갈 정보 (Claims)
    public Map<String, Object> getClaims() {
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("email", email);
//    dataMap.put("pw", pw);
        dataMap.put("nickname", nickname);
        dataMap.put("social", social);
        dataMap.put("department", department); // JWT에 포함
        dataMap.put("approved", approved);
        dataMap.put("roleNames", roleNames);
        dataMap.put("faceEnabled", faceEnabled);
        return dataMap;
    }
}