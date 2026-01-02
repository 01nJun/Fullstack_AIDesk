package com.desk.controller;

import com.desk.dto.AITicketRequestDTO;
import com.desk.dto.AITicketResponseDTO;
import com.desk.service.AITicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/ticket") // [중요] 프론트엔드 연결 주소
@RequiredArgsConstructor
@Log4j2
public class AITicketController {

    private final AITicketService aiTicketService;

    @PostMapping("/chat")
    @PreAuthorize("isAuthenticated()") // 로그인한 사용자만 가능
    public AITicketResponseDTO chat(@RequestBody AITicketRequestDTO request) {
        
        log.info("[AI Ticket] Chat Request | ConvID: {} | User: {}", 
                request.getConversationId(), 
                request.getSenderDept());
        
        // 핵심 로직 실행 (라우팅 -> 담당자 -> 인터뷰)
        return aiTicketService.processRequest(request);
    }
}