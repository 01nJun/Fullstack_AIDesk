//package com.desk.service;
//
//import com.desk.dto.MemberDTO;
//import com.desk.dto.PageRequestDTO;
//import com.desk.dto.PageResponseDTO;
//import com.desk.domain.Member;
//import com.desk.repository.MemberRepository;
//import lombok.extern.log4j.Log4j2;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//@SpringBootTest
//@Log4j2
//public class AdminServiceTests {
//
//    @Autowired
//    private AdminService adminService;
//
//    @Autowired
//    private MemberRepository memberRepository;
//
//    @Test
//    @Transactional
//    public void testGetPendingMembers() {
//        // When
//        List<MemberDTO> pendingList = adminService.getPendingMembers();
//
//        // Then
//        log.info("승인 대기 회원 수: " + pendingList.size());
//        pendingList.forEach(log::info);
//    }
//
//    @Test
//    @Transactional
//    public void testGetActiveMembersWithPaging() {
//        // Given
//        PageRequestDTO pageRequestDTO = PageRequestDTO.builder()
//                .page(1)
//                .size(10)
//                .build();
//
//        // When
//        PageResponseDTO<MemberDTO> response = adminService.getActiveMembers(pageRequestDTO);
//
//        // Then
//        assertThat(response.getDtoList()).isNotEmpty();
//        log.info("현재 페이지: " + response.getPageRequestDTO().getPage());
//        log.info("전체 승인 회원 수: " + response.getTotalCount());
//        response.getDtoList().forEach(log::info);
//    }
//
//    @Test
//    @Transactional
//    public void testApproveMember() {
//        // Given (승인 대기 중인 이메일 선택)
//        String email = "pending51@desk.com";
//
//        // When
//        adminService.approveMember(email);
//
//        // Then
//        Member member = memberRepository.findById(email).orElseThrow();
//        assertThat(member.isApproved()).isTrue();
//        log.info("회원 승인 완료 확인: " + member.getEmail());
//    }
//
//    @Test
//    @Transactional
//    public void testDeleteMember() {
//        // Given
//        String email = "user2@desk.com";
//
//        // When
//        adminService.deleteMember(email);
//
//        // Then
//        Member member = memberRepository.findById(email).orElseThrow();
//        assertThat(member.isDeleted()).isTrue();
//        log.info("회원 삭제(Soft Delete) 완료 확인: " + member.getEmail());
//    }
//}