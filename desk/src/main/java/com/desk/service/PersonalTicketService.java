package com.desk.service;

import com.desk.domain.TicketState;
import com.desk.dto.PageRequestDTO;
import com.desk.dto.PageResponseDTO;
import com.desk.dto.TicketFilterDTO;
import com.desk.dto.TicketReceivedListDTO;

public interface PersonalTicketService {

    // 받은 티켓 리스트 (페이징 + 필터)
    PageResponseDTO<TicketReceivedListDTO> listRecieveTicket(String receiver, TicketFilterDTO filter, PageRequestDTO pageRequestDTO);

    // 받은 티켓 단일 (pno 기준)
    TicketReceivedListDTO readRecieveTicket(Long pno, String receiver, boolean markAsRead);

    // 받은 티켓 단일 (tno 기준)
    TicketReceivedListDTO readRecieveTicketByTno(Long tno, String receiver, boolean markAsRead);

    // 진행상태 변경
    TicketReceivedListDTO changeState(Long pno, String receiver, TicketState state);
}
