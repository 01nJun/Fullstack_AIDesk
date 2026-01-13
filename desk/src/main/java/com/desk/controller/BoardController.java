package com.desk.controller;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.desk.dto.PageRequestDTO;
import com.desk.dto.PageResponseDTO;
import com.desk.dto.BoardDTO;
import com.desk.service.BoardService;

@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/api/board")
public class BoardController {
  
    private final BoardService service;

    //  개별 조회 - (SecurityConfig에서 permitAll 설정됨)
    @GetMapping("/{bno}")
    public BoardDTO get(@PathVariable(name ="bno") Long bno) {
        log.info("--- 게시글 상세 조회 번호: " + bno);
        return service.get(bno);
    }

    // 목록 조회 (페이징 처리) - (SecurityConfig에서 permitAll 설정됨)
    @GetMapping("/list")
    public PageResponseDTO<BoardDTO> list(PageRequestDTO pageRequestDTO) {
        log.info("--- 게시글 목록 요청: " + pageRequestDTO);
        return service.list(pageRequestDTO);
    }

    //  등록 - (일반 로그인 사용자 권한 필요)
    // 만약 등록도 관리자만 하게 하려면 @PreAuthorize("hasRole('ADMIN')")를 추가하세요.
    @PostMapping("/")
    public Map<String, Long> register(@RequestBody BoardDTO boardDTO) {
        log.info("--- 게시글 등록 시작: " + boardDTO);
        
        Long bno = service.register(boardDTO);
        
        return Map.of("BNO", bno);
    }

    //  ADMIN 권한 확인
    @PreAuthorize("hasRole('ADMIN')") 
    @PutMapping("/{bno}")
    public Map<String, String> modify(@PathVariable(name="bno") Long bno, @RequestBody BoardDTO boardDTO) {
        
        boardDTO.setBno(bno); // URL의 번호를 DTO에 주입
        log.info("--- 게시글 수정 시작(관리자 전용): " + boardDTO);

        service.modify(boardDTO);
        
        return Map.of("RESULT", "SUCCESS");
    }

    // ADMIN 권한 확인
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{bno}")
    public Map<String, String> remove(@PathVariable(name="bno") Long bno) {
        
        log.info("--- 게시글 삭제 시작(관리자 전용): " + bno);

        service.remove(bno);
        
        return Map.of("RESULT", "SUCCESS");
    }
}