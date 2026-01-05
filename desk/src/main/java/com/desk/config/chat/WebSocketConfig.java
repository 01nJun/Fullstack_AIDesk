package com.desk.config.chat;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket STOMP 설정
 * 
 * - SEND 엔드포인트: /app/chat/send
 * - SUBSCRIBE 엔드포인트: /topic/chat/{roomId}
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 메시지 브로커 활성화 (Simple Broker 사용)
        // 클라이언트가 구독할 수 있는 destination prefix
        config.enableSimpleBroker("/topic");
        
        // 클라이언트가 메시지를 보낼 때 사용하는 destination prefix
        config.setApplicationDestinationPrefixes("/app");
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket 연결 엔드포인트
        // 클라이언트는 ws://host:port/ws 로 연결
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // CORS 설정 (프로덕션에서는 특정 도메인으로 제한)
                .withSockJS(); // SockJS 지원 (fallback 옵션)
    }
}


