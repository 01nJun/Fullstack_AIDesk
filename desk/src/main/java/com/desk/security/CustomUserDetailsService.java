package com.desk.security;

import com.desk.domain.Member;
import com.desk.dto.MemberDTO;
import com.desk.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;


@Service
@Log4j2
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    // Spring Security가 로그인 처리 시 자동 호출, username → 로그인 시 사용자가 입력한 email 혹은 아이디
    @Override
    public UserDetails loadUserByUsername(String username) throws BadCredentialsException {

        log.info("----------------loadUserByUsername-----------------------------");

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
                member.getDepartment() != null ? member.getDepartment().name() : "", // Enum -> String
                member.isApproved(),
                member.getRoleList().stream().map(Enum::name).collect(Collectors.toList()),
                member.isFaceEnabled());

        // DTO에도 부서 정보 등을 담고 싶다면 MemberDTO 필드 추가 필요
        return memberDTO;
        // 사용자 정보를 DB에서 조회해서 UserDetails로 반환
        // 비밀번호 비교 → Spring Security 내부(DaoAuthenticationProvider + PasswordEncoder)에서 자동으로 처리
    }
}