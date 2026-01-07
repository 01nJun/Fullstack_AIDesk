package com.desk.service;

import com.desk.config.OllamaConfig;
import com.desk.dto.OllamaDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

@Service
@Log4j2
@RequiredArgsConstructor
public class AITicketClientServiceImpl implements AITicketClientService {

    @Qualifier("aiRestTemplate")
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final OllamaConfig ollamaConfig;

    @Override
    public String generateText(String prompt) {
        return callOllama(prompt, false);
    }

    @Override
    public String generateJson(String prompt) {
        return callOllama(prompt, true);
    }

    @Override
    public List<Double> getEmbedding(String text, String embeddingModel) {
        String apiUrl = ollamaConfig.getBaseUrl() + "/api/embeddings";
        // JSON 문자열 직접 구성 (간단한 구조)
        String requestJson = String.format("{\"model\": \"%s\", \"prompt\": \"%s\"}", embeddingModel, text.replace("\"", "\\\""));

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);
            
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode embeddingNode = root.get("embedding");
            
            if (embeddingNode != null && embeddingNode.isArray()) {
                return objectMapper.convertValue(embeddingNode, List.class);
            }
            return Collections.emptyList();

        } catch (Exception e) {
            log.error("[AI Client] Embedding Error: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private String callOllama(String prompt, boolean jsonMode) {
        String apiUrl = ollamaConfig.getBaseUrl() + "/api/chat";

        OllamaDTO.Request requestDTO = OllamaDTO.Request.builder()
                .model(ollamaConfig.getModelName())
                .messages(Collections.singletonList(
                        OllamaDTO.Message.builder().role("user").content(prompt).build()
                ))
                .stream(false)
                .format(jsonMode ? "json" : null)
                .options(OllamaDTO.Request.Options.builder().temperature(0.0).build())
                .build();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<OllamaDTO.Request> entity = new HttpEntity<>(requestDTO, headers);

            ResponseEntity<OllamaDTO.Response> response = restTemplate.postForEntity(apiUrl, entity, OllamaDTO.Response.class);

            if (response.getBody() == null || response.getBody().getMessage() == null) {
                throw new RuntimeException("Ollama response body is null");
            }

            return response.getBody().getMessage().getContent();

        } catch (Exception e) {
            log.error("[AI Client] Connection Error: {}", e.getMessage());
            throw new RuntimeException("AI Server Connection Failed: " + e.getMessage());
        }
    }
}