package com.desk.repository;

import com.desk.domain.Ticket;
import com.desk.dto.TicketFilterDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface TicketSearch {
    // QueryDSL 동적 쿼리 메서드
    Page<Ticket> findAllWithPersonalList(String writer, TicketFilterDTO filter, Pageable pageable);
    Optional<Ticket> findWithPersonalListById(Long tno);

    // 전체 티켓(보낸 것 + 받은 것) 조회를 위한 커스텀 메서드
    Page<Ticket> findAllAll(String email, TicketFilterDTO filter, Pageable pageable);
}