package com.desk.repository;

import com.desk.domain.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface TicketRepository extends JpaRepository<Ticket, Long>, TicketSearch  {
    Optional<Ticket> findByTnoAndWriter_Email(Long tno, String email);
}
