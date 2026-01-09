package com.desk.controller.chat;

import com.desk.dto.PageRequestDTO;
import com.desk.dto.PageResponseDTO;
import com.desk.dto.chat.*;
import com.desk.service.chat.ChatMessageService;
import com.desk.service.chat.ChatRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.Collections;
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
    private final SimpMessagingTemplate messagingTemplate;
    
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
            @PathVariable("roomId") Long roomId,
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
            @PathVariable("roomId") Long roomId,
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
            @PathVariable("roomId") Long roomId,
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
            @PathVariable("roomId") Long roomId,
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
            @PathVariable("roomId") Long roomId,
            @RequestBody ChatMessageCreateDTO createDTO,
            Principal principal) {
        String userId = principal.getName();
        log.info("[Chat] 메시지 전송 | roomId={} | senderId={} | type={}", 
                roomId, userId, createDTO.getMessageType());
        
        ChatMessageDTO message = chatMessageService.sendMessage(roomId, createDTO, userId);
        // ✅ REST 전송이어도 구독자에게 브로드캐스트(첨부 전송/WS 불안정 대비)
        messagingTemplate.convertAndSend("/topic/chat/" + roomId, message);
        return ResponseEntity.ok(message);
    }

    /**
     * POST /api/chat/rooms/{roomId}/messages/files
     * 메시지 전송 + 파일 첨부 (multipart/form-data)
     *
     * - WS로는 multipart를 못 보내므로 첨부가 있을 때만 이 엔드포인트를 사용
     * - 저장 후 /topic/chat/{roomId}로 브로드캐스트
     */
    @PostMapping(value = "/rooms/{roomId}/messages/files", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<ChatMessageDTO> sendMessageWithFiles(
            @PathVariable("roomId") Long roomId,
            @RequestPart("message") ChatMessageCreateDTO createDTO,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            Principal principal
    ) {
        String userId = principal.getName();
        List<MultipartFile> safeFiles = (files == null) ? Collections.emptyList() : files;
        log.info("[Chat] 메시지+파일 전송 | roomId={} | senderId={} | fileCount={}", roomId, userId, safeFiles.size());

        ChatMessageDTO message = chatMessageService.sendMessageWithFiles(roomId, createDTO, safeFiles, userId);
        messagingTemplate.convertAndSend("/topic/chat/" + roomId, message);
        return ResponseEntity.ok(message);
    }
    
    /**
     * PUT /api/chat/rooms/{roomId}/read
     * 읽음 처리
     */
    @PutMapping("/rooms/{roomId}/read")
    public ResponseEntity<Map<String, String>> markAsRead(
            @PathVariable("roomId") Long roomId,
            @RequestBody ChatReadUpdateDTO readDTO,
            Principal principal) {
        String userId = principal.getName();
        log.info("[Chat] 읽음 처리 | roomId={} | userId={} | messageSeq={}", 
                roomId, userId, readDTO.getMessageSeq());
        
        chatMessageService.markAsRead(roomId, readDTO, userId);
        return ResponseEntity.ok(Map.of("result", "SUCCESS"));
    }
}


