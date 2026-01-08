package com.desk.controller.chat;

import com.desk.dto.chat.ChatFileDTO;
import com.desk.service.chat.ChatFileService;
import com.desk.util.CustomFileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

/**
 * 채팅 파일 컨트롤러
 */
@RestController
@RequestMapping("/api/chat/files")
@RequiredArgsConstructor
@Log4j2
public class ChatFileController {

    private final ChatFileService chatFileService;
    private final CustomFileUtil fileUtil;

    /**
     * POST /api/chat/files
     * 파일 업로드 (TEMP 상태로 저장)
     */
    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<ChatFileDTO> uploadFile(
            @RequestParam("chatRoomId") Long chatRoomId,
            @RequestParam("file") MultipartFile file,
            Principal principal) {
        String userId = principal.getName();
        log.info("[ChatFile] 업로드 요청 | roomId={} | userId={} | fileName={}", 
                chatRoomId, userId, file.getOriginalFilename());

        ChatFileDTO dto = chatFileService.uploadFile(chatRoomId, file, userId);
        return ResponseEntity.ok(dto);
    }

    /**
     * GET /api/chat/files/{uuid}/download
     * 파일 다운로드
     * JWT 필터를 거치지 않으므로 Principal은 null일 수 있음
     */
    @GetMapping("/{uuid}/download")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable String uuid,
            Principal principal) {
        // JWT 필터를 거치지 않으므로 principal이 null일 수 있음
        String userId = principal != null ? principal.getName() : null;
        log.info("[ChatFile] 다운로드 요청 | uuid={} | userId={}", uuid, userId);

        // 인증 없이 파일 조회
        ChatFileDTO fileDTO = chatFileService.getFileWithoutAuth(uuid);
        // uuid가 파일 저장명이므로 그대로 사용, fileName을 전달하여 다운로드로 처리
        return fileUtil.getFile(fileDTO.getUuid(), fileDTO.getFileName());
    }

    /**
     * GET /api/chat/files/{uuid}/view
     * 파일 보기 (이미지 프리뷰용, Content-Disposition: inline)
     * JWT 필터를 거치지 않으므로 Principal은 null일 수 있음
     */
    @GetMapping("/{uuid}/view")
    public ResponseEntity<Resource> viewFile(
            @PathVariable String uuid,
            Principal principal) {
        // JWT 필터를 거치지 않으므로 principal이 null일 수 있음
        // 파일 조회를 위해 userId는 null로 처리 (서비스에서 인증 없이 조회 가능하도록 수정 필요)
        String userId = principal != null ? principal.getName() : null;
        log.info("[ChatFile] 보기 요청 | uuid={} | userId={}", uuid, userId);

        // 인증 없이 파일 조회 (서비스 메서드 수정 필요)
        ChatFileDTO fileDTO = chatFileService.getFileWithoutAuth(uuid);
        
        // CustomFileUtil의 헬퍼 메서드 사용 (inline 처리)
        // uuid가 파일 저장명이므로 그대로 사용
        return fileUtil.getFileInline(fileDTO.getUuid(), fileDTO.getMimeType());
    }

    /**
     * GET /api/chat/files/message/{messageId}
     * 메시지에 첨부된 파일 목록 조회
     */
    @GetMapping("/message/{messageId}")
    public ResponseEntity<java.util.List<ChatFileDTO>> getFilesByMessage(
            @PathVariable Long messageId,
            Principal principal) {
        String userId = principal.getName();
        log.info("[ChatFile] 메시지 파일 목록 조회 | messageId={} | userId={}", messageId, userId);

        java.util.List<ChatFileDTO> files = chatFileService.getFilesByMessageId(messageId, userId);
        return ResponseEntity.ok(files);
    }
}

