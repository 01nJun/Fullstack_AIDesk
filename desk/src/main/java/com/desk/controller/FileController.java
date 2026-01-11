package com.desk.controller;

import com.desk.dto.PageRequestDTO;
import com.desk.dto.PageResponseDTO;
import com.desk.dto.TicketFileDTO;
import com.desk.dto.TicketFilterDTO;
import com.desk.domain.TicketFile;
import com.desk.domain.ChatFile;
import com.desk.repository.TicketFileRepository;
import com.desk.repository.chat.ChatFileRepository;
import com.desk.service.FileService;
import com.desk.util.CustomFileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/api/files")
public class FileController {
    private final FileService fileService; // 파일함 조회 로직 담당 서비스
    private final CustomFileUtil fileUtil;
    private final TicketFileRepository ticketFileRepository;
    private final ChatFileRepository chatFileRepository;

    // 파일함 목록 조회 (전체/보낸/받은 탭 통합)
    @GetMapping("/list")
    public ResponseEntity<PageResponseDTO<TicketFileDTO>> getFileBox(
            @RequestParam("email") String email,
            @RequestParam("type") String type, // ALL, SENT, RECEIVED
            @ModelAttribute TicketFilterDTO filter, // 검색어 포함
            @ModelAttribute PageRequestDTO pageRequestDTO
    ) {
        return ResponseEntity.ok(fileService.getFileBoxList(email, type, filter, pageRequestDTO));
    }

    // 이미지 보기 (이미지 태그의 src에서 호출)
    @GetMapping("/view/{fileName}")
    public ResponseEntity<Resource> viewFile(@PathVariable("fileName") String fileName) {
        return fileUtil.getFile(fileName, null);
    }

    // 파일 다운로드 (알림창 확인 후 호출)
    @GetMapping("/download/{fileName}")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable("fileName") String fileName,
            @RequestParam(value = "originalName", required = false) String ignoredOriginalName,
            Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String email = principal.getName();

        // ✅ DB에서 원본 파일명 조회 + 접근 권한 체크 (ticket_file → chat_file 순)
        Optional<TicketFile> tf = ticketFileRepository.findById(fileName);
        if (tf.isPresent()) {
            TicketFile f = tf.get();
            boolean allowed = email.equals(f.getWriter()) || (f.getReceiver() != null && f.getReceiver().contains(email));
            if (!allowed) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            return fileUtil.getFile(fileName, f.getFileName());
        }

        Optional<ChatFile> cf = chatFileRepository.findById(fileName);
        if (cf.isPresent()) {
            ChatFile f = cf.get();
            // ✅ 작성자 본인이면 무조건 허용 (권한 체크 쿼리 패스)
            boolean isWriter = f.getWriter() != null && f.getWriter().equals(email);

            if (!isWriter && !chatFileRepository.existsAccessibleChatFileByUuid(fileName, email)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            return fileUtil.getFile(fileName, f.getFileName());
        }

        return ResponseEntity.notFound().build();
    }
    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> deleteFile(@PathVariable("uuid") String uuid, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        String email = principal.getName();
        log.info("[File] 삭제 요청 | uuid={} | requester={}", uuid, email);
        fileService.deleteFile(uuid, email);
        return ResponseEntity.noContent().build();
    }
}