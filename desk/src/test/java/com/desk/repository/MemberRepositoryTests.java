package com.desk.repository;

import com.desk.domain.Department;
import com.desk.domain.Member;
import com.desk.domain.MemberRole;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.IntStream;

@SpringBootTest
@Log4j2
public class MemberRepositoryTests {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @Transactional
    @Commit // 실제 DB에 반영하고 싶을 때 사용
    public void insertDummies() {
        // 1. 일반 회원 (승인 완료) 50명 생성
        IntStream.rangeClosed(1, 50).forEach(i -> {
            Member member = Member.builder()
                    .email("user" + i + "@desk.com")
                    .pw(passwordEncoder.encode("1111"))
                    .nickname("사원" + i)
                    .social(false)
                    .department(i % 2 == 0 ? Department.DEVELOPMENT : Department.HR) // 가상의 부서
                    .isApproved(true)
                    .isDeleted(false)
                    .build();
            member.addRole(MemberRole.USER);
            memberRepository.save(member);
        });

        // 2. 승인 대기 중인 회원 10명 생성
        IntStream.rangeClosed(51, 60).forEach(i -> {
            Member member = Member.builder()
                    .email("pending" + i + "@desk.com")
                    .pw(passwordEncoder.encode("1111"))
                    .nickname("대기자" + i)
                    .social(false)
                    .department(Department.SALES)
                    .isApproved(false)
                    .isDeleted(false)
                    .build();
            member.addRole(MemberRole.USER);
            memberRepository.save(member);
        });

        log.info("더미 데이터 생성 완료");
    }

    @Test
    @Transactional
    @Commit // 실제 DB에 반영하기 위해 추가
    public void testUpdateMemberRole() {
        // 1. 9번째 직원 이메일 지정
        String email = "user9@desk.com";

        // 2. 해당 회원 조회 (권한 정보까지 같이 가져오기 위해 getWithRoles 사용)
        Member member = memberRepository.getWithRoles(email);

        if (member != null) {
            // 3. ADMIN 권한 추가 (중복 방지 로직이 addRole 내부에 있다면 안심하고 추가 가능)
            // MemberRole.ADMIN 이 정의되어 있다고 가정합니다.
            member.addRole(MemberRole.ADMIN);

            // 4. 저장 (JPA Dirty Checking에 의해 사실 save를 호출하지 않아도 트랜잭션 종료 시 반영되지만 명시적 작성)
            memberRepository.save(member);

            log.info("-------------------------------------------");
            log.info("회원 " + email + "에게 ADMIN 권한이 부여되었습니다.");
            log.info("현재 권한 목록: " + member.getRoleList());
        } else {
            log.error("해당 이메일을 가진 회원을 찾을 수 없습니다: " + email);
        }
    }

    @Test
    public void testReadMember() {
        String email = "user1@desk.com";
        Member member = memberRepository.getWithRoles(email);
        log.info("-----------------------");
        log.info(member);
        log.info(member.getRoleList());
    }
}
