package com.desk.controller;

import com.desk.domain.Member;
import com.desk.dto.MemberDTO;
import com.desk.repository.MemberRepository;
import com.desk.util.CustomJWTException;
import com.desk.util.JWTUtil;
import com.desk.security.token.RefreshCookieUtil;
import com.desk.security.token.RefreshTokenService;
import com.desk.security.token.TokenType;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Log4j2
public class APIRefreshController {

  // application.properties의 refresh.token.storage 설정에 따라 자동으로 Redis 또는 DB가 주입됨
  // redis: Redis 사용 (기본값)
  // db: DB 사용
  private final RefreshTokenService refreshTokenService;
  private final MemberRepository memberRepository;
  
  @Value("${refresh.token.storage:redis}")
  private String storageType;

  @RequestMapping("/api/member/refresh") 
  // HTTP 요청 헤더 중 Authorization 값을 가져와서 authHeader 변수에 담음 → 일반적으로 JWT를 Bearer 토큰 형태로 받을 때 사용 (Access Token)
  // required = false로 설정하여 Authorization 헤더가 없어도 Refresh Token만으로 재발급 가능
  public Map<String, Object> refresh(@RequestHeader(value = "Authorization", required = false) String authHeader, HttpServletRequest request, HttpServletResponse response){

    String refreshToken = RefreshCookieUtil.get(request);
    if(refreshToken == null || refreshToken.isBlank()) {
      throw new CustomJWTException("NULL_REFRESH");
    }
    
    // Authorization 헤더 처리 (있으면 사용, 없으면 Refresh Token만으로 진행)
    String accessToken = null;
    String expectedEmail = null;
    
    if (authHeader != null && authHeader.length() >= 7) {
      // "Bearer " 접두사 제거 → 실제 토큰 추출
      accessToken = authHeader.substring(7);
      
      // accessToken이 "undefined" 또는 빈 문자열이면 null로 처리
      if (accessToken == null || accessToken.isBlank() || "undefined".equals(accessToken)) {
        accessToken = null;
      } else {
        // access token 타입 확인 (만료 여부와 무관하게 Refresh 토큰을 Access처럼 쓰는 우회 방지)
        try {
          Map<String, Object> accessClaims = JWTUtil.getClaimsAllowExpired(accessToken);
          Object tokenType = accessClaims.get("tokenType");
          if (tokenType != null && !TokenType.ACCESS.name().equals(tokenType.toString())) {
            throw new CustomJWTException("INVALID_TOKEN_TYPE");
          }
        } catch (CustomJWTException e) {
          // CustomJWTException은 그대로 전파
          throw e;
        } catch (Exception e) {
          // MalFormed, Invalid 등의 에러는 무시하고 null로 처리
          // (손상된 토큰은 Refresh Token만으로 진행)
          accessToken = null;
        }

        // Access 토큰이 유효하고 만료되지 않았다면 그대로 반환
        if (accessToken != null && JWTUtil.isExpired(accessToken) == false) {
          return Map.of("accessToken", accessToken);
        }

        // Access 토큰은 만료되어도 email 정도는 비교 용도로 추출 가능(만료 허용 파싱). 실패 시 null 처리.
        if (accessToken != null) {
          try {
            Map<String, Object> accessClaims = JWTUtil.getClaimsAllowExpired(accessToken);
            expectedEmail = (String) accessClaims.get("email");
          } catch (Exception e) {
            // Access Token 파싱 실패 시 null로 처리 (refresh token만으로 진행)
          }
        }
      }
    }
    
    // accessToken이 없거나 유효하지 않으면 Refresh Token만으로 진행

    // ========== Refresh Token 회전 (성능 측정) ==========
    long startTime = System.nanoTime();
    String newRefreshToken = refreshTokenService.rotateRefreshToken(refreshToken, request, expectedEmail);
    long rotationEndTime = System.nanoTime();
    int maxAgeSeconds = refreshTokenService.refreshCookieMaxAgeSeconds();
    long endTime = System.nanoTime();
    
    long rotationDurationMs = (rotationEndTime - startTime) / 1_000_000;
    long totalDurationMs = (endTime - startTime) / 1_000_000;
    
    log.info("=== Refresh Token Rotation Performance ===");
    log.info("Storage: {} | Rotation: {} ms | Total: {} ms", storageType, rotationDurationMs, totalDurationMs);
    // ====================================================
    
    RefreshCookieUtil.set(request, response, newRefreshToken, maxAgeSeconds);

    // 새 Access 발급(Stateless) - refresh claims를 그대로 쓰지 않고 최소한의 정보만 사용
    Map<String, Object> refreshClaims = JWTUtil.validateToken(newRefreshToken);
    String email = (String) refreshClaims.get("email");

    // 기존 access의 사용자 클레임을 최대한 유지 (프론트 호환)
    Map<String, Object> newAccessClaims = new HashMap<>();
    
    // 1순위: accessToken이 있으면 oldAccessClaims에서 가져오기 시도
    if (accessToken != null) {
      try {
        Map<String, Object> oldAccessClaims = JWTUtil.getClaimsAllowExpired(accessToken);
        
        if (oldAccessClaims != null && !oldAccessClaims.isEmpty()) {
          // 주체 일치 확인
          if (oldAccessClaims.get("email") != null && !email.equals(oldAccessClaims.get("email"))) {
            throw new CustomJWTException("REFRESH_BINDING_MISMATCH");
          }
          
          // oldAccessClaims 복사
          newAccessClaims.putAll(oldAccessClaims);
          
          // 표준 클레임/내부키 제거(재발급 시 JWTUtil이 새 exp/iat를 설정)
          newAccessClaims.remove("exp");
          newAccessClaims.remove("iat");
          newAccessClaims.remove("nbf");
          newAccessClaims.remove("jti");
          newAccessClaims.remove("familyId");
        }
      } catch (CustomJWTException e) {
        // CustomJWTException은 그대로 전파
        throw e;
      } catch (Exception e) {
        // MalFormed 등의 에러는 무시하고 DB에서 조회
      }
    }
    
    // 2순위: newAccessClaims가 비어있으면 DB에서 사용자 정보 조회
    if (newAccessClaims.isEmpty()) {
      Member member = memberRepository.getWithRoles(email);
      
      if (member == null || member.isDeleted() || !member.isApproved()) {
        throw new CustomJWTException("INVALID_USER");
      }
      
      // MemberDTO의 getClaims() 메서드와 동일한 형식으로 생성
      MemberDTO memberDTO = new MemberDTO(
          member.getEmail(),
          member.getPw(),
          member.getNickname(),
          member.isSocial(),
          member.getDepartment() != null ? member.getDepartment().name() : "",
          member.isApproved(),
          member.getRoleList().stream().map(Enum::name).collect(Collectors.toList()),
          member.isFaceEnabled()
      );
      
      newAccessClaims = memberDTO.getClaims();
    }

    newAccessClaims.put("tokenType", TokenType.ACCESS.name());
    newAccessClaims.put("auth_time", Instant.now().getEpochSecond());
    String newAccessToken = JWTUtil.generateToken(newAccessClaims, 15);

    return Map.of("accessToken", newAccessToken);

  }
}