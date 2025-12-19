package com.desk.controller;

import com.desk.domain.TicketState;
import com.desk.dto.PersonalTicketDTO;
import com.desk.service.PersonalTicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/api/mytickets")
public class PersonalTicketController {

    private final PersonalTicketService personalTicketService;

    // 내가 받은 티켓 리스트 (receiver 기준)
    @GetMapping
    public ResponseEntity<Page<PersonalTicketDTO>> listPersonalTicket(
            @RequestParam String receiver,
            @PageableDefault(size = 10) Pageable pageable
    ) {

        log.info("listPersonalTicket receiver={}, pageable={}", receiver, pageable);

        return ResponseEntity.ok(personalTicketService.listPersonalTicket(receiver, pageable));
    }

    // 내가 받은 티켓 1개 조회 (receiver 권한)
    @GetMapping("/{tpno}")
    public ResponseEntity<PersonalTicketDTO> readPersonalTicket(
            @PathVariable Long tpno,
            @RequestParam String receiver
    ) {

        log.info("readPersonalTicket tpno={}, receiver={}", tpno, receiver);

        return ResponseEntity.ok(personalTicketService.readPersonalTicket(tpno, receiver));
    }

    // 내 진행 상태 변경 (receiver 권한)
    @PutMapping("/{tpno}/state")
    public ResponseEntity<PersonalTicketDTO> changeState(
            @PathVariable Long tpno,
            @RequestParam String receiver,
            @RequestParam TicketState state
    ) {

        log.info("changeState tpno={}, receiver={}, state={}", tpno, receiver, state);

        return ResponseEntity.ok(
                personalTicketService.changePersonalTicketState(tpno, receiver, state)
        );
    }
}
