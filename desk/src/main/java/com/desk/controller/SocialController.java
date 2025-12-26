package com.desk.controller;

import com.desk.dto.MemberDTO;
import com.desk.dto.MemberModifyDTO;
import com.desk.service.MemberService;
import com.desk.util.JWTUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;


@RestController // 모든 메서드의 반환값이 JSON 형태로 응답됨
@Log4j2
@RequiredArgsConstructor
public class SocialController {

    private final MemberService memberService;

    @GetMapping("/api/member/kakao") // 카카오 로그인 API (/api/member/kakao?accessToken=xxxx)
    public Map<String, Object> getMemberFromKakao(String accessToken) {

        // 내부 동작 (Service에서 처리): 카카오 API 호출 → 이메일 조회, DB에 회원이 있으면 그대로 사용, 없으면 소셜 회원 자동 생성, 결과: MemberDTO
        MemberDTO memberDTO = memberService.getKakaoMember(accessToken);

        // 이미 부서 정보까지 입력했는데 승인이 안 난 경우 -> 에러 반환
        if (!memberDTO.isApproved() && memberDTO.getDepartment() != null) {
            return Map.of("error", "PENDING_APPROVAL");
        }

        // 토큰 발급 (처음 가입했거나, 부서 정보 입력하러 갈 때 필요함)
        Map<String, Object> claims = memberDTO.getClaims();

        String jwtAccessToken = JWTUtil.generateToken(claims, 60);
        String jwtRefreshToken = JWTUtil.generateToken(claims, 60*24);

        claims.put("accessToken", jwtAccessToken);
        claims.put("refreshToken", jwtRefreshToken);

        return claims;
    }

    // 회원 정보 수정
    @PutMapping("/api/member/modify")
    // API Service에서 처리되는 내용 : 회원 조회, 비밀번호 암호화 후 변경, 닉네임 변경, social = false → 일반 회원 전환
    public Map<String,String> modify(@RequestBody MemberModifyDTO memberModifyDTO) {

        log.info("member modify: " + memberModifyDTO);

        memberService.modifyMember(memberModifyDTO);

        return Map.of("result","modified");

    }

}