package com.desk.controller;

import com.desk.service.SttService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/stt")
@RequiredArgsConstructor
@Log4j2
public class SttController {

    private final SttService sttService;  // 주석 해제

    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, String>> uploadAudio(
            @RequestParam("file") MultipartFile file) throws IOException {
        log.info("Stt 요청 받음: 파일명 = {}, 크기 = {} bytes",
                file.getOriginalFilename(), file.getSize());

        try {
            // STT 서비스 호출
            String text = sttService.stt(file.getBytes());
            return ResponseEntity.ok(Map.of("text", text));
        } catch (Exception e) {
            log.error("STT 처리 중 오류 발생", e);
            return ResponseEntity.ok(Map.of("text", "")); // 에러 시 빈 문자열 반환
        }
    }
}