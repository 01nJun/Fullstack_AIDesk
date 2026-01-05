package com.desk.service.chat.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * AI 메시지 처리 컴포넌트
 * Ollama를 사용하여 메시지 정제 및 티켓 문맥 감지 수행
 */
@Component
@RequiredArgsConstructor
@Log4j2
public class AiMessageProcessor {
    
    @Value("${ai.message.enabled:false}")
    private boolean aiEnabled;
    
    private final OllamaClient ollamaClient;
    
    /**
     * AI 메시지 처리 결과 DTO
     */
    public static class ProcessResult {
        private final String processedContent;
        private final boolean ticketTrigger;
        
        public ProcessResult(String processedContent, boolean ticketTrigger) {
            this.processedContent = processedContent;
            this.ticketTrigger = ticketTrigger;
        }
        
        public String getProcessedContent() {
            return processedContent;
        }
        
        public boolean isTicketTrigger() {
            return ticketTrigger;
        }
    }
    
    /**
     * AI 메시지 처리 수행
     * 
     * @param originalMessage 원문 메시지
     * @param frontendAiEnabled 프론트엔드에서 전달한 AI 사용 여부
     * @return 처리 결과 (정제된 메시지, 티켓 트리거 여부)
     */
    public ProcessResult processMessage(String originalMessage, Boolean frontendAiEnabled) {
        // 서버 설정이 false이면 AI 기능 사용 안 함
        if (!aiEnabled) {
            log.debug("[AI] 서버 설정으로 인해 AI 기능 비활성화");
            return new ProcessResult(originalMessage, false);
        }
        
        // 프론트엔드 요청이 false이면 AI 기능 사용 안 함
        if (frontendAiEnabled == null || !frontendAiEnabled) {
            log.debug("[AI] 프론트엔드 요청으로 인해 AI 기능 비활성화");
            return new ProcessResult(originalMessage, false);
        }
        
        // 원문 메시지가 없으면 그대로 반환
        if (originalMessage == null || originalMessage.trim().isEmpty()) {
            return new ProcessResult(originalMessage, false);
        }
        
        log.info("[AI] 메시지 처리 시작");
        
        try {
            // 동기적으로 처리 (블로킹) - timeout 설정 포함 (6분)
            // filterMessage는 필터링된 메시지와 티켓 생성 여부를 함께 반환
            OllamaClient.FilterResult result = ollamaClient.filterMessage(originalMessage)
                    .block(Duration.ofSeconds(360)); // 360초 (6분)
            
            log.info("[AI] 메시지 처리 완료 | ticketTrigger={}", result.isShouldCreateTicket());
            
            // 원문 메시지 참조 제거 (가비지 컬렉션 대상)
            originalMessage = null;
            
            return new ProcessResult(result.getFilteredMessage(), result.isShouldCreateTicket());
            
        } catch (Exception e) {
            log.error("[AI] 메시지 처리 중 예외 발생 | type={} | error={}", 
                    e.getClass().getSimpleName(), e.getMessage(), e);
            // 에러 발생 시 원문 반환
            return new ProcessResult(originalMessage, false);
        }
    }
}

