package com.desk.security.filter;

import com.desk.dto.MemberDTO;
import com.desk.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FaceAuthenticationProvider implements AuthenticationProvider {
    private final CustomUserDetailsService userDetailsService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String email = (String) authentication.getPrincipal();
        MemberDTO memberDTO = (MemberDTO) userDetailsService.loadUserByUsername(email);

        // 얼굴 로그인 비활성 상태 체크
        if (!memberDTO.isFaceEnabled()) {
            throw new BadCredentialsException("FACE_LOGIN_DISABLED");
        }

        // 인증 성공 토큰 반환
        return new FaceAuthenticationToken(memberDTO, memberDTO.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return FaceAuthenticationToken.class.isAssignableFrom(authentication);
    }
}