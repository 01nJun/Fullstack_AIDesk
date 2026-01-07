package com.desk.repository.chat;

import com.desk.domain.ChatRoom;
import com.desk.domain.ChatRoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    
    /**
     * DIRECT 타입 채팅방에서 pairKey로 조회
     * 1:1 채팅방 중복 생성 방지용
     */
    Optional<ChatRoom> findByRoomTypeAndPairKey(ChatRoomType roomType, String pairKey);
    
    /**
     * 사용자가 참여 중인 채팅방 목록 조회 (ACTIVE 상태만)
     */
    @Query("SELECT DISTINCT cr FROM ChatRoom cr " +
           "JOIN cr.participants cp " +
           "WHERE cp.userId = :userId AND cp.status = 'ACTIVE' " +
           "ORDER BY cr.lastMsgAt DESC NULLS LAST, cr.createdAt DESC")
    java.util.List<ChatRoom> findActiveRoomsByUserId(@Param("userId") String userId);
}


