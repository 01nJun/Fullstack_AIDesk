package com.desk.repository;

import com.desk.domain.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;


public interface TicketRepository extends JpaRepository<Ticket, Long>, TicketSearch  {

}
