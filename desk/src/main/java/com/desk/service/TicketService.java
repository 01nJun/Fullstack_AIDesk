package com.desk.service;

import com.desk.domain.TicketState;
import com.desk.dto.PersonalTicketDTO;
import com.desk.dto.TicketDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TicketService {

    // 내가 보낸 티켓 목록
    Page<TicketDTO> listSendTicket(String tWriter, Pageable pageable);

    // 내가 보낸 티켓 하나 조회
    TicketDTO readSendTicket(Long tno, String tWriter);

    // 내가 보낸 티켓 수정
    TicketDTO modifySendTicket(Long tno, String tWriter, TicketDTO dto);

    // 전체 티켓 조회(관리자용)
    Page<TicketDTO> listAllTicket(Pageable pageable);

    // 등록/삭제
    TicketDTO registerNewTicket(TicketDTO dto);
    void removeTicket(Long tno);
}
