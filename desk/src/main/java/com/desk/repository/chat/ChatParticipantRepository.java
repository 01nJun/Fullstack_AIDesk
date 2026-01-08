package com.desk.repository.chat;

import com.desk.domain.ChatParticipant;
import com.desk.domain.ChatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Long> {
    
    /**
     * 채팅방 ID와 사용자 ID로 참여자 조회
     */
    Optional<ChatParticipant> findByChatRoomIdAndUserId(Long chatRoomId, String userId);
    
    /**
     * 채팅방의 모든 참여자 조회 (ACTIVE 상태만)
     */
    List<ChatParticipant> findByChatRoomIdAndStatus(Long chatRoomId, ChatStatus status);
    
    /**
     * 사용자가 특정 채팅방에 참여 중인지 확인
     */
    @Query("SELECT COUNT(cp) > 0 FROM ChatParticipant cp " +
           "WHERE cp.chatRoom.id = :roomId AND cp.userId = :userId AND cp.status = 'ACTIVE'")
    boolean existsByChatRoomIdAndUserIdAndActive(@Param("roomId") Long roomId, @Param("userId") String userId);
    
    /**
     * 채팅방의 모든 참여자 조회 (상태 무관)
     */
    List<ChatParticipant> findByChatRoomId(Long chatRoomId);
}



