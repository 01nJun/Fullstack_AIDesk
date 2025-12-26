 package com.desk.controller;

 import com.desk.dto.PageRequestDTO;
 import com.desk.dto.PageResponseDTO;
 import com.desk.dto.ReplyDTO;
 import com.desk.service.ReplyService;
 import lombok.RequiredArgsConstructor;
 import lombok.extern.log4j.Log4j2;
 import org.springframework.web.bind.annotation.*;

 import java.util.Map; // [주의] Map을 쓰려면 이 친구가 꼭 필요해요!

 @RestController
 @RequiredArgsConstructor
 @RequestMapping("/api/replies")
 @Log4j2
 public class ReplyController {

     private final ReplyService replyService;

     // 댓글 등록: POST /api/replies/
     @PostMapping("/")
     public Map<String, Long> register(@RequestBody ReplyDTO replyDTO) {
         log.info("댓글 등록 요청: " +    replyDTO);
        
         Long rno = replyService.register(replyDTO);
        
         return Map.of("rno", rno);
     }

     // 특정 게시물의 댓글 목록: GET /api/replies/list/41
     @GetMapping("/list/{bno}")
     public PageResponseDTO<ReplyDTO> getList(
             @PathVariable("bno") Long bno,
             PageRequestDTO pageRequestDTO) {
        
         log.info("댓글 목록 조회 - 게시글 번호: " + bno);
        
         return replyService.getListOfBoard(bno, pageRequestDTO);
     }
 }