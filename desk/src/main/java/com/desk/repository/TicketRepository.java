package com.desk.repository;

import com.desk.domain.Ticket;
import com.desk.domain.UploadTicketFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;


public interface TicketRepository extends JpaRepository<Ticket, Long>, TicketSearch  {
    Optional<Ticket> findByTnoAndWriter_Email(Long tno, String email);

    @EntityGraph(attributePaths = "documentList")
    @Query("select p from Ticket p where p.tno = :tno")
    Optional<Ticket> selectOne(@Param("tno") Long tno);

    // [핵심] 내가 작성자(writer)이거나, 수신자(receiver)로 포함된 티켓의 '모든 파일'을 조회
    // DISTINCT를 사용하여 중복 제거
    @Query("SELECT DISTINCT d " +
            "FROM Ticket t JOIN t.documentList d " +
            "WHERE t.writer.email = :email " +
            "   OR EXISTS (SELECT p FROM t.personalList p WHERE p.receiver.email = :email) " +
            "ORDER BY t.tno DESC") // 최신 티켓의 파일부터 정렬
    Page<UploadTicketFile> findAllFilesByUser(@Param("email") String email, Pageable pageable);

    // 파일 삭제를 위한 역추적 (UUID로 티켓 찾기)
    @Query("SELECT t FROM Ticket t JOIN t.documentList d WHERE d.uuid = :uuid")
    Optional<Ticket> findByFileUuid(@Param("uuid") String uuid);
}
