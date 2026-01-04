package com.desk.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;

@Service
@Log4j2
@RequiredArgsConstructor
public class AITicketRAGServiceImpl implements AITicketRAGService {

    private final AITicketClientService aiClient; // 인터페이스 주입
    private final ObjectMapper objectMapper;

    @Value("${ai.ollama.embedding-model-name}")
    private String embeddingModelName;

    // 메모리에 상주할 지식 데이터베이스
    private final List<Document> knowledgeBase = new ArrayList<>();

    // 내부 클래스: 문서 단위
    private static class Document {
        String content;
        String dept;
        List<Double> vector;

        public Document(String content, String dept, List<Double> vector) {
            this.content = content;
            this.dept = dept;
            this.vector = vector;
        }
    }

    @Override
    @PostConstruct // 서버 시작 시 자동 실행
    public void init() {
        try {
            log.info("[RAG] Loading knowledge base from JSON files...");
            
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            // resources/data 폴더 아래의 knowledge_*.json 파일들 스캔
            Resource[] resources = resolver.getResources("classpath:data/knowledge_*.json");

            if (resources.length == 0) {
                log.warn("[RAG] No knowledge JSON files found in 'classpath:data/'.");
                return;
            }

            for (Resource res : resources) {
                String filename = res.getFilename();
                String dept = extractDeptFromFilename(filename); // 파일명에서 부서 추출
                
                try (InputStream is = res.getInputStream()) {
                    // 파일 포맷: {"DEVELOPMENT": ["내용1", "내용2"]} 구조 대응
                    Map<String, List<String>> data = objectMapper.readValue(is, new TypeReference<>() {});
                    
                    for (Map.Entry<String, List<String>> entry : data.entrySet()) {
                        String jsonDept = entry.getKey(); // JSON 키값 사용 (부서명)
                        for (String text : entry.getValue()) {
                            // 텍스트를 벡터로 변환 (AI 서버 호출)
                            List<Double> vector = aiClient.getEmbedding(text, embeddingModelName);
                            if (!vector.isEmpty()) {
                                knowledgeBase.add(new Document(text, jsonDept, vector));
                            }
                        }
                    }
                }
            }
            log.info("[RAG] Successfully loaded {} documents from {} files.", knowledgeBase.size(), resources.length);

        } catch (Exception e) {
            log.error("[RAG] Initialization Failed: {}", e.getMessage());
        }
    }

    @Override
    public String searchContext(String targetDept, String userInput) {
        if (knowledgeBase.isEmpty()) return "관련 가이드라인 없음";

        // 1. 사용자 질문을 벡터로 변환
        List<Double> queryVector = aiClient.getEmbedding(userInput, embeddingModelName);
        if (queryVector.isEmpty()) return "가이드라인 검색 실패";

        // 2. 유사도 계산 및 정렬 (우선순위 큐 사용)
        PriorityQueue<Document> pq = new PriorityQueue<>(
            (d1, d2) -> Double.compare(cosineSimilarity(queryVector, d2.vector), cosineSimilarity(queryVector, d1.vector))
        );

        for (Document doc : knowledgeBase) {
            // 해당 부서 문서만 검색 (부서 미정이면 전체 검색)
            if (targetDept == null || targetDept.equalsIgnoreCase(doc.dept)) {
                pq.offer(doc);
            }
        }

        // 3. 상위 3개 결과 합치기
        StringBuilder context = new StringBuilder();
        int k = 3;
        while (k-- > 0 && !pq.isEmpty()) {
            context.append("- ").append(pq.poll().content).append("\n");
        }
        
        return context.length() > 0 ? context.toString() : "관련된 상세 가이드라인을 찾지 못했습니다. 육하원칙(5W1H)에 따라 상세히 작성해주세요.";
    }

    private String extractDeptFromFilename(String filename) {
        if (filename == null) return "COMMON";
        // knowledge_dev.json -> DEV
        return filename.replace("knowledge_", "").replace(".json", "").toUpperCase();
    }

    private double cosineSimilarity(List<Double> v1, List<Double> v2) {
        if (v1.size() != v2.size()) return 0.0;
        double dotProduct = 0.0, normA = 0.0, normB = 0.0;
        for (int i = 0; i < v1.size(); i++) {
            dotProduct += v1.get(i) * v2.get(i);
            normA += Math.pow(v1.get(i), 2);
            normB += Math.pow(v2.get(i), 2);
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}