package com.desk.repository.chat;

import com.desk.domain.ChatFile;
import com.desk.domain.ChatFileStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatFileRepository extends JpaRepository<ChatFile, String> {
    
    /**
     * 메시지에 첨부된 파일 목록 조회
     */
    List<ChatFile> findByChatMessageId(Long chatMessageId);
    
    /**
     * 파일 UUID 목록으로 TEMP 상태의 파일 조회 (바인딩 전 파일 검증용)
     */
    @Query("SELECT f FROM ChatFile f " +
           "WHERE f.uuid IN :fileUuids " +
           "AND f.status = :status " +
           "AND f.chatRoomId = :chatRoomId " +
           "AND f.uploaderId = :uploaderId")
    List<ChatFile> findByUuidInAndStatusAndChatRoomIdAndUploaderId(
            @Param("fileUuids") List<String> fileUuids, 
            @Param("status") ChatFileStatus status, 
            @Param("chatRoomId") Long chatRoomId, 
            @Param("uploaderId") String uploaderId
    );
    
    /**
     * 채팅방의 TEMP 상태 파일 조회 (정리용)
     */
    List<ChatFile> findByStatusAndChatRoomId(ChatFileStatus status, Long chatRoomId);
    
    /**
     * 특정 시간 이전의 TEMP 상태 파일 조회 (정리용)
     */
    @Query("SELECT f FROM ChatFile f " +
           "WHERE f.status = :status " +
           "AND f.createdAt < :beforeTime")
    List<ChatFile> findTempFilesOlderThan(
            @Param("status") ChatFileStatus status,
            @Param("beforeTime") java.time.LocalDateTime beforeTime
    );
    
    /**
     * 특정 시간 이전의 DELETED 상태 파일 조회 (물리 파일 정리용)
     */
    @Query("SELECT f FROM ChatFile f " +
           "WHERE f.status = :status " +
           "AND f.deletedAt < :beforeTime")
    List<ChatFile> findDeletedFilesOlderThan(
            @Param("status") ChatFileStatus status,
            @Param("beforeTime") java.time.LocalDateTime beforeTime
    );
}

