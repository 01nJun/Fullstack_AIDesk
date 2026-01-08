package com.desk.controller.chat;

import com.desk.dto.chat.ChatMessageCreateDTO;
import com.desk.dto.chat.ChatMessageDTO;
import com.desk.service.chat.ChatMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * WebSocket STOMP 메시지 핸들러
 * 
 * - SEND: /app/chat/send/{roomId}
 * - SUBSCRIBE: /topic/chat/{roomId}
 */
@Controller
@RequiredArgsConstructor
@Log4j2
public class ChatWebSocketController {
    
    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;
    
    /**
     * WebSocket을 통한 메시지 전송
     * 
     * 클라이언트가 /app/chat/send/{roomId}로 메시지를 보내면
     * 이 메서드가 처리하고 /topic/chat/{roomId}로 브로드캐스트
     */
    @MessageMapping("/chat/send/{roomId}")
    public void sendMessage(
            @DestinationVariable Long roomId,
            @Payload ChatMessageCreateDTO createDTO,
            Principal principal) {
        
        String senderId = principal.getName();
        log.info("[WebSocket] 메시지 수신 | roomId={} | senderId={} | type={}", 
                roomId, senderId, createDTO.getMessageType());
        
        try {
            // 메시지 저장 및 처리
            ChatMessageDTO message = chatMessageService.sendMessage(roomId, createDTO, senderId);
            
            // 채팅방의 모든 구독자에게 메시지 브로드캐스트
            messagingTemplate.convertAndSend("/topic/chat/" + roomId, message);
            
            log.info("[WebSocket] 메시지 브로드캐스트 완료 | roomId={} | messageSeq={}", 
                    roomId, message.getMessageSeq());
            
        } catch (Exception e) {
            log.error("[WebSocket] 메시지 처리 실패 | roomId={} | error={}", roomId, e.getMessage(), e);
            // 에러 발생 시 클라이언트에게 에러 메시지 전송 (선택사항)
            // messagingTemplate.convertAndSend("/topic/chat/" + roomId + "/errors", errorMessage);
        }
    }
}



