package com.desk.controller;

import com.desk.dto.*;
import com.desk.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.web.PageableDefault;

@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/api/tickets")
public class TicketController {
    // 생성자주입
    private final TicketService ticketService;

    // ---> /api/tickets 경로로 Post 요청하면 이리로...
    // 티켓 생성 ---> writer + 수신인 리스트로 Ticket 1건과 TicketPersonal N건 생성
    @PostMapping
    public ResponseEntity<TicketSentListDTO> create(
            // 쿼리스트링으로 writer 받아옴
            @RequestParam String writer,
            // 바디(JSON)을 DTO로 변환
            @RequestBody TicketCreateDTO req
    ) {
        // 수신자 수 체크 ---> 수신자 null 이면 에러나므로 삼항연산자로 null 방어
        int receiverCount = (req.getReceivers() == null) ? 0 : req.getReceivers().size();
        log.info("[Ticket] 생성 요청 | 작성자={} | 수신자수={}", writer, receiverCount);

        // 생성 (수신자마다 생성됨)
        TicketSentListDTO created = ticketService.create(req, writer);
        log.info("[Ticket] 생성 완료 | 작성자={} | 티켓번호={}", writer, created.getTno());

        // HTTP 200 OK (이거도 나중에 수정해야 할 수도 있을 것 같아요 굳이 여기서 ok하지말고 exception으로 빼도될듯)
        // created 는 DTO입니다
        return ResponseEntity.ok(created);
    }

    // 보낸함 페이지 조회 --- writer 기준 + filter + 페이징/정렬
    @GetMapping("/sent")
    public ResponseEntity<Page<TicketSentListDTO>> listSent(
            @RequestParam String writer,
            // 쿼리스트링 파라미터들을 DTO 필드에 묶어서 넣음
            @ModelAttribute TicketFilterDTO filter,
            // default Pageable 설정 정하는거라는데 이거 수정해야할듯싶네요
            @PageableDefault(size = 10, sort = "tno", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        String sort = pageable.getSort().isSorted() ? pageable.getSort().toString() : "없음";

        log.info("[Ticket] 보낸함 목록 요청 | 작성자={} | page={} | size={} | sort={} | filter={}",
                writer,
                pageable.getPageNumber(),
                pageable.getPageSize(),
                sort,
                filter
        );

        Page<TicketSentListDTO> page = ticketService.listSent(writer, filter, pageable);    // 리스트 조회니까 Page로

        log.info("[Ticket] 보낸함 목록 응답 | 작성자={} | page={} | size={} | 반환건수={} | 전체건수={}",
                writer,
                page.getNumber(),
                page.getSize(),
                page.getNumberOfElements(),
                page.getTotalElements()
        );

        return ResponseEntity.ok(page);
    }

    // 보낸 티켓 단일 조회 --- tno + writer로 권한 확인 후 반환
    @GetMapping("/sent/{tno}")
    public ResponseEntity<TicketSentListDTO> readSent(
            @PathVariable Long tno,
            @RequestParam String writer
    ) {
        log.info("[Ticket] 보낸티켓 단건 조회 요청 | 작성자={} | 티켓번호={}", writer, tno);

        TicketSentListDTO dto = ticketService.readSent(tno, writer);

        log.info("[Ticket] 보낸티켓 단건 조회 완료 | 작성자={} | 티켓번호={}", writer, tno);
        return ResponseEntity.ok(dto);
    }

    // 티켓 삭제 --- writer 요청 시 Ticket 삭제 (연관 TicketPersonal도 함께 삭제)
    @DeleteMapping("/{tno}")
    public ResponseEntity<Void> deleteSent(
            @PathVariable Long tno,
            @RequestParam String writer
    ) {
        log.info("[Ticket] 보낸티켓 삭제 요청 | 작성자={} | 티켓번호={}", writer, tno);

        ticketService.deleteSent(tno, writer);

        log.info("[Ticket] 보낸티켓 삭제 완료 | 작성자={} | 티켓번호={}", writer, tno);
        return ResponseEntity.noContent().build();
    }
}
