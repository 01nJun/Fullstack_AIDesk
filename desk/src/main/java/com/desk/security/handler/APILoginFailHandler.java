package com.desk.security.handler;

import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

@Log4j2 // AuthenticationFailureHandler 구현 → Spring Security에서 로그인 실패 시 호출
public class APILoginFailHandler implements AuthenticationFailureHandler{

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException, ServletException{
        log.info("Login fail...."+ exception);

        String errorMessage = "ERROR_LOGIN";

        // 예외 메시지에 따라 구분
        if (exception.getMessage().equals("PENDING_APPROVAL")) {
            errorMessage = "PENDING_APPROVAL";
        } else if (exception.getMessage().equals("DELETED_ACCOUNT")) {
            errorMessage = "DELETED_ACCOUNT";
        } else if (exception instanceof BadCredentialsException) {
            errorMessage = "BAD_CREDENTIALS"; // 비번 틀림
        }

        Gson gson = new Gson();
        // Map → JSON 문자열 변환, 클라이언트는 로그인 실패 이유를 JSON으로 받음
        String jsonStr = gson.toJson(Map.of("error", errorMessage));

        response.setContentType("application/json");
        // Content-Type 설정 → JSON, JSON 문자열 출력 후 스트림 종료
        PrintWriter printWriter = response.getWriter();
        printWriter.println(jsonStr);
        printWriter.close();
    }
}