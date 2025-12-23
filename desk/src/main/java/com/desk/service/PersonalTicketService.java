package com.desk.service;

import com.desk.domain.TicketState;
import com.desk.dto.TicketFilterDTO;
import com.desk.dto.TicketReceivedListDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PersonalTicketService {

    // 받은 티켓 리스트 (페이징 + 필터)
    Page<TicketReceivedListDTO> listRecieveTicket(String receiver, TicketFilterDTO filter, Pageable pageable);

    // 받은 티켓 단일 (pno 기준)
    TicketReceivedListDTO readRecieveTicket(Long pno, String receiver, boolean markAsRead);

    // 받은 티켓 단일 (tno 기준)
    TicketReceivedListDTO readRecieveTicketByTno(Long tno, String receiver, boolean markAsRead);

    // 진행상태 변경
    TicketReceivedListDTO changeState(Long pno, String receiver, TicketState state);
}
