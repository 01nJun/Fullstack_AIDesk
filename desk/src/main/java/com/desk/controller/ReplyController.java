package com.desk.controller;

import com.desk.dto.PageRequestDTO;
import com.desk.dto.PageResponseDTO;
import com.desk.dto.ReplyDTO;
import com.desk.service.ReplyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/replies")
@Log4j2
public class ReplyController {

    private final ReplyService replyService;

    // 1. 댓글 등록: POST /api/replies/
    @PostMapping("/")
    public Map<String, Long> register(@RequestBody ReplyDTO replyDTO) {
        log.info("댓글 등록 요청: " + replyDTO);
        Long rno = replyService.register(replyDTO);
        return Map.of("rno", rno);
    }

    // 2. 특정 게시물의 댓글 목록: GET /api/replies/list/{bno}
    @GetMapping("/list/{bno}")
    public PageResponseDTO<ReplyDTO> getList(
            @PathVariable("bno") Long bno,
            PageRequestDTO pageRequestDTO) {
        log.info("댓글 목록 조회 - 게시글 번호: " + bno);
        return replyService.getListOfBoard(bno, pageRequestDTO);
    }

    // 3. 댓글 수정: PUT /api/replies/{rno}
    // [권한 규칙] 서비스 계층(ReplyServiceImpl)에서 본인 여부를 체크합니다.
    @PutMapping("/{rno}")
    public Map<String, String> modify(
            @PathVariable("rno") Long rno,
            @RequestBody ReplyDTO replyDTO) {

        log.info("댓글 수정 요청 - 번호: " + rno + ", 내용: " + replyDTO);

        // PathVariable로 받은 rno를 DTO에 반드시 세팅해야 서비스에서 인식합니다.
        replyDTO.setRno(rno);
        replyService.modify(replyDTO);

        return Map.of("RESULT", "SUCCESS");
    }

    // 4. 댓글 삭제: DELETE /api/replies/{rno}
    // [권한 규칙] 서비스 계층에서 본인 또는 관리자(ADMIN) 여부를 체크합니다.
    @DeleteMapping("/{rno}")
    public Map<String, String> remove(@PathVariable("rno") Long rno) {

        log.info("댓글 삭제 요청 - 번호: " + rno);
        replyService.remove(rno);

        return Map.of("RESULT", "SUCCESS");
    }
}
