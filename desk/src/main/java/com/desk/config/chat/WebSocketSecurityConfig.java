package com.desk.config.chat;

import com.desk.security.filter.JWTCheckFilter;
import com.desk.util.JWTUtil;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * WebSocket 인증 설정
 * STOMP 메시지에 JWT 토큰을 포함하여 인증 처리
 */
@Configuration
@EnableWebSocketMessageBroker
@Log4j2
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class WebSocketSecurityConfig implements WebSocketMessageBrokerConfigurer {
    
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    // CONNECT 시 JWT 토큰 검증
                    List<String> authHeaders = accessor.getNativeHeader("Authorization");
                    
                    if (authHeaders != null && !authHeaders.isEmpty()) {
                        String authHeader = authHeaders.get(0);
                        
                        try {
                            if (authHeader.startsWith("Bearer ")) {
                                String token = authHeader.substring(7);
                                
                                // JWT 검증
                                Map<String, Object> claims = JWTUtil.validateToken(token);
                                String email = (String) claims.get("email");
                                
                                if (email != null) {
                                    // Principal 설정
                                    Principal principal = () -> email;
                                    accessor.setUser(principal);
                                    
                                    log.info("[WebSocket] 인증 성공 | email={}", email);
                                }
                            }
                        } catch (Exception e) {
                            log.warn("[WebSocket] 인증 실패 | error={}", e.getMessage());
                            // 인증 실패 시 연결 거부
                            return null;
                        }
                    } else {
                        log.warn("[WebSocket] Authorization 헤더 없음");
                        return null;
                    }
                }
                
                return message;
            }
        });
    }
}

