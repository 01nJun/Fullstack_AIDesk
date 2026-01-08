package com.desk.repository.chat;

import com.desk.domain.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * ChatMessage QueryDSL 커스텀 인터페이스
 */
public interface ChatMessageSearch {
    /**
     * 채팅방의 메시지 목록 조회 (chatFiles 포함, N+1 방지)
     * QueryDSL로 최적화된 쿼리 사용
     */
    Page<ChatMessage> findByChatRoomIdOrderByMessageSeqDescWithFiles(Long chatRoomId, Pageable pageable);
}

