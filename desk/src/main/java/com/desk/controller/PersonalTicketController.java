package com.desk.controller;

import com.desk.domain.TicketState;
import com.desk.dto.PageRequestDTO;
import com.desk.dto.PageResponseDTO;
import com.desk.dto.TicketFilterDTO;
import com.desk.dto.TicketReceivedListDTO;
import com.desk.service.PersonalTicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/api/tickets/received")
public class PersonalTicketController {

    private final PersonalTicketService personalTicketService;

    // 받은함 페이지 조회 --- receiver 기준 + 필터 + 페이징
    @GetMapping
    public ResponseEntity<PageResponseDTO<TicketReceivedListDTO>> listInbox(
            @RequestParam String receiver,
            @ModelAttribute TicketFilterDTO filter,
            @ModelAttribute PageRequestDTO pageRequestDTO
    ) {
        log.info("[Inbox] 목록 요청 | 수신자={} | PageRequest={}", receiver, pageRequestDTO);

        PageResponseDTO<TicketReceivedListDTO> response =
                personalTicketService.listRecieveTicket(receiver, filter, pageRequestDTO);

        return ResponseEntity.ok(response);
    }

    // 받은 티켓 단일 조회 (pno 기준) --- receiver 소유 검증 + markAsRead 옵션
    @GetMapping("/by-pno/{pno}")
    public ResponseEntity<TicketReceivedListDTO> readInboxByPno(
            @PathVariable Long pno,
            @RequestParam String receiver,
            @RequestParam(defaultValue = "true") boolean markAsRead
    ) {
        log.info("[Inbox] 단건 조회(pno) 요청 | pno={} | 수신자={} | 읽음처리={}",
                pno, receiver, markAsRead);

        TicketReceivedListDTO dto =
                personalTicketService.readRecieveTicket(pno, receiver, markAsRead);

        log.info("[Inbox] 단건 조회(pno) 완료 | pno={} | 수신자={}", pno, receiver);
        return ResponseEntity.ok(dto);
    }

    // 받은 티켓 단일 조회 (tno 기준) --- receiver+tno로 대상 TicketPersonal 찾은 뒤 markAsRead 적용
    @GetMapping("/by-tno/{tno}")
    public ResponseEntity<TicketReceivedListDTO> readInboxByTno(
            @PathVariable Long tno,
            @RequestParam String receiver,
            @RequestParam(defaultValue = "true") boolean markAsRead
    ) {
        log.info("[Inbox] 단건 조회(tno) 요청 | tno={} | 수신자={} | 읽음처리={}",
                tno, receiver, markAsRead);

        TicketReceivedListDTO dto =
                personalTicketService.readRecieveTicketByTno(tno, receiver, markAsRead);

        log.info("[Inbox] 단건 조회(tno) 완료 | tno={} | 수신자={}", tno, receiver);
        return ResponseEntity.ok(dto);
    }

    // 진행상태 변경 --- receiver가 소유한 pno의 state 변경
    @PatchMapping("/{pno}/state")
    public ResponseEntity<TicketReceivedListDTO> changeState(
            @PathVariable Long pno,
            @RequestParam String receiver,
            @RequestParam TicketState state
    ) {
        log.info("[Inbox] 상태 변경 요청 | pno={} | 수신자={} | state={}",
                pno, receiver, state);

        TicketReceivedListDTO dto =
                personalTicketService.changeState(pno, receiver, state);

        log.info("[Inbox] 상태 변경 완료 | pno={} | 수신자={} | state={}",
                pno, receiver, state);

        return ResponseEntity.ok(dto);
    }
}
