package com.desk.repository;

import com.desk.domain.TicketPersonal;
import com.desk.domain.TicketState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TicketPersonalRepository extends JpaRepository<TicketPersonal, Long> {

    Page<TicketPersonal> findByTpReceiver(String tpReceiver, Pageable pageable);



    @EntityGraph(attributePaths = "ticket") // 1 + n 문제 해결 위해 필요
    Page<TicketPersonal> findWithTicketByTpReceiver(String tpReceiver, Pageable pageable);

    @EntityGraph(attributePaths = "ticket")
    Optional<TicketPersonal> findWithTicketByTpno(Long tpno);

    // 필터링용 예시... receiver + state
    @EntityGraph(attributePaths = "ticket")
    Page<TicketPersonal> findWithTicketByTpReceiverAndTpState(String tpReceiver, TicketState tpState, Pageable pageable);

    // 필터링용 예시... receiver + read
    @EntityGraph(attributePaths = "ticket")
    Page<TicketPersonal> findWithTicketByTpReceiverAndTpRead(String tpReceiver, boolean tpRead, Pageable pageable);
}
