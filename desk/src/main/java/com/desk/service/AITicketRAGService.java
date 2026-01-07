package com.desk.service;

public interface AITicketRAGService {

    // 초기화 (서버 시작 시 데이터 로딩)
    void init();

    // 검색 (부서명과 사용자 질문을 받아 가장 유사한 가이드라인 반환)
    String searchContext(String targetDept, String userInput);
}