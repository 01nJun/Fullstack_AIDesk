package com.desk.service;

import com.desk.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TicketService {

    // Ticket + TicketPersonal N개 생성
    TicketSentListDTO create(TicketCreateDTO req, String writer);

    // 보낸 티켓 목록(페이징 + 필터)
    Page<TicketSentListDTO> listSent(String writer, TicketFilterDTO filter, Pageable pageable);

    // 보낸 티켓 단일 상세
    TicketSentListDTO readSent(Long tno, String writer);

    // 삭제
    void deleteSent(Long tno, String writer);
}
