package com.desk.repository;

import com.desk.domain.TicketPersonal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TicketPersonalRepository extends JpaRepository<TicketPersonal, Long>, TicketPersonalSearch{

    // Tno와 받은 이를 가지고 pno를 찾는 쿼리
    @Query("select tp.pno from TicketPersonal tp where tp.receiver.email = :receiver and tp.ticket.tno = :tno")
    Optional<Long> findPnoByReceiverAndTno(@Param("receiver") String receiver, @Param("tno") Long tno);
    
    // 삭제 검증용 (테스트)
    long countByTicket_Tno(Long tno);

}
