package com.desk.service;

import com.desk.dto.TicketDTO;

import java.util.List;

public interface TicketService {

    TicketDTO register(TicketDTO dto);     // POST
    TicketDTO read(Long tno);              // GET one
    List<TicketDTO> list();                // GET list
    TicketDTO modify(Long tno, TicketDTO dto); // PUT
    void remove(Long tno);                 // DELETE
}
