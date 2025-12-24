package com.desk.repository;

import com.desk.domain.TicketPersonal;
import com.desk.dto.TicketFilterDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface TicketPersonalSearch {
    // QueryDSL 동적 쿼리 메서드
    // 페이지 조회
    Page<TicketPersonal> findAllWithTicket(String receiver, TicketFilterDTO filter, Pageable pageable);
    // 단건 조회
    Optional<TicketPersonal> findWithTicketByPno(Long pno);
}
