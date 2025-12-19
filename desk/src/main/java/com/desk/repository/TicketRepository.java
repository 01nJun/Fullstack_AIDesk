package com.desk.repository;

import com.desk.domain.Ticket;
import com.desk.domain.TicketGrade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    Page<Ticket> findBytWriter(String tWriter, Pageable pageable);

    // 필터링용 임시: 보낸 사람 + 등급
    Page<Ticket> findBytWriterAndTGrade(String tWriter, TicketGrade tGrade, Pageable pageable);
}
