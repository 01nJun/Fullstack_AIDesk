package com.desk.controller;

import com.desk.dto.*;
import com.desk.service.PersonalTicketService;
import com.desk.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/api/tickets")
public class TicketController {
    // 생성자주입
    private final TicketService ticketService;
    private final PersonalTicketService personalTicketService;

    // ---> /api/tickets 경로로 Post 요청하면 이리로...
    // 티켓 생성 ---> writer + 수신인 리스트로 Ticket 1건과 TicketPersonal N건 생성
    // consumes를 multipart/form-data로 명시, MULTIPART_FORM_DATA_VALUE = 텍스트 + 파일을 함께 보내기 위한 형식
    // consumes - 요청(Request) 타입, produces - 응답(Response) 타입
    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<TicketSentListDTO> create(
            // 쿼리스트링으로 writer 받아옴
            @RequestParam String writer,
            // 바디(JSON)을 DTO로 변환
            @RequestPart("ticket") TicketCreateDTO req,
            // 파일 리스트는 @RequestPart("files")로 받음
            @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) {
        // 수신자 수 체크 ---> 수신자 null 이면 에러나므로 삼항연산자로 null 방어
        int receiverCount = (req.getReceivers() == null) ? 0 : req.getReceivers().size();
        log.info("[Ticket] 생성 요청 | 작성자={} | 수신자수={}", writer, receiverCount);

        // 생성 (수신자마다 생성됨)
        TicketSentListDTO created = ticketService.createWithFiles(req, writer, files);
        log.info("[Ticket] 생성 완료 | 작성자={} | 티켓번호={}", writer, created.getTno());

        // HTTP 200 OK (이거도 나중에 수정해야 할 수도 있을 것 같아요 굳이 여기서 ok하지말고 exception으로 빼도될듯)
        // created 는 DTO입니다
        return ResponseEntity.ok(created);
    }

    // 보낸 티켓 단일 조회 --- tno + writer로 권한 확인 후 반환
    @GetMapping("/sent/{tno}")
    public ResponseEntity<TicketSentListDTO> readSent(
            @PathVariable Long tno,
            @RequestParam String writer
    ) {
        TicketSentListDTO dto = ticketService.readSent(tno, writer);
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
    // 보낸함 페이지 조회 --- writer 기준 + filter + 페이징/정렬
    @GetMapping("/sent")
    public ResponseEntity<PageResponseDTO<TicketSentListDTO>> listSent(
            @RequestParam String writer,
            @ModelAttribute TicketFilterDTO filter,
            @ModelAttribute PageRequestDTO pageRequestDTO
    ) {
        log.info("[Ticket] 보낸티켓 목록 조회 | 수신자={} | PageRequest={}", writer, pageRequestDTO);
        PageResponseDTO<TicketSentListDTO> response = ticketService.listSent(writer, filter, pageRequestDTO);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    public ResponseEntity<PageResponseDTO<TicketSentListDTO>> listAll(
            @RequestParam String email,
            @ModelAttribute TicketFilterDTO filter,
            @ModelAttribute PageRequestDTO pageRequestDTO
    ) {
        log.info("[Ticket] 전체 티켓 목록 조회 | 수신자={} | PageRequest={}", email, pageRequestDTO);
        PageResponseDTO<TicketSentListDTO> response = ticketService.listAll(email, filter, pageRequestDTO);
        return ResponseEntity.ok(response);
    }
}
