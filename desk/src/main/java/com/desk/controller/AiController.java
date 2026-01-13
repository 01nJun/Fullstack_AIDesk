package com.desk.controller;

import com.desk.dto.AITicketRequestDTO;
import com.desk.dto.AITicketResponseDTO;
import com.desk.dto.MeetingMinutesDTO;
import com.desk.service.AITicketService;
import com.desk.service.OllamaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Log4j2
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final OllamaService ollamaService;
    private final AITicketService aiTicketService;

    // 1. 단순 텍스트 요약 요청
    @PostMapping(value = "/summary")
    public ResponseEntity<MeetingMinutesDTO> getReportSummary(
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestPart(value = "data") MeetingMinutesDTO data // JSON 데이터 수신
    ) {
        log.info("AI Summary Request: {}", data);
        MeetingMinutesDTO result = ollamaService.getMeetingInfoFromAi(
                file,
                data.getTitle(),
                data.getShortSummary(),
                data.getOverview(),
                data.getDetails()
        );
        return ResponseEntity.ok(result);
    }
    // ✅ 3. 파란창 요약 데이터 그대로 PDF 생성
    @PostMapping("/summary-pdf")
    public ResponseEntity<?> downloadSummaryPdf(@RequestBody MeetingMinutesDTO summary) {

        byte[] pdfBytes = ollamaService.generatePdf(summary);

        log.info("PDF bytes length: {}", (pdfBytes == null ? -1 : pdfBytes.length));
        if (pdfBytes != null && pdfBytes.length >= 5) {
            String head = new String(pdfBytes, 0, 5);
            log.info("PDF head: {}", head);
        }
        if (pdfBytes == null || pdfBytes.length < 5 ||
                !new String(pdfBytes, 0, 5).equals("%PDF-")) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("message", "서버에서 유효한 PDF를 만들지 못했습니다."));
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);

        String filename = "Meeting_Summary.pdf";
        headers.setContentDispositionFormData("attachment", filename);
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }
    @PostMapping("/ticket/chat")
    @PreAuthorize("isAuthenticated()") // 로그인한 사용자만 가능
    public AITicketResponseDTO chat(@RequestBody AITicketRequestDTO request) {

        log.info("[AI Ticket] Chat Request | ConvID: {} | User: {}",
                request.getConversationId(),
                request.getSenderDept());

        // 핵심 로직 실행 (라우팅 -> 담당자 -> 인터뷰)
        return aiTicketService.processRequest(request);
    }
}