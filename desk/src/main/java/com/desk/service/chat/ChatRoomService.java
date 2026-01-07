package com.desk.service.chat;

import com.desk.dto.chat.*;

import java.util.List;

/**
 * 채팅방 관련 비즈니스 로직 인터페이스
 */
public interface ChatRoomService {
    
    /**
     * 사용자가 참여 중인 채팅방 목록 조회
     */
    List<ChatRoomDTO> getRooms(String userId);
    
    /**
     * 채팅방 상세 정보 조회
     */
    ChatRoomDTO getRoom(Long roomId, String userId);
    
    /**
     * 그룹 채팅방 생성
     */
    ChatRoomDTO createGroupRoom(ChatRoomCreateDTO createDTO, String creatorId);
    
    /**
     * 1:1 채팅방 생성 또는 기존 방 반환
     */
    ChatRoomDTO createOrGetDirectRoom(DirectChatRoomCreateDTO createDTO, String userId);
    
    /**
     * 채팅방 나가기
     */
    void leaveRoom(Long roomId, String userId);
    
    /**
     * 채팅방 초대
     */
    void inviteUsers(Long roomId, ChatInviteDTO inviteDTO, String inviterId);
}

