package com.desk.controller.chat;

import com.desk.dto.PageRequestDTO;
import com.desk.dto.PageResponseDTO;
import com.desk.dto.chat.*;
import com.desk.service.chat.ChatMessageService;
import com.desk.service.chat.ChatRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * 채팅 REST API Controller
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Log4j2
public class ChatController {
    
    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;
    
    /**
     * GET /api/chat/rooms
     * 사용자가 참여 중인 채팅방 목록 조회
     */
    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoomDTO>> getRooms(Principal principal) {
        String userId = principal.getName();
        log.info("[Chat] 채팅방 목록 조회 | userId={}", userId);
        
        List<ChatRoomDTO> rooms = chatRoomService.getRooms(userId);
        return ResponseEntity.ok(rooms);
    }
    
    /**
     * POST /api/chat/rooms
     * 그룹 채팅방 생성
     */
    @PostMapping("/rooms")
    public ResponseEntity<ChatRoomDTO> createGroupRoom(
            @RequestBody ChatRoomCreateDTO createDTO,
            Principal principal) {
        String userId = principal.getName();
        log.info("[Chat] 그룹 채팅방 생성 | userId={} | name={}", userId, createDTO.getName());
        
        ChatRoomDTO room = chatRoomService.createGroupRoom(createDTO, userId);
        return ResponseEntity.ok(room);
    }
    
    /**
     * POST /api/chat/rooms/direct
     * 1:1 채팅방 생성 또는 기존 방 반환
     */
    @PostMapping("/rooms/direct")
    public ResponseEntity<ChatRoomDTO> createOrGetDirectRoom(
            @RequestBody DirectChatRoomCreateDTO createDTO,
            Principal principal) {
        String userId = principal.getName();
        log.info("[Chat] 1:1 채팅방 생성/조회 | userId={} | targetUserId={}", userId, createDTO.getTargetUserId());
        
        ChatRoomDTO room = chatRoomService.createOrGetDirectRoom(createDTO, userId);
        return ResponseEntity.ok(room);
    }
    
    /**
     * GET /api/chat/rooms/{roomId}
     * 채팅방 상세 정보 조회
     */
    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<ChatRoomDTO> getRoom(
            @PathVariable Long roomId,
            Principal principal) {
        String userId = principal.getName();
        log.info("[Chat] 채팅방 상세 조회 | roomId={} | userId={}", roomId, userId);
        
        ChatRoomDTO room = chatRoomService.getRoom(roomId, userId);
        return ResponseEntity.ok(room);
    }
    
    /**
     * POST /api/chat/rooms/{roomId}/leave
     * 채팅방 나가기
     */
    @PostMapping("/rooms/{roomId}/leave")
    public ResponseEntity<Map<String, String>> leaveRoom(
            @PathVariable Long roomId,
            Principal principal) {
        String userId = principal.getName();
        log.info("[Chat] 채팅방 나가기 | roomId={} | userId={}", roomId, userId);
        
        chatRoomService.leaveRoom(roomId, userId);
        return ResponseEntity.ok(Map.of("result", "SUCCESS"));
    }
    
    /**
     * POST /api/chat/rooms/{roomId}/invite
     * 채팅방 초대
     */
    @PostMapping("/rooms/{roomId}/invite")
    public ResponseEntity<Map<String, String>> inviteUsers(
            @PathVariable Long roomId,
            @RequestBody ChatInviteDTO inviteDTO,
            Principal principal) {
        String userId = principal.getName();
        log.info("[Chat] 채팅방 초대 | roomId={} | inviterId={} | userIds={}", 
                roomId, userId, inviteDTO.getUserIds());
        
        chatRoomService.inviteUsers(roomId, inviteDTO, userId);
        return ResponseEntity.ok(Map.of("result", "SUCCESS"));
    }
    
    /**
     * GET /api/chat/rooms/{roomId}/messages
     * 채팅방 메시지 목록 조회 (페이징)
     */
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<PageResponseDTO<ChatMessageDTO>> getMessages(
            @PathVariable Long roomId,
            @ModelAttribute PageRequestDTO pageRequestDTO,
            Principal principal) {
        String userId = principal.getName();
        log.info("[Chat] 메시지 목록 조회 | roomId={} | userId={} | page={}", 
                roomId, userId, pageRequestDTO.getPage());
        
        PageResponseDTO<ChatMessageDTO> response = chatMessageService.getMessages(roomId, userId, pageRequestDTO);
        return ResponseEntity.ok(response);
    }
    
    /**
     * POST /api/chat/rooms/{roomId}/messages
     * 메시지 전송 (REST API용, WebSocket 대체)
     */
    @PostMapping("/rooms/{roomId}/messages")
    public ResponseEntity<ChatMessageDTO> sendMessage(
            @PathVariable Long roomId,
            @RequestBody ChatMessageCreateDTO createDTO,
            Principal principal) {
        String userId = principal.getName();
        log.info("[Chat] 메시지 전송 | roomId={} | senderId={} | type={}", 
                roomId, userId, createDTO.getMessageType());
        
        ChatMessageDTO message = chatMessageService.sendMessage(roomId, createDTO, userId);
        return ResponseEntity.ok(message);
    }
    
    /**
     * PUT /api/chat/rooms/{roomId}/read
     * 읽음 처리
     */
    @PutMapping("/rooms/{roomId}/read")
    public ResponseEntity<Map<String, String>> markAsRead(
            @PathVariable Long roomId,
            @RequestBody ChatReadUpdateDTO readDTO,
            Principal principal) {
        String userId = principal.getName();
        log.info("[Chat] 읽음 처리 | roomId={} | userId={} | messageSeq={}", 
                roomId, userId, readDTO.getMessageSeq());
        
        chatMessageService.markAsRead(roomId, readDTO, userId);
        return ResponseEntity.ok(Map.of("result", "SUCCESS"));
    }
}


