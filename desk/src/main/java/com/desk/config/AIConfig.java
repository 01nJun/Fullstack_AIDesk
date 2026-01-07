package com.desk.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * [AI 공통 설정]
 * 1. 타임아웃 5분 설정
 * 2. API Key 자동 주입 (Interceptor)
 */
@Configuration
@RequiredArgsConstructor
public class AIConfig {

    private final OllamaConfig ollamaConfig;

    @Bean("aiRestTemplate")
    public RestTemplate aiRestTemplate() {
        // 1. 타임아웃 설정
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(100 * 1000);      // 연결 10초
        factory.setReadTimeout(5 * 60 * 1000);     // 읽기 5분

        RestTemplate restTemplate = new RestTemplate(factory);

        // 2. [핵심] 헤더 자동 주입 인터셉터 추가
        // 이 RestTemplate으로 보내는 모든 요청에 자동으로 헤더가 붙습니다.
        List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
        interceptors.add((request, body, execution) -> {
            request.getHeaders().add("X-API-Key", ollamaConfig.getApiKey());
            return execution.execute(request, body);
        });
        
        restTemplate.setInterceptors(interceptors);

        return restTemplate;
    }
}