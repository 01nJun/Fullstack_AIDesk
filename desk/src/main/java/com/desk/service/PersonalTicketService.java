package com.desk.service;

import com.desk.domain.TicketState;
import com.desk.dto.PersonalTicketDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PersonalTicketService {

    Page<PersonalTicketDTO> listPersonalTicket(String receiver, Pageable pageable);     // 내가 받은 티켓 리스트, 페이징
    PersonalTicketDTO readPersonalTicket(Long tpno, String receiver);            // 내가 받은 티켓 하나 Read
    PersonalTicketDTO changePersonalTicketState(Long tpno, String receiver, TicketState state); // 내 진행 상태 변경
}
