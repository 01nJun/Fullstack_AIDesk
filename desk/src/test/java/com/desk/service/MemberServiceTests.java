//package com.desk.service;
//
//import com.desk.domain.Department;
//import com.desk.domain.Member;
//import com.desk.dto.MemberJoinDTO;
//import com.desk.dto.MemberModifyDTO;
//import com.desk.repository.MemberRepository;
//import com.desk.util.MemberExistException;
//import lombok.extern.log4j.Log4j2;
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.Optional;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//@SpringBootTest
//@Log4j2
//public class MemberServiceTests {
//
//    @Autowired
//    private MemberService memberService;
//
//    @Autowired
//    private MemberRepository memberRepository;
//
//    @Test
//    @Transactional
//    public void testJoin() {
//        // Given
//        MemberJoinDTO joinDTO = MemberJoinDTO.builder()
//                .email("test_join@desk.com")
//                .pw("password123")
//                .nickname("테스터")
//                .department("DESIGN") // Enum에 존재하는 값이어야 함
//                .build();
//
//        // When
//        memberService.join(joinDTO);
//
//        // Then
//        Optional<Member> result = memberRepository.findById("test_join@desk.com");
//        assertThat(result.isPresent()).isTrue();
//        assertThat(result.get().getNickname()).isEqualTo("테스터");
//        log.info("회원가입 성공: " + result.get());
//    }
//
//    @Test
//    public void testJoinDuplicateEmail() {
//        // 이미 존재하는 이메일 설정
//        String existingEmail = "user1@desk.com";
//
//        MemberJoinDTO joinDTO = MemberJoinDTO.builder()
//                .email(existingEmail)
//                .pw("1111")
//                .nickname("중복이")
//                .department("HR")
//                .build();
//
//        // 중복 가입 시 Exception이 발생하는지 확인
//        Assertions.assertThrows(MemberExistException.class, () -> {
//            memberService.join(joinDTO);
//        });
//    }
//
//    @Test
//    @Transactional
//    public void testModifyMember() {
//        // Given (이미 존재하는 회원의 정보 수정)
//        MemberModifyDTO modifyDTO = MemberModifyDTO.builder()
//                .email("user1@desk.com")
//                .pw("newPassword")
//                .nickname("수정된닉네임")
//                .department("SALES")
//                .build();
//
//        // When
//        memberService.modifyMember(modifyDTO);
//
//        // Then
//        Member updatedMember = memberRepository.findById("user1@desk.com").orElseThrow();
//        assertThat(updatedMember.getNickname()).isEqualTo("수정된닉네임");
//        log.info("회원정보 수정 완료: " + updatedMember);
//    }
//}