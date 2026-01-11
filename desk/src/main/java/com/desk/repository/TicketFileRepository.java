package com.desk.repository;

import com.desk.domain.Department;
import com.desk.domain.TicketFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface TicketFileRepository extends JpaRepository<TicketFile, String> {

    // 전체 파일 조회 (작성자 혹은 수신자가 나이면서, 파일명/작성자/수신자에 키워드 포함)
    @Query("SELECT f FROM TicketFile f WHERE (f.writer = :email OR f.receiver = :email) " +
            "AND (f.fileName LIKE %:kw% OR f.writer LIKE %:kw% OR f.receiver LIKE %:kw%)")
    Page<TicketFile> findAllByEmailAndSearch(@Param("email") String email, @Param("kw") String kw, Pageable pageable);

    // 내가 보낸 파일
    @Query("SELECT f FROM TicketFile f WHERE f.writer = :email " +
            "AND (f.fileName LIKE %:kw% OR f.writer LIKE %:kw% OR f.receiver LIKE %:kw%)")
    Page<TicketFile> findByWriterAndSearch(@Param("email") String email, @Param("kw") String kw, Pageable pageable);

    // 내가 받은 파일
    @Query("SELECT f FROM TicketFile f WHERE f.receiver = :email " +
            "AND (f.fileName LIKE %:kw% OR f.writer LIKE %:kw% OR f.receiver LIKE %:kw%)")
    Page<TicketFile> findByReceiverAndSearch(@Param("email") String email, @Param("kw") String kw, Pageable pageable);

    /**
     * [AI 파일조회 전용]
     * - 접근제어: 내가 티켓 작성자이거나(Writer) 티켓 수신자(Receiver)인 경우만
     * - 기간/상대/부서/키워드로 필터링
     */
    @Query("""
            SELECT DISTINCT f
            FROM TicketFile f
            JOIN f.ticket t
            JOIN t.writer w
            LEFT JOIN t.personalList tp
            LEFT JOIN tp.receiver r
            WHERE (w.email = :myEmail OR r.email = :myEmail)
              AND (:fromDt IS NULL OR COALESCE(f.createdAt, t.birth) >= :fromDt)
              AND (:toDt IS NULL OR COALESCE(f.createdAt, t.birth) <= :toDt)
              AND (:counterEmail IS NULL OR w.email = :counterEmail OR r.email = :counterEmail)
              AND (:dept IS NULL OR w.department = :dept OR r.department = :dept)
              AND (
                :kw = '' OR
                LOWER(f.fileName) LIKE LOWER(CONCAT('%', :kw, '%')) OR
                LOWER(t.title) LIKE LOWER(CONCAT('%', :kw, '%')) OR
                LOWER(t.content) LIKE LOWER(CONCAT('%', :kw, '%')) OR
                LOWER(COALESCE(t.purpose, '')) LIKE LOWER(CONCAT('%', :kw, '%')) OR
                LOWER(COALESCE(t.requirement, '')) LIKE LOWER(CONCAT('%', :kw, '%')) OR
                LOWER(w.email) LIKE LOWER(CONCAT('%', :kw, '%')) OR
                LOWER(w.nickname) LIKE LOWER(CONCAT('%', :kw, '%')) OR
                LOWER(r.email) LIKE LOWER(CONCAT('%', :kw, '%')) OR
                LOWER(r.nickname) LIKE LOWER(CONCAT('%', :kw, '%'))
              )
            """)
    Page<TicketFile> searchAccessibleFilesForAI(@Param("myEmail") String myEmail,
                                               @Param("kw") String kw,
                                               @Param("fromDt") LocalDateTime fromDt,
                                               @Param("toDt") LocalDateTime toDt,
                                               @Param("counterEmail") String counterEmail,
                                               @Param("dept") Department dept,
                                               Pageable pageable);

    /**
     * [AI 파일 미리보기/다운로드 권한 체크]
     */
    @Query("""
            SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END
            FROM TicketFile f
            JOIN f.ticket t
            JOIN t.writer w
            LEFT JOIN t.personalList tp
            LEFT JOIN tp.receiver r
            WHERE f.uuid = :uuid
              AND (w.email = :myEmail OR r.email = :myEmail)
            """)
    boolean existsAccessibleFileByUuid(@Param("uuid") String uuid, @Param("myEmail") String myEmail);
}