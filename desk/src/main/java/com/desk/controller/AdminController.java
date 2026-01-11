package com.desk.controller;

import com.desk.dto.MemberDTO;
import com.desk.dto.PageRequestDTO;
import com.desk.dto.PageResponseDTO;
import com.desk.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Log4j2
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class AdminController {

    private final AdminService adminService; // MemberService 대신 AdminService 분리 추천
    // 1. 승인 대기 목록 조회
    @GetMapping("/pending")
    public PageResponseDTO<MemberDTO> getPendingMembers(
            PageRequestDTO pageRequestDTO,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "department", required = false) String department) {
        return adminService.getPendingMembers(pageRequestDTO, keyword, department);
    }

    // 2. 전체 직원 목록 조회
    @GetMapping("/active")
    public PageResponseDTO<MemberDTO> getActiveMembers(
            PageRequestDTO pageRequestDTO,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "department", required = false) String department) {
        return adminService.getActiveMembers(pageRequestDTO, keyword, department);
    }

    // 3. 승인
    @PutMapping("/approve/{email}")
    public Map<String, String> approveMember(@PathVariable("email") String email) {
        adminService.approveMember(email);
        return Map.of("result", "SUCCESS");
    }

    // 4. 삭제
    @PutMapping("/delete/{email}")
    public Map<String, String> deleteMember(@PathVariable("email") String email) {
        adminService.deleteMember(email);
        return Map.of("result", "SUCCESS");
    }
}