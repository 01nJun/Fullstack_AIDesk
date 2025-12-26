package com.desk.controller;

import com.desk.dto.MemberJoinDTO;
import com.desk.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/api/member")
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/join")
    public Map<String, String> join(@RequestBody MemberJoinDTO memberJoinDTO) {
        log.info("member join: " + memberJoinDTO);

        memberService.join(memberJoinDTO);

        return Map.of("result", "success");
    }
}