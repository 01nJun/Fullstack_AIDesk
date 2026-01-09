package com.desk.security.filter;

import com.google.gson.Gson;
import com.desk.dto.MemberDTO;
import com.desk.util.CustomJWTException;
import com.desk.util.JWTUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

@Log4j2 // OncePerRequestFilter 상속 → 모든 HTTP 요청마다 한 번 실행되는 필터
public class JWTCheckFilter extends OncePerRequestFilter{
    // shouldNotFilter()가 false면 필터 로직(doFilterInternal)이 실행되어 JWT 검증 등 인증 처리가 수행되고, 
    // true면 필터를 건너뛰고 다음 필터로 넘어간다.
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException{

        // Preflight(안전하지 않은)요청은 체크하지 않음
        if(request.getMethod().equals("OPTIONS")){
            return true;
        }

        String path = request.getRequestURI();

        log.info("check uri......................."+path);

        // “로그인 안 한 사용자도 접근 가능한 API”는 JWT 체크 안 함
        // api/member/ 경로의 호출은 체크하지 않음
        if(path.startsWith("/api/member/")) {
            return true;
        }

        // 이미지 조회 경로는 체크하지 않음
        if (path.startsWith("/api/files/view/")) {
            return true;
        }

        return false;
    }
    @Override // 실제 JWT 검증 처리를 수행. 성공 → SecurityContext에 인증 정보 설정, 실패 → JSON 에러 응답
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException{

        log.info("------------------------JWTCheckFilter------------------");

        // WebSocket 핸드셰이크 요청인지 확인
        String upgradeHeader = request.getHeader("Upgrade");
        boolean isWebSocket = "websocket".equalsIgnoreCase(upgradeHeader) || request.getRequestURI().startsWith("/ws");

        // 클라이언트에서 Authorization: Bearer <JWT>로 전달
        String authHeaderStr = request.getHeader("Authorization");

        if (authHeaderStr == null || !authHeaderStr.startsWith("Bearer ")) {
            log.error("JWT Check Error: Authorization header is missing or invalid");

            // WebSocket 핸드셰이크 실패 시 403 Forbidden 반환
            if (isWebSocket) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().close();
                return;
            }

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            PrintWriter printWriter = response.getWriter();
            printWriter.println(new Gson().toJson(Map.of("error", "UNAUTHORIZED")));
            printWriter.close();
            return;
        }

        try {
            //Bearer accestoken... "Bearer " 접두사 제거
            String accessToken = authHeaderStr.substring(7);
            // JWT 서명 확인 + payload(claims) 반환, 실패 시 예외 → catch 블록으로 이동
            Map<String, Object> claims = JWTUtil.validateToken(accessToken);

            log.info("JWT claims: " + claims);

            String email = (String) claims.get("email");
//      String pw = (String) claims.get("pw");
            String pw = "PROTECTED"; // 비밀번호 대신 사용할 임의의 문자열 (보안상 실제 비번 노출 안 함)

            String nickname = (String) claims.get("nickname");
            Boolean social = (Boolean) claims.get("social");
            String department = (String) claims.get("department");
            Boolean approved = (Boolean) claims.get("approved");
            List<String> roleNames = (List<String>) claims.get("roleNames");
            Boolean faceEnabled = (Boolean) claims.get("faceEnabled");

            // JWT에서 추출한 정보로 인증 객체(MemberDTO) 생성, UserDetails 역할
            MemberDTO memberDTO = new MemberDTO(email, pw, nickname, social.booleanValue(), department, approved.booleanValue(), roleNames, faceEnabled);

            log.info("-----------------------------------");
            log.info(memberDTO);
            log.info(memberDTO.getAuthorities());

            UsernamePasswordAuthenticationToken authenticationToken
                    = new UsernamePasswordAuthenticationToken(memberDTO, pw, memberDTO.getAuthorities());

            // 지금 로그인한 사용자의 인증 정보(사용자 정보, 비밀번호, 권한 등)를 SecurityContext에 저장
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);

        }catch(CustomJWTException e){ // 예외 처리 (JWT 검증 실패만 처리)
            log.error("JWT Check Error..............");
            log.error(e.getMessage());

            // WebSocket 핸드셰이크 실패 시
            if (isWebSocket) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().close();
                return;
            }

            Gson gson = new Gson();
            String msg = gson.toJson(Map.of("error", "ERROR_ACCESS_TOKEN"));

            response.setContentType("application/json");
            PrintWriter printWriter = response.getWriter();
            printWriter.println(msg);
            printWriter.close();
            return; // JWT 검증 실패 시 여기서 종료

        }catch(Exception e){ // JWT 검증과 무관한 예외는 그대로 전파
            // JWT 검증과 무관한 예외는 그대로 전파 (ServletException, IOException 등)
            throw e;
        }

        // JWT 검증 성공 시 필터 체인 계속 진행
        filterChain.doFilter(request, response);
    }

}