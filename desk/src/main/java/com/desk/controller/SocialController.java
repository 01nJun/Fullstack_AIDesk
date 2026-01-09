package com.desk.controller;

import com.desk.dto.MemberDTO;
import com.desk.dto.MemberModifyDTO;
import com.desk.security.token.RefreshCookieUtil;
import com.desk.security.token.RefreshTokenService;
import com.desk.security.token.TokenType;
import com.desk.service.MemberService;
import com.desk.util.JWTUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.Map;


@RestController // 모든 메서드의 반환값이 JSON 형태로 응답됨
@Log4j2
@RequiredArgsConstructor
public class SocialController {

    private final MemberService memberService;
    
    // application.properties의 refresh.token.storage 설정에 따라 자동으로 Redis 또는 DB가 주입됨
    private final RefreshTokenService refreshTokenService;

    @GetMapping("/api/member/kakao") // 카카오 로그인 API (Authorization: Bearer <kakaoAccessToken>)
    public Map<String, Object> getMemberFromKakao(@RequestHeader("Authorization") String authHeader,
                                                  HttpServletRequest request,
                                                  HttpServletResponse response) {

        if (authHeader == null || !authHeader.startsWith("Bearer ") || authHeader.length() < 8) {
            return Map.of("error", "INVALID_KAKAO_TOKEN");
        }
        String accessToken = authHeader.substring(7);

        // 내부 동작 (Service에서 처리): 카카오 API 호출 → 이메일 조회, DB에 회원이 있으면 그대로 사용, 없으면 소셜 회원 자동 생성, 결과: MemberDTO
        MemberDTO memberDTO = memberService.getKakaoMember(accessToken);

        // 이미 부서 정보까지 입력했는데 승인이 안 난 경우 -> 에러 반환
        if (!memberDTO.isApproved() && memberDTO.getDepartment() != null) {
            return Map.of("error", "PENDING_APPROVAL");
        }

        // 토큰 발급 (처음 가입했거나, 부서 정보 입력하러 갈 때 필요함)
        Map<String, Object> claims = memberDTO.getClaims();

        claims.put("tokenType", TokenType.ACCESS.name());
        claims.put("auth_time", Instant.now().getEpochSecond());
        String jwtAccessToken = JWTUtil.generateToken(claims, 15);

        claims.put("accessToken", jwtAccessToken);

        String refreshToken = refreshTokenService.issueNewSessionRefreshToken(memberDTO.getEmail(), request, "social");
        RefreshCookieUtil.set(request, response, refreshToken, refreshTokenService.refreshCookieMaxAgeSeconds());

        return claims;
    }

    // 회원 정보 수정
    @PutMapping("/api/member/modify")
    @PreAuthorize("isAuthenticated()")
    // API Service에서 처리되는 내용 : 회원 조회, 비밀번호 암호화 후 변경, 닉네임 변경, social = false → 일반 회원 전환
    public Map<String,String> modify(@AuthenticationPrincipal MemberDTO principal,
                                     @RequestBody MemberModifyDTO memberModifyDTO) {

        log.info("member modify: " + memberModifyDTO);

        if (principal == null || principal.getEmail() == null) {
            return Map.of("error", "UNAUTHORIZED");
        }
        if (memberModifyDTO.getEmail() == null || !principal.getEmail().equals(memberModifyDTO.getEmail())) {
            return Map.of("error", "SUBJECT_MISMATCH");
        }

        memberService.modifyMember(memberModifyDTO);

        return Map.of("result","modified");

    }

}