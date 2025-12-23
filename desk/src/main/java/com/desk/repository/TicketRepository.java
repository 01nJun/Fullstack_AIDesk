package com.desk.repository;

import com.desk.domain.Ticket;
import com.desk.dto.TicketFilterDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    
    // QueryDSL 동적 쿼리 메서드
    Page<Ticket> findAllWithPersonalList(String writer, TicketFilterDTO filter, Pageable pageable);
    Optional<Ticket> findWithPersonalListById(Long tno);
}
