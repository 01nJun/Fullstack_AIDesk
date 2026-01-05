package com.desk.repository.chat;

import com.desk.domain.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    /**
     * 채팅방의 메시지 목록 조회 (messageSeq 기준 내림차순)
     * 무한 스크롤을 위한 페이징 지원
     */
    Page<ChatMessage> findByChatRoomIdOrderByMessageSeqDesc(Long chatRoomId, Pageable pageable);
    
    /**
     * 채팅방의 최신 메시지 조회
     */
    Optional<ChatMessage> findFirstByChatRoomIdOrderByMessageSeqDesc(Long chatRoomId);
    
    /**
     * 채팅방의 최대 messageSeq 조회
     * 새로운 메시지의 messageSeq 생성용
     */
    @Query("SELECT COALESCE(MAX(m.messageSeq), 0) FROM ChatMessage m WHERE m.chatRoom.id = :roomId")
    Long findMaxMessageSeqByChatRoomId(@Param("roomId") Long roomId);
    
    /**
     * 특정 messageSeq 이후의 메시지 개수 조회
     * 안 읽은 메시지 계산용
     */
    @Query("SELECT COUNT(m) FROM ChatMessage m " +
           "WHERE m.chatRoom.id = :roomId AND m.messageSeq > :lastReadSeq")
    Long countUnreadMessages(@Param("roomId") Long roomId, @Param("lastReadSeq") Long lastReadSeq);
}


