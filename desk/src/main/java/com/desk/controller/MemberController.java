package com.desk.controller;

import com.desk.dto.MemberDTO;
import com.desk.dto.MemberJoinDTO;
import com.desk.service.FaceService;
import com.desk.dto.PageRequestDTO;
import com.desk.dto.PageResponseDTO;
import com.desk.service.MemberService;
import com.desk.security.token.RefreshCookieUtil;
import com.desk.security.token.RefreshTokenService;
import com.desk.util.JWTUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.desk.domain.Member;
import com.desk.repository.MemberRepository;

@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/api/member")
public class MemberController {

    private final MemberService memberService;
    private final FaceService faceService;
    
    // application.properties의 refresh.token.storage 설정에 따라 자동으로 Redis 또는 DB가 주입됨
    private final RefreshTokenService refreshTokenService;
    private final MemberRepository memberRepository;

    @PostMapping("/join")
    public Map<String, String> join(@RequestBody MemberJoinDTO memberJoinDTO) {
        log.info("member join: " + memberJoinDTO);

        memberService.join(memberJoinDTO);

        return Map.of("result", "success");
    }

    @PostMapping("/face-register")
    @PreAuthorize("isAuthenticated()")
    public Map<String, String> faceRegister(@AuthenticationPrincipal MemberDTO principal,
                                            @RequestParam String email,
                                            @RequestParam MultipartFile faceFile) throws IOException {
        if (principal == null || principal.getEmail() == null) return Map.of("error", "UNAUTHORIZED");
        if (email == null || !principal.getEmail().equals(email)) return Map.of("error", "SUBJECT_MISMATCH");
        faceService.registerFace(principal.getEmail(), faceFile);
        return Map.of("result", "success");
    }

    @PutMapping("/update-face-status")
    @PreAuthorize("isAuthenticated()")
    public Map<String, String> updateFaceStatus(@AuthenticationPrincipal MemberDTO principal,
                                                @RequestBody Map<String, Object> params) {
        if (principal == null || principal.getEmail() == null) return Map.of("error", "UNAUTHORIZED");
        Object email = params.get("email");
        if (email != null && !principal.getEmail().equals(email.toString())) return Map.of("error", "SUBJECT_MISMATCH");
        faceService.updateFaceStatus(principal.getEmail(), (Boolean) params.get("status"));
        return Map.of("result", "success");
    }

    // 일반 사용자용 멤버 검색 API (승인된 멤버만 검색)
    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public PageResponseDTO<MemberDTO> searchMembers(
            PageRequestDTO pageRequestDTO,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "department", required = false) String department) {
        log.info("member search - keyword: {}, department: {}", keyword, department);
        return memberService.searchActiveMembers(pageRequestDTO, keyword, department);
    }

    // 로그아웃 API - Refresh Token 삭제
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public Map<String, String> logout(HttpServletRequest request, HttpServletResponse response) {
        log.info("Logout request received");
        String refreshToken = RefreshCookieUtil.get(request);
        if (refreshToken != null && !refreshToken.isBlank()) {
            try {
                // Refresh Token에서 jti와 familyId 추출
                Map<String, Object> claims = JWTUtil.validateToken(refreshToken);
                String jti = (String) claims.get("jti");
                String familyId = (String) claims.get("familyId");
                
                if (jti != null && familyId != null) {
                    refreshTokenService.revokeFamily(familyId);
                    log.info("Revoked refresh token family: {}", familyId);
                }
            } catch (Exception e) {
                log.warn("Failed to revoke refresh token during logout: {}", e.getMessage());
                // Refresh Token 파싱 실패해도 쿠키는 삭제해야 함
            }
        }
        
        // Refresh Token 쿠키 삭제 (HttpOnly 쿠키는 서버에서만 삭제 가능)
        RefreshCookieUtil.clear(request, response);
        log.info("Refresh token cookie cleared");
        
        response.setContentType("application/json; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        
        // 응답 전송 전에 쿠키 헤더가 적용되도록 flush
        try {
            response.flushBuffer();
        } catch (IOException e) {
            log.warn("Failed to flush response buffer: {}", e.getMessage());
        }
        
        return Map.of("result", "success");
    }

    // 담당자 정보 조회 API (email로 부서, 닉네임 조회)
    @GetMapping("/info/{email}")
    public ResponseEntity<Map<String, String>> getMemberInfo(@PathVariable("email") String email) {
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        log.info("담당자 정보 조회 요청: {}", email);
        Optional<Member> member = memberRepository.findById(email);
        if (member.isPresent()) {
            Member m = member.get();
            Map<String, String> info = new HashMap<>();
            info.put("email", m.getEmail());
            info.put("nickname", m.getNickname() != null ? m.getNickname() : "");
            info.put("department", m.getDepartment() != null ? m.getDepartment().name() : "");
            return ResponseEntity.ok(info);
        }
        return ResponseEntity.notFound().build();
    }
}