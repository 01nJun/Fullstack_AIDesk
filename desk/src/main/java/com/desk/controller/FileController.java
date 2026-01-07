package com.desk.controller;

import com.desk.dto.PageRequestDTO;
import com.desk.dto.PageResponseDTO;
import com.desk.dto.TicketFileDTO;
import com.desk.dto.TicketFilterDTO;
import com.desk.service.FileService;
import com.desk.util.CustomFileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/api/files")
public class FileController {
    private final FileService fileService; // 파일함 조회 로직 담당 서비스
    private final CustomFileUtil fileUtil;

    // 파일함 목록 조회 (전체/보낸/받은 탭 통합)
    @GetMapping("/list")
    public ResponseEntity<PageResponseDTO<TicketFileDTO>> getFileBox(
            @RequestParam String email,
            @RequestParam String type, // ALL, SENT, RECEIVED
            @ModelAttribute TicketFilterDTO filter, // 검색어 포함
            @ModelAttribute PageRequestDTO pageRequestDTO
    ) {
        return ResponseEntity.ok(fileService.getFileBoxList(email, type, filter, pageRequestDTO));
    }

    // 이미지 보기 (이미지 태그의 src에서 호출)
    @GetMapping("/view/{fileName}")
    public ResponseEntity<Resource> viewFile(@PathVariable String fileName) {
        return fileUtil.getFile(fileName, null);
    }

    // 파일 다운로드 (알림창 확인 후 호출)
    @GetMapping("/download/{fileName}")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable String fileName,
            @RequestParam String originalName) {
        return fileUtil.getFile(fileName, originalName);
    }
    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> deleteFile(@PathVariable String uuid) {
        log.info("[File] 삭제 요청 | uuid={}", uuid);
        fileService.deleteFile(uuid);
        return ResponseEntity.noContent().build();
    }
}