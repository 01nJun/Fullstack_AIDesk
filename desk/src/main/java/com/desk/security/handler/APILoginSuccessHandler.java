package com.desk.security.handler;

import com.google.gson.Gson;
import com.desk.dto.MemberDTO;
import com.desk.util.JWTUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

@Log4j2
public class APILoginSuccessHandler implements AuthenticationSuccessHandler{

    @Override // authentication → 로그인 성공 후 Spring Security 인증 객체
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException{
        log.info("-------------------------------");
        log.info(authentication);
        log.info("-------------------------------");

        // authentication.getPrincipal()로 가져오는 객체는 사용자 정보 + 로그인 인증 상태 + 권한 정보를 모두 담고 있는 Security 전용 객체
        MemberDTO memberDTO = (MemberDTO)authentication.getPrincipal();

        // JWT payload로 담을 정보, memberDTO에 정의된 함수
        Map<String, Object> claims  = memberDTO.getClaims();

        String accessToken = JWTUtil.generateToken(claims, 60);
        String refreshToken = JWTUtil.generateToken(claims,60*24);

        // JSON 응답에 accessToken + refreshToken 포함
        claims.put("accessToken", accessToken);
        claims.put("refreshToken", refreshToken);

        Gson gson = new Gson();
        // claims - Map → JSON 문자열로 변환
        String jsonStr = gson.toJson(claims);

        // 브라우저/클라이언트에 JSON 형식으로 반환
        response.setContentType("application/json; charset=UTF-8");
        PrintWriter printWriter = response.getWriter();
        printWriter.println(jsonStr);
        printWriter.close();
    }
}