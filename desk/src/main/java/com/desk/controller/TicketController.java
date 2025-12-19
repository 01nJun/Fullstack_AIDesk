package com.desk.controller;

import com.desk.dto.TicketDTO;
import com.desk.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    //등록 (새 티켓)
    @PostMapping("/")
    public ResponseEntity<TicketDTO> register(@RequestBody TicketDTO dto) {

        log.info("register ticket: {}", dto);

        TicketDTO saved = ticketService.registerNewTicket(dto);

        return ResponseEntity.ok(saved);
    }

    // 내가 보낸 티켓 목록 (writer 기준)
    @GetMapping("/send")
    public ResponseEntity<Page<TicketDTO>> listSendTicket(
            @RequestParam String writer,
            @PageableDefault(size = 10) Pageable pageable
    ) {

        log.info("listSendTicket writer={}, pageable={}", writer, pageable);

        return ResponseEntity.ok(ticketService.listSendTicket(writer, pageable));
    }

    // 내가 보낸 티켓 1개 조회 (writer 기준)
    @GetMapping("/send/{tno}")
    public ResponseEntity<TicketDTO> readSendTicket(
            @PathVariable Long tno,
            @RequestParam String writer
    ) {

        log.info("readSendTicket tno={}, writer={}", tno, writer);

        return ResponseEntity.ok(ticketService.readSendTicket(tno, writer));
    }

    // 내가 보낸 티켓 수정 (writer 권한)
    @PutMapping("/send/{tno}")
    public ResponseEntity<TicketDTO> modifySendTicket(
            @PathVariable Long tno,
            @RequestParam String writer,
            @RequestBody TicketDTO dto
    ) {

        log.info("modifySendTicket tno={}, writer={}, dto={}", tno, writer, dto);

        TicketDTO updated = ticketService.modifySendTicket(tno, writer, dto);

        return ResponseEntity.ok(updated);
    }

    // 전체 티켓 조회 (관리자용)
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/admin/list")
    public ResponseEntity<Page<TicketDTO>> listAllTicket(
            @PageableDefault(size = 10) Pageable pageable
    ) {

        log.info("listAllTicket pageable={}", pageable);

        return ResponseEntity.ok(ticketService.listAllTicket(pageable));
    }

    // 티켓 삭제
    @DeleteMapping("/{tno}")
    public ResponseEntity<Map<String, String>> removeTicket(@PathVariable Long tno) {

        log.info("removeTicket tno={}", tno);

        ticketService.removeTicket(tno);

        return ResponseEntity.ok(Map.of("RESULT", "SUCCESS"));
    }
}
