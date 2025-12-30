package com.desk.controller;

import com.desk.domain.UploadTicketFile;
import com.desk.dto.*;
import com.desk.service.PersonalTicketService;
import com.desk.service.TicketService;
import com.desk.util.CustomFileUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final CustomFileUtil fileUtil; // 건영
    private final ObjectMapper objectMapper; // 건영

    // ---> /api/tickets 경로로 Post 요청하면 이리로...
    // 티켓 생성 ---> writer + 수신인 리스트로 Ticket 1건과 TicketPersonal N건 생성
    @PostMapping(value = "/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TicketSentListDTO> create(
            @RequestPart("writer_email") String writer,
            @RequestPart("req") String reqJson,
            @RequestPart(value = "files", required = false) List<MultipartFile> files
    )throws Exception {

        TicketCreateDTO req = objectMapper.readValue(reqJson, TicketCreateDTO.class);

        // 수신자 수 체크 ---> 수신자 null 이면 에러나므로 삼항연산자로 null 방어
        int receiverCount = (req.getReceivers() == null) ? 0 : req.getReceivers().size();
        log.info("[Ticket] 생성 요청 | 작성자={} | 수신자수={}", writer, receiverCount);

        // 생성 (수신자마다 생성됨)
        TicketSentListDTO created = ticketService.create(req, writer, files);
        log.info("[Ticket] 생성 완료 | 작성자={} | 티켓번호={}", writer, created.getTno());

        // HTTP 200 OK (이거도 나중에 수정해야 할 수도 있을 것 같아요 굳이 여기서 ok하지말고 exception으로 빼도될듯)
        // created 는 DTO입니다
        return ResponseEntity.ok(created);
    }

    @GetMapping("/{tno}/files")
    public ResponseEntity<List<FileItemDTO>> getTicketFiles(@PathVariable Long tno) {

        List<FileItemDTO> files = ticketService.getTicketFiles(tno);
        return ResponseEntity.ok(files);
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

    // [NEW] 티켓 내 개별 파일 삭제
    // DELETE /api/tickets/{tno}/files/{uuid}?writer=이메일
    @DeleteMapping("/files/{uuid}")
    public ResponseEntity<Void> removeFile(
            @PathVariable String uuid,
            @RequestParam String writer
    ) {
        log.info("[File Delete] 요청 | uuid={} | 요청자={}", uuid, writer);

        ticketService.removeFile(uuid, writer);

        return ResponseEntity.noContent().build();
    }
    // [NEW] 내 파일 보관함(전체 파일 목록) 조회
    @GetMapping("/files/all")
    public ResponseEntity<PageResponseDTO<FileItemDTO>> listUserFiles(
            @RequestParam("email") String email,
            @ModelAttribute PageRequestDTO pageRequestDTO
    ) {
        log.info("[FileBox] 파일 목록 조회 요청 | 사용자={}", email);
        PageResponseDTO<FileItemDTO> response = ticketService.listUserFiles(email, pageRequestDTO);
        return ResponseEntity.ok(response);
    }

}
