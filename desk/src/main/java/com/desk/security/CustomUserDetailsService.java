package com.desk.security;

import com.desk.domain.Member;
import com.desk.dto.MemberDTO;
import com.desk.repository.MemberRepository;
import com.desk.security.token.LoginLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@Log4j2
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;
    private final LoginLockService loginLockService;

    @Override
    public UserDetails loadUserByUsername(String username) throws BadCredentialsException {
        log.info("loadUserByUsername: {}", username);

        // 로그인 잠금 체크
        if (loginLockService.isLocked(username)) {
            int remainingMinutes = loginLockService.getRemainingLockMinutes(username);
            String message = String.format("로그인이 잠겨 있습니다. 남은 시간: %d분", remainingMinutes);
            throw new BadCredentialsException(message);
        }

        Member member = memberRepository.getWithRoles(username);

        if (member == null) {
            throw new BadCredentialsException("Not Found");
        }

        // 1. 삭제된 회원 체크
        if (member.isDeleted()) {
            throw new BadCredentialsException("DELETED_ACCOUNT");
        }

        // 2. 미승인 회원 체크
        if (!member.isApproved()) {
            throw new BadCredentialsException("PENDING_APPROVAL");
        }

        // DB에서 가져온 회원 정보를 Spring Security 인증 객체(MemberDTO → UserDetails 구현체)로 변환
        MemberDTO memberDTO = new MemberDTO(
                member.getEmail(),
                member.getPw(),
                member.getNickname(),
                member.isSocial(),
                member.getDepartment() != null ? member.getDepartment().name() : "",
                member.isApproved(),
                member.getRoleList().stream().map(Enum::name).collect(Collectors.toList()),
                member.isFaceEnabled());

        return memberDTO;
    }
}
