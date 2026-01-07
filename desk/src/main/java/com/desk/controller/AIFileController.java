package com.desk.controller;

import com.desk.domain.TicketFile;
import com.desk.dto.AIFileRequestDTO;
import com.desk.dto.AIFileResponseDTO;
import com.desk.repository.TicketFileRepository;
import com.desk.service.AIFileService;
import com.desk.util.CustomFileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Optional;

@RestController
@RequestMapping("/api/ai/file")
@RequiredArgsConstructor
@Log4j2
public class AIFileController {

    private final AIFileService aiFileService;
    private final TicketFileRepository ticketFileRepository;
    private final CustomFileUtil fileUtil;

    @PostMapping("/chat")
    @PreAuthorize("isAuthenticated()")
    public AIFileResponseDTO chat(@RequestBody AIFileRequestDTO request, Principal principal) {
        String email = principal.getName();
        log.info("[AI File] Chat | email={} | convId={}", email, request.getConversationId());
        return aiFileService.chat(email, request);
    }

    @GetMapping("/view/{uuid}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> view(@PathVariable String uuid, Principal principal) {
        if (uuid == null || uuid.isBlank() || principal == null) {
            return ResponseEntity.badRequest().build();
        }
        String email = principal.getName();
        if (!ticketFileRepository.existsAccessibleFileByUuid(uuid, email)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return fileUtil.getFile(uuid, null);
    }

    @GetMapping("/download/{uuid}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> download(@PathVariable String uuid, Principal principal) {
        if (uuid == null || uuid.isBlank() || principal == null) {
            return ResponseEntity.badRequest().build();
        }
        String email = principal.getName();
        if (!ticketFileRepository.existsAccessibleFileByUuid(uuid, email)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Optional<TicketFile> found = ticketFileRepository.findById(uuid);
        if (found.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        String originalName = found.get().getFileName();
        // 다운로드 파일명은 원본 파일명 그대로 사용
        return fileUtil.getFile(uuid, originalName);
    }
}


