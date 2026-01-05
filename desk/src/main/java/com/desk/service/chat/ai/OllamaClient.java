package com.desk.service.chat.ai;

import com.desk.config.OllamaConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Ollama API 클라이언트
 * - 예전 방식: 시스템 프롬프트 기반으로
 *   filteredMessage + shouldCreateTicket 를 JSON으로 한 번에 반환
 */
@Component
@RequiredArgsConstructor
@Log4j2
public class OllamaClient {

    private final OllamaConfig ollamaConfig;

    // ObjectMapper는 직접 생성해도 되지만, 성능/일관성 위해 하나만 둠
    private final ObjectMapper objectMapper = new ObjectMapper();

    private WebClient webClient;

    /**
     * WebClient 인스턴스 생성 (지연 초기화)
     * - HttpClient 레벨에서 connectTimeout, responseTimeout 설정
     * - Cloud 모델의 hang 문제를 방지하기 위해 네트워크 레벨 timeout 추가
     */
    private WebClient getWebClient() {
        if (webClient == null) {
            // Reactor Netty HttpClient 설정
            HttpClient httpClient = HttpClient.create()
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000) // 연결 timeout: 5초
                    .responseTimeout(Duration.ofSeconds(360)) // 응답 timeout: 360초 (6분) - Reactor timeout보다 약간 길게
                    .doOnConnected(conn -> 
                        conn.addHandlerLast(new ReadTimeoutHandler(360)) // 읽기 timeout: 360초 (6분)
                            .addHandlerLast(new WriteTimeoutHandler(10)) // 쓰기 timeout: 10초
                    );
            
            webClient = WebClient.builder()
                    .baseUrl(ollamaConfig.getBaseUrl())
                    .clientConnector(new ReactorClientHttpConnector(httpClient))
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .defaultHeader("X-API-Key", ollamaConfig.getApiKey())
                    .build();
        }
        return webClient;
    }
    
    /**
     * 모델이 Cloud 모델인지 판단
     * - 모델 이름에 "cloud"가 포함되어 있으면 Cloud 모델로 간주
     */
    private boolean isCloudModel(String modelName) {
        if (modelName == null) return false;
        return modelName.toLowerCase().contains("cloud");
    }
    
    /**
     * Cloud 모델에 대한 안전한 옵션 설정
     * - num_predict를 줄여서 응답 시간 단축
     * - temperature를 낮춰서 일관성 확보
     */
    private Map<String, Object> getModelOptions(String modelName, boolean isCloud) {
        if (isCloud) {
            // Cloud 모델: 더 짧은 응답과 낮은 temperature
            return Map.of(
                    "temperature", 0.1,
                    "top_p", 0.8,
                    "num_predict", 100 // Cloud 모델은 더 짧게 제한
            );
        } else {
            // 로컬 모델: 기존 설정 유지
            return Map.of(
                    "temperature", 0.2,
                    "top_p", 0.9,
                    "num_predict", 200
            );
        }
    }

    /**
     * Timeout 처리 전략:
     * 1. WebClient 레벨: HttpClient에 responseTimeout(35초) 설정
     * 2. Reactor 레벨: .timeout(Duration.ofSeconds(30)) - WebClient timeout보다 짧게
     * 3. Cloud 모델: 별도 timeout 정책 및 옵션 적용
     * 4. 모든 에러는 onErrorResume으로 fallback 처리
     * 
     * @param originalMessage 원문 메시지
     * @return Mono<FilterResult>
     */
    public Mono<FilterResult> filterMessage(String originalMessage) {
        if (originalMessage == null || originalMessage.trim().isEmpty()) {
            return Mono.just(FilterResult.builder()
                    .filteredMessage(originalMessage)
                    .shouldCreateTicket(false)
                    .build());
        }

        // 모델 선택 및 Cloud 모델 여부 판단
        String modelName = ollamaConfig.getModelName();
        boolean isCloud = isCloudModel(modelName);
        
        // Cloud 모델 감지 로그
        if (isCloud) {
            log.info("[Ollama] Cloud 모델 감지 | model={}", modelName);
        }

        String systemPrompt = """
            너는 메신저에서 전송될 메시지를 '정중한 업무용 문장'으로 자동 변환하는 필터 AI다.
            너의 출력은 반드시 JSON 객체 하나만 반환해야 하며, JSON 이외의 텍스트(설명/주석/코드블록/추가 문장)는 절대 출력하지 않는다.
            
            [처리 순서 — 반드시 이 순서를 지킬 것]
            1. 욕설, 비속어, 공격적 표현을 모두 제거하거나 완곡한 표현으로 대체한다.
            2. 문장의 의미는 유지하되, 상대를 존중하는 말투로 변환한다.
            3) 변환된 문장을 기준으로 티켓 생성 필요 여부를 판단한다.
            
            [변환 원칙]
            - 핵심 사실/요청/긴급도는 유지한다. (단, 공격성은 제거)
            - 상대방 비난/인신공격 → 문제/상황 중심 표현으로 바꾼다.
            - 명령형/다그침 → 요청형/협의형으로 바꾼다.
            - 과격한 감정 표현 → 불편/우려/긴급 등의 중립 표현으로 바꾼다.
            - 개인정보/실명 비난이 포함돼도 원문을 그대로 옮기지 말고, 필요 시 "담당자/해당 인원" 등으로 일반화한다.
            - 원문 일부를 따옴표로 인용하거나 그대로 재현하지 않는다. (원문 절대 포함 금지)
            
            [티켓 생성 트리거 판단 규칙]
            아래 의미가 '명확히' 포함되면 shouldCreateTicket = true:
            - 티켓 생성/등록/처리 요청 (예: "티켓 생성해줘", "티켓으로 처리", "티켓 등록", "이슈 티켓화")
            - 이슈를 공식적으로 기록/추적/접수해달라는 요청 (예: "이슈 남겨줘", "접수해줘", "추적 필요", "정식으로 등록")
            반대로,
            - 단순 불평/욕설/감정 표현만 있고 구체적인 요청이 없으면 false
            - "티켓" 단어가 농담/비유로만 쓰였고 업무 처리 의미가 없으면 false
            
            [응답 형식 — 반드시 아래 키 그대로]
            {
              "filteredMessage": "정중하게 변환된 메시지",
              "shouldCreateTicket": true/false
            }
            
            [예시 — 아래 스타일을 반드시 따라라]
            입력: "야 김부장 개새끼야 당장 티켓 만들어"
            출력:
            {
              "filteredMessage": "김부장님. 해당 이슈를 티켓으로 등록해 주세요.",
              "shouldCreateTicket": true
            }
            
            입력: "진짜 개빡치네 또 버그냐"
            출력:
            {
              "filteredMessage": "현재 시스템 동작이 기대와 달라 확인이 필요합니다.",
              "shouldCreateTicket": false
            }
            
            입력: "지금 당장 처리해. 왜 이렇게 느려?"
            출력:
            {
              "filteredMessage": "가능한 빠르게 처리 상황을 확인해 주실 수 있을까요?",
              "shouldCreateTicket": false
            }
            
            입력: "너 때문에 다 망했잖아. 책임져."
            출력:
            {
              "filteredMessage": "이번 이슈의 원인과 대응 방안을 함께 확인하고 싶습니다.",
              "shouldCreateTicket": false
            }
            
            [마지막 제한]
            - 출력은 반드시 JSON 1개만
            - 줄바꿈/공백은 자유지만 JSON 문법은 반드시 유효해야 함
            - filteredMessage는 비어있으면 안 됨
            """;

        Map<String, Object> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);

        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", originalMessage);

        // Ollama API 요청 바디
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", modelName);
        requestBody.put("messages", List.of(systemMessage, userMessage));
        requestBody.put("stream", false);
        requestBody.put("format", "json");
        requestBody.put("think", false);
        
        // Cloud 모델 여부에 따라 옵션 분기
        requestBody.put("options", getModelOptions(modelName, isCloud));

        // timeout: 6분 (360초) - 임시 설정
        Duration timeoutDuration = Duration.ofSeconds(360);
        
        log.info("[Ollama] 필터링 요청 | baseUrl={} | model={} | isCloud={} | timeout={}s", 
                ollamaConfig.getBaseUrl(), modelName, isCloud, timeoutDuration.getSeconds());

        // Fallback 결과 (에러 발생 시 사용)
        FilterResult fallbackResult = FilterResult.builder()
                .filteredMessage(originalMessage.trim())
                .shouldCreateTicket(false)
                .build();

        return getWebClient()
                .post()
                .uri("/api/chat")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                // Reactor 레벨 timeout: WebClient timeout보다 짧게 설정하여 명시적으로 에러 발생
                .timeout(timeoutDuration)
                // map 연산자 사용 (flatMap 대신) - timeout이 제대로 전파되도록
                .map(rawResponse -> {
                    // content 변수를 try 블록 밖에서 선언하여 catch 블록에서도 접근 가능하도록 함
                    String content = null;
                    try {
                        // 1) 응답에서 message.content(문자열) 뽑기
                        content = extractContent(rawResponse);

                        // 2) content 비면 fallback
                        if (content == null || content.trim().isEmpty()) {
                            log.warn("[Ollama] content 비어있음 -> 원문 fallback");
                            return fallbackResult;
                        }

                        // 3) content는 JSON 문자열이어야 함. 파싱
                        Map<String, Object> parsed = objectMapper.readValue(
                                content,
                                new TypeReference<Map<String, Object>>() {}
                        );

                        String filteredMessage = parsed.get("filteredMessage") != null
                                ? parsed.get("filteredMessage").toString()
                                : null;

                        boolean shouldCreateTicket = parseBoolean(parsed.get("shouldCreateTicket"));

                        if (filteredMessage == null || filteredMessage.trim().isEmpty()) {
                            filteredMessage = originalMessage.trim();
                        }

                        return FilterResult.builder()
                                .filteredMessage(filteredMessage.trim())
                                .shouldCreateTicket(shouldCreateTicket)
                                .build();

                    } catch (Exception e) {
                        // JSON 파싱 실패 시: content 자체를 메시지로 쓰고, 티켓은 키워드로 보수적으로 판단
                        log.warn("[Ollama] JSON 파싱 실패 -> content fallback 사용 | content={}", content, e);

                        boolean shouldCreateTicket = content != null && (
                                content.contains("티켓") || content.toLowerCase().contains("ticket")
                        );

                        return FilterResult.builder()
                                .filteredMessage(content != null ? content.trim() : originalMessage.trim())
                                .shouldCreateTicket(shouldCreateTicket)
                                .build();
                    }
                })
                // WebClientResponseException: HTTP 에러 (4xx, 5xx)
                .onErrorResume(WebClientResponseException.class, ex -> {
                    log.error("[Ollama] HTTP 에러 | status={} | error={} | model={}",
                            ex.getStatusCode(), ex.getMessage(), modelName);
                    return Mono.just(fallbackResult);
                })
                // TimeoutException: Reactor timeout 발생
                .onErrorResume(TimeoutException.class, ex -> {
                    log.error("[Ollama] Timeout 발생 | timeout={}s | model={} | isCloud={}",
                            timeoutDuration.getSeconds(), modelName, isCloud);
                    return Mono.just(fallbackResult);
                })
                // 기타 모든 예외 (네트워크 에러, 파싱 에러 등)
                .onErrorResume(Exception.class, ex -> {
                    log.error("[Ollama] 예외 발생 | type={} | error={} | model={}",
                            ex.getClass().getSimpleName(), ex.getMessage(), modelName, ex);
                    return Mono.just(fallbackResult);
                })
                // 최종 안전장치: 모든 에러를 잡아서 fallback 반환
                .onErrorReturn(fallbackResult);
    }

    /**
     * 기존처럼 "변환된 메시지"만 필요할 때 쓰는 편의 메서드
     */
    public Mono<String> processMessage(String originalMessage) {
        return filterMessage(originalMessage)
                .map(FilterResult::getFilteredMessage);
    }

    // -----------------------
    // 내부 유틸
    // -----------------------

    @SuppressWarnings("unchecked")
    private String extractContent(Map rawResponse) {
        if (rawResponse == null) return null;

        Object msgObj = rawResponse.get("message");
        if (!(msgObj instanceof Map)) return null;

        Map<String, Object> message = (Map<String, Object>) msgObj;
        Object contentObj = message.get("content");
        return contentObj != null ? contentObj.toString() : null;
    }

    private boolean parseBoolean(Object obj) {
        if (obj == null) return false;
        if (obj instanceof Boolean) return (Boolean) obj;
        return Boolean.parseBoolean(obj.toString());
    }

    // -----------------------
    // DTO
    // -----------------------

    @Builder
    @Getter
    public static class FilterResult {
        private String filteredMessage;
        private boolean shouldCreateTicket;
    }
}
