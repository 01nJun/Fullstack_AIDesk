package com.desk.controller;

import com.desk.dto.MemberDTO;
import com.desk.dto.MemberJoinDTO;
import com.desk.service.FaceService;
import com.desk.dto.PageRequestDTO;
import com.desk.dto.PageResponseDTO;
import com.desk.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.desk.domain.Member;
import com.desk.repository.MemberRepository;

@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/api/member")
public class MemberController {

    private final MemberService memberService;
    private final FaceService faceService;
    private final MemberRepository memberRepository;

    @PostMapping("/join")
    public Map<String, String> join(@RequestBody MemberJoinDTO memberJoinDTO) {
        log.info("member join: " + memberJoinDTO);

        memberService.join(memberJoinDTO);

        return Map.of("result", "success");
    }

    @PostMapping("/face-register")
    public Map<String, String> faceRegister(@RequestParam String email, @RequestParam MultipartFile faceFile) throws IOException {
        faceService.registerFace(email, faceFile);
        return Map.of("result", "success");
    }

    @PutMapping("/update-face-status")
    public Map<String, String> updateFaceStatus(@RequestBody Map<String, Object> params) {
        faceService.updateFaceStatus((String) params.get("email"), (Boolean) params.get("status"));
        return Map.of("result", "success");
    }

    // 일반 사용자용 멤버 검색 API (승인된 멤버만 검색)
    @GetMapping("/search")
    public PageResponseDTO<MemberDTO> searchMembers(
            PageRequestDTO pageRequestDTO,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "department", required = false) String department) {
        log.info("member search - keyword: {}, department: {}", keyword, department);
        return memberService.searchActiveMembers(pageRequestDTO, keyword, department);
    }

    // 담당자 정보 조회 API (email로 부서, 닉네임 조회)
    @GetMapping("/info/{email}")
    public ResponseEntity<Map<String, String>> getMemberInfo(@PathVariable("email") String email) {
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        log.info("담당자 정보 조회 요청: {}", email);
        Optional<Member> member = memberRepository.findById(email);
        if (member.isPresent()) {
            Member m = member.get();
            Map<String, String> info = new HashMap<>();
            info.put("email", m.getEmail());
            info.put("nickname", m.getNickname() != null ? m.getNickname() : "");
            info.put("department", m.getDepartment() != null ? m.getDepartment().name() : "");
            return ResponseEntity.ok(info);
        }
        return ResponseEntity.notFound().build();
    }
}