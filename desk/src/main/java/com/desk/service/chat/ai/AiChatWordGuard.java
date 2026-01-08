package com.desk.service.chat.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * AI 채팅 금칙어 가드
 * - aichat_words.json에서 금칙어 리스트를 로드
 * - 공백/특수문자/줄바꿈을 무시하고 금칙어 감지
 */
@Component
@Log4j2
public class AiChatWordGuard implements ApplicationRunner {
    
    private final Set<String> normalizedWords = new HashSet<>();
    // [TEST MODE ONLY] 욕설 테스트 대본(치환) 매핑 목록
    private final List<TestMapping> testMappings = new ArrayList<>();
    private static final Pattern SPECIAL_CHARS_PATTERN = Pattern.compile("[^\\p{L}\\p{N}]");
    
    @Override
    public void run(ApplicationArguments args) throws Exception {
        loadWords();
        loadTestMappings();
    }
    
    /**
     * JSON 파일에서 금칙어 로드
     */
    private void loadWords() {
        try {
            ClassPathResource resource = new ClassPathResource("data/aichat_words.json");
            InputStream inputStream = resource.getInputStream();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(inputStream);
            JsonNode wordsNode = root.get("words");
            
            if (wordsNode != null && wordsNode.isArray()) {
                for (JsonNode wordNode : wordsNode) {
                    String word = wordNode.asText();
                    if (word != null && !word.trim().isEmpty()) {
                        // 정규화해서 저장
                        normalizedWords.add(normalize(word));
                    }
                }
            }
            
            log.info("[AiChatWordGuard] 금칙어 로드 완료 | count={}", normalizedWords.size());
            
        } catch (Exception e) {
            log.error("[AiChatWordGuard] 금칙어 로드 실패", e);
        }
    }

    /**
     * [TEST MODE ONLY]
     * 테스트 대본(치환) 로드
     * - data/aichat_test_filter.json
     */
    private void loadTestMappings() {
        try {
            ClassPathResource resource = new ClassPathResource("data/aichat_test_filter.json");
            if (!resource.exists()) {
                log.info("[AiChatWordGuard] 테스트 대본 파일 없음 (skip)");
                return;
            }

            try (InputStream inputStream = resource.getInputStream()) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(inputStream);
                JsonNode mappingsNode = root.get("mappings");

                if (mappingsNode != null && mappingsNode.isArray()) {
                    for (JsonNode mappingNode : mappingsNode) {
                        String key = mappingNode.hasNonNull("key") ? mappingNode.get("key").asText() : null;
                        String output = mappingNode.hasNonNull("output") ? mappingNode.get("output").asText() : null;

                        if (key == null || key.isBlank() || output == null) continue;

                        String normalizedKey = normalize(key);
                        if (normalizedKey.isBlank()) continue;

                        testMappings.add(new TestMapping(normalizedKey, output));
                    }
                }
            }

            log.info("[AiChatWordGuard] 테스트 대본 로드 완료 | count={}", testMappings.size());
        } catch (Exception e) {
            log.error("[AiChatWordGuard] 테스트 대본 로드 실패", e);
        }
    }
    
    /**
     * 문자열 정규화
     * - NFKC 정규화
     * - 소문자화
     * - 공백/특수문자/줄바꿈 제거 (한글/영문/숫자만 남김)
     */
    private String normalize(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        // NFKC 정규화
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFKC);
        
        // 소문자화
        normalized = normalized.toLowerCase();
        
        // 공백/특수문자/줄바꿈 제거 (한글/영문/숫자만 남김)
        normalized = SPECIAL_CHARS_PATTERN.matcher(normalized).replaceAll("");
        
        return normalized;
    }
    
    /**
     * 금칙어 포함 여부 감지
     * 
     * @param message 원본 메시지
     * @return true면 금칙어 포함
     */
    public boolean containsProfanity(String message) {
        if (message == null || message.trim().isEmpty()) {
            return false;
        }
        
        String normalizedMessage = normalize(message);
        
        for (String word : normalizedWords) {
            if (normalizedMessage.contains(word)) {
                log.debug("[AiChatWordGuard] 금칙어 감지 | word={}", word);
                return true;
            }
        }
        
        return false;
    }

    /**
     * [TEST MODE ONLY]
     * 테스트 대본 치환 적용
     *
     * - 메시지를 정규화한 뒤, mapping.key가 포함되면 output 반환
     * - 매칭 없으면 null
     */
    public String applyTestFilter(String message) {
        if (message == null || message.isBlank()) return null;
        if (testMappings.isEmpty()) return null;

        String normalizedMessage = normalize(message);
        if (normalizedMessage.isBlank()) return null;

        for (TestMapping mapping : testMappings) {
            if (normalizedMessage.contains(mapping.normalizedKey)) {
                return mapping.output;
            }
        }
        return null;
    }

    private static class TestMapping {
        private final String normalizedKey;
        private final String output;

        private TestMapping(String normalizedKey, String output) {
            this.normalizedKey = normalizedKey;
            this.output = output;
        }
    }
    
}

