package com.desk.security.handler;

import com.google.gson.Gson;
import com.desk.dto.MemberDTO;
import com.desk.security.token.RefreshCookieUtil;
import com.desk.security.token.RefreshTokenService;
import com.desk.security.token.LoginLockService;
import com.desk.security.token.TokenType;
import com.desk.util.JWTUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.Map;

@Log4j2
public class APILoginSuccessHandler implements AuthenticationSuccessHandler {

    // application.properties의 refresh.token.storage 설정에 따라 자동으로 Redis 또는 DB가 주입됨
    private final RefreshTokenService refreshTokenService;
    private final LoginLockService loginLockService;

    public APILoginSuccessHandler(RefreshTokenService refreshTokenService, LoginLockService loginLockService) {
        this.refreshTokenService = refreshTokenService;
        this.loginLockService = loginLockService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        MemberDTO memberDTO = (MemberDTO) authentication.getPrincipal();
        Map<String, Object> claims = memberDTO.getClaims();

        // Access Token 생성
        claims.put("tokenType", TokenType.ACCESS.name());
        claims.put("auth_time", Instant.now().getEpochSecond());
        String amr = (String) request.getAttribute("amr");
        claims.put("amr", amr == null ? "pwd" : amr);
        String accessToken = JWTUtil.generateToken(claims, 15);

        // 로그인 성공 시 실패 횟수 초기화
        loginLockService.resetFailureCount(memberDTO.getEmail());

        String refreshToken = refreshTokenService.issueNewSessionRefreshToken(memberDTO.getEmail(), request, amr == null ? "pwd" : amr);
        RefreshCookieUtil.set(request, response, refreshToken, refreshTokenService.refreshCookieMaxAgeSeconds());

        // JSON 응답
        claims.put("accessToken", accessToken);

        Gson gson = new Gson();
        String jsonStr = gson.toJson(claims);

        response.setContentType("application/json; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        PrintWriter printWriter = response.getWriter();
        printWriter.print(jsonStr);
        printWriter.flush();
        printWriter.close();
    }
}
