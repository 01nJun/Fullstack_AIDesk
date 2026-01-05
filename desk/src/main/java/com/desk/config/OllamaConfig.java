package com.desk.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Ollama 설정 클래스
 * 환경 변수로부터 Ollama 관련 설정을 주입받음
 */
@Configuration
@Getter
public class OllamaConfig {
    
    @Value("${OLLAMA_BASE_URL:http://127.0.0.1:11434}")
    private String baseUrl;
    
    @Value("${OLLAMA_MODEL_NAME:qwen3:8b}")
    private String modelName;
    
    @Value("${OLLAMA_API_KEY:}")
    private String apiKey;
}


