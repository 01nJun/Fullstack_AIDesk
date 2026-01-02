package com.desk.service;

import java.util.List;

public interface AITicketClientService {
    
    // 단순 텍스트 생성 (라우팅, 담당자 확인용)
    String generateText(String prompt);

    // JSON 형식 응답 생성 (티켓 인터뷰용)
    String generateJson(String prompt);

    // 텍스트 임베딩 (벡터 변환, RAG용)
    List<Double> getEmbedding(String text, String embeddingModel);
}