package com.desk.repository.chat;

import com.desk.domain.ChatFile;
import com.desk.domain.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface ChatFileRepository extends JpaRepository<ChatFile, String> {

    /**
     * [파일함 탭 전용]
     * - 접근제어: 내가 해당 방의 ACTIVE 참여자이며, joinedAt <= 파일 createdAt
     * - 검색: fileName/writer/receiver에 키워드 포함
     */
    @Query("""
            SELECT f
            FROM ChatFile f
            JOIN f.chatRoom cr
            JOIN cr.participants p
            WHERE p.userId = :email
              AND (p.joinedAt IS NULL OR f.createdAt >= p.joinedAt)
              AND (p.leftAt IS NULL OR f.createdAt <= p.leftAt)
              AND (
                :kw = '' OR
                LOWER(f.fileName) LIKE LOWER(CONCAT('%', :kw, '%')) OR
                LOWER(f.writer) LIKE LOWER(CONCAT('%', :kw, '%')) OR
                LOWER(COALESCE(f.receiver, '')) LIKE LOWER(CONCAT('%', :kw, '%'))
              )
            """)
    Page<ChatFile> findAccessibleAllByEmailAndSearch(@Param("email") String email,
                                                     @Param("kw") String kw,
                                                     Pageable pageable);

    @Query("""
            SELECT f
            FROM ChatFile f
            JOIN f.chatRoom cr
            JOIN cr.participants p
            WHERE p.userId = :email
              AND (p.joinedAt IS NULL OR f.createdAt >= p.joinedAt)
              AND (p.leftAt IS NULL OR f.createdAt <= p.leftAt)
              AND f.writer = :email
              AND (
                :kw = '' OR
                LOWER(f.fileName) LIKE LOWER(CONCAT('%', :kw, '%')) OR
                LOWER(f.writer) LIKE LOWER(CONCAT('%', :kw, '%')) OR
                LOWER(COALESCE(f.receiver, '')) LIKE LOWER(CONCAT('%', :kw, '%'))
              )
            """)
    Page<ChatFile> findAccessibleSentByEmailAndSearch(@Param("email") String email,
                                                      @Param("kw") String kw,
                                                      Pageable pageable);

    @Query("""
            SELECT f
            FROM ChatFile f
            JOIN f.chatRoom cr
            JOIN cr.participants p
            WHERE p.userId = :email
              AND (p.joinedAt IS NULL OR f.createdAt >= p.joinedAt)
              AND (p.leftAt IS NULL OR f.createdAt <= p.leftAt)
              AND f.writer <> :email
              AND (
                :kw = '' OR
                LOWER(f.fileName) LIKE LOWER(CONCAT('%', :kw, '%')) OR
                LOWER(f.writer) LIKE LOWER(CONCAT('%', :kw, '%')) OR
                LOWER(COALESCE(f.receiver, '')) LIKE LOWER(CONCAT('%', :kw, '%'))
              )
            """)
    Page<ChatFile> findAccessibleReceivedByEmailAndSearch(@Param("email") String email,
                                                          @Param("kw") String kw,
                                                          Pageable pageable);

    /**
     * [AI 파일조회 전용]
     * - 접근제어: 내가 ACTIVE 참여자이며, joinedAt <= 파일 createdAt
     * - 기간/상대/부서/키워드(+채팅방 이름)로 필터링
     *
     * NOTE:
     * - 상대(counterEmail)는 DIRECT 방에서는 상대 참여자 필터로, GROUP 방에서는 "해당 사용자가 참여한 방" 필터로 처리
     * - 부서(dept)는 업로더(writer) 또는 상대(지정된 경우) 기준으로 필터링
     */
    @Query("""
            SELECT DISTINCT f
            FROM ChatFile f
            JOIN f.chatRoom cr
            JOIN cr.participants myp
            JOIN com.desk.domain.Member w ON w.email = f.writer
            LEFT JOIN cr.participants cp
            LEFT JOIN com.desk.domain.Member cm ON cm.email = cp.userId
            WHERE myp.userId = :myEmail
              AND (myp.joinedAt IS NULL OR f.createdAt >= myp.joinedAt)
              AND (myp.leftAt IS NULL OR f.createdAt <= myp.leftAt)
              AND (:fromDt IS NULL OR f.createdAt >= :fromDt)
              AND (:toDt IS NULL OR f.createdAt <= :toDt)
              AND (
                :counterEmail IS NULL OR
                EXISTS (
                  SELECT 1
                  FROM com.desk.domain.ChatParticipant p2
                  WHERE p2.chatRoom = cr AND p2.userId = :counterEmail
                )
              )
              AND (
                :dept IS NULL OR
                w.department = :dept OR
                (cm IS NOT NULL AND cm.department = :dept)
              )
              AND (
                :kw = '' OR
                LOWER(f.fileName) LIKE LOWER(CONCAT('%', :kw, '%')) OR
                LOWER(cr.name) LIKE LOWER(CONCAT('%', :kw, '%')) OR
                LOWER(w.email) LIKE LOWER(CONCAT('%', :kw, '%')) OR
                LOWER(w.nickname) LIKE LOWER(CONCAT('%', :kw, '%')) OR
                LOWER(COALESCE(f.receiver, '')) LIKE LOWER(CONCAT('%', :kw, '%'))
              )
            """)
    Page<ChatFile> searchAccessibleChatFilesForAI(@Param("myEmail") String myEmail,
                                                  @Param("kw") String kw,
                                                  @Param("fromDt") LocalDateTime fromDt,
                                                  @Param("toDt") LocalDateTime toDt,
                                                  @Param("counterEmail") String counterEmail,
                                                  @Param("dept") Department dept,
                                                  Pageable pageable);

    /**
     * [AI/다운로드 권한 체크]
     */
    @Query("""
            SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END
            FROM ChatFile f
            JOIN f.chatRoom cr
            JOIN cr.participants p
            WHERE f.uuid = :uuid
              AND p.userId = :myEmail
              AND (p.joinedAt IS NULL OR f.createdAt >= p.joinedAt)
              AND (p.leftAt IS NULL OR f.createdAt <= p.leftAt)
            """)
    boolean existsAccessibleChatFileByUuid(@Param("uuid") String uuid, @Param("myEmail") String myEmail);

    /**
     * 메시지별 첨부 조회 (채팅 메시지 목록/WS payload에 첨부 포함용)
     */
    @Query("""
            SELECT f
            FROM ChatFile f
            WHERE f.chatRoom.id = :roomId
              AND f.messageSeq IN :messageSeqs
            """)
    java.util.List<ChatFile> findByRoomIdAndMessageSeqIn(@Param("roomId") Long roomId,
                                                         @Param("messageSeqs") java.util.List<Long> messageSeqs);
}


