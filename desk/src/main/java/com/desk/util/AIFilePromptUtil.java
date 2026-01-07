package com.desk.util;

import java.time.LocalDate;

/**
 * [AI 파일조회 프롬프트 관리 유틸]
 * 파일 검색을 위한 자연어 파싱 프롬프트를 관리합니다.
 */
public class AIFilePromptUtil {

    // ========================================================================
    // 파일 검색 자연어 파싱 프롬프트 - JSON 출력 필수
    // ========================================================================
    private static final String FILE_SEARCH_PARSE_INSTRUCTION = """
            당신은 파일 검색 쿼리 파싱 전문가입니다.
            사용자의 자연어 입력에서 검색 조건을 추출하여 JSON 형식으로 반환하십시오.
            
            ### 현재 날짜 기준 ###
            오늘: %s
            
            ### 추출해야 할 정보 ###
            1. **기간 (dateRange)**: 파일 생성/수정 날짜 범위
               - 인식 키워드: "오늘", "어제", "이번주", "지난주", "이번달", "지난달", "1월", "2월", "12월에서 2월사이", "1월부터 3월까지", "다음주", "다음 달" 등
               - 날짜 표현: "1월 20일", "2026-01-25", "다음주 금요일", "이번달 말", "2주 후" 등
               - 변환 규칙: 오늘(%s) 기준으로 계산하여 'YYYY-MM-DD' 형식으로 변환
               - 범위가 없으면 단일 날짜를 from과 to 모두에 설정
               - 날짜 정보가 없으면 null
            
            2. **상대 이메일 (counterEmail)**: 특정 상대와 주고받은 파일
               - 이메일 주소 직접 추출 (예: "user@example.com")
               - 닉네임 추출 (예: "황시우", "김철수") - 이메일 변환은 백엔드에서 처리
               - "이랑", "랑", "와", "과", "한테", "에게" 같은 조사 제거
               - 상대 정보가 없으면 null
            
            3. **부서 (department)**: 특정 부서와 관련된 파일
               - 인식 키워드: "디자인", "개발", "영업", "인사", "재무", "기획"
               - "디자인팀", "디자인 부서", "DESIGN" 등도 인식
               - 부서 정보가 없으면 null
            
            4. **보낸/받은 필터 (senderOnly/receiverOnly)**: 파일 전송 방향
               - "보낸", "전송한", "전달한" → senderOnly: true
               - "받은", "수신한" → receiverOnly: true
               - "주고받은", "얘기한", "대화한" → 둘 다 false (전체 조회)
               - 기본값: 둘 다 false
            
            5. **키워드 (keyword)**: 파일명, 제목, 내용 검색용 키워드
               - 날짜, 상대, 부서, 동작 키워드("보낸", "받은" 등)를 제거한 나머지 텍스트
               - 공백 정리 후 반환
               - 키워드가 없으면 빈 문자열("")
            
            ### 사용자 입력 ###
            %s
            
            ### 출력 형식 (JSON 필수) ###
            반드시 아래 JSON 포맷을 준수하여 응답해야 합니다. 마크다운(```)이나 잡담을 섞지 마십시오. 순수 JSON 문자열만 출력하십시오.
            {
                "dateRange": {
                    "from": "YYYY-MM-DD" 또는 null,
                    "to": "YYYY-MM-DD" 또는 null
                },
                "counterEmail": "이메일 또는 닉네임" 또는 null,
                "department": "DESIGN|DEVELOPMENT|SALES|HR|FINANCE|PLANNING" 또는 null,
                "keyword": "검색 키워드",
                "senderOnly": true 또는 false,
                "receiverOnly": true 또는 false
            }
            """;

    public static String getFileSearchParsePrompt(String userInput) {
        String today = LocalDate.now().toString();
        return String.format(FILE_SEARCH_PARSE_INSTRUCTION, today, today, userInput);
    }
}


