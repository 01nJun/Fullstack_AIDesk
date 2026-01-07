package com.desk.controller;

import com.desk.util.CustomJWTException;
import com.desk.util.JWTUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Log4j2
public class APIRefreshController {

  @RequestMapping("/api/member/refresh") 
  // HTTP 요청 헤더 중 Authorization 값을 가져와서 authHeader 변수에 담음 → 일반적으로 JWT를 Bearer 토큰 형태로 받을 때 사용 (Access Token)
  public Map<String, Object> refresh(@RequestHeader("Authorization") String authHeader,
                                     @RequestParam("refreshToken") String refreshToken){
    // refreshToken 없으면 예외
    if(refreshToken == null) {
      throw new CustomJWTException("NULL_REFRASH");
    }
    // Authorization 헤더 없거나 형식이 잘못되면 예외
    if(authHeader == null || authHeader.length() < 7) {
      throw new CustomJWTException("INVALID_STRING");
    }
    // "Bearer " 접두사 제거 → 실제 토큰 추출
    String accessToken = authHeader.substring(7);

    //Access 토큰이 만료되지 않았다면 그대로 반환
     if (JWTUtil.isExpired(accessToken) == false) {
      return Map.of("accessToken", accessToken, "refreshToken", refreshToken);
    }
    //Refresh토큰 검증 
    Map<String, Object> claims = JWTUtil.validateToken(refreshToken);

    log.info("refresh ... claims: " + claims);

    String newAccessToken = JWTUtil.generateToken(claims, 60);

    // refreshToken 재발급 여부 결정: 만료까지 1시간 미만 → 새 refreshToken 발급, 아직 충분히 남음 → 기존 refreshToken 사용
    String newRefreshToken = JWTUtil.checkTime((Integer) claims.get("exp")) == true 
        ? JWTUtil.generateToken(claims, 60 * 24) 
        : refreshToken;


    return Map.of("accessToken", newAccessToken, "refreshToken", newRefreshToken);

  }
}