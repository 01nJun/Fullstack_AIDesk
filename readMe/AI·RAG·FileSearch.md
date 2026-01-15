## 박태오

# 🧑‍💻 담당한 기능 요약

- **AI 업무티켓 (Ticket Copilot)**
    
    라우팅 → 담당자 확정(DB 검증) → RAG+JSON 인터뷰 기반으로 **대화형 티켓 자동 작성 플로우** 구현
    
- **AI 업무티켓 시나리오 모드(JSON 템플릿 기반)**
    
     stage 세션을 두고, **시연/반복패턴은 템플릿 우선 매칭** + 매칭 실패 시 LLM로 fallback
    
- **RAG(부서별 지식 주입)**
    
    knowledge_*.json을 임베딩 적재하고 cosine 유사도 Top-K를 컨텍스트로 주입해 **부서별 질문 품질/누락 질문**을 보강
    
- **AI 파일조회 (Natural Language File Retrieval)**
    
    기간/상대/부서/송수신/키워드/긴급 조건을 **규칙 기반으로 1차 파싱**하고, 애매한 입력은 **AI로 보완**하는 하이브리드 검색 로직 구현
    
- **AI 파일조회 검색 전략 고도화(0건 UX 개선)**
    
    strict AND → AI 재파싱(strict 재시도) → 키워드 refine → overlap(조건 겹침 최대) 순으로 단계화해 **“0건 체감”을 줄이는 결과 제시 UX**를 서버에서 설계
    
- **AI 파일조회 보안(ACL) 통합**
    
    다운로드/뷰 시점에 티켓/채팅 파일 모두 **접근 가능한 사용자만 내려주도록 권한 체크를 강제**하여 보안 누수 방지
    
- **AI 비서함 통합 UI(모달/위젯)**
    
    채팅 모달과 **업무티켓/파일조회가 한 흐름으로 전환**되도록 통합 UI(탭/전환/패널)를 구성하고 연결
    
- **AI 채팅 가드/제재(보조 기능)**
    
    AI OFF 상태 금칙어 1차 감지 + 10초 2회 경고 + OFF 후 재욕설 60초 강제 ON 제재로 **채팅 사고 방지 안전장치**를 구현
    
- **AI 채팅 파일 첨부(드래그&드랍)**
    
    첨부파일은 multipart REST로 전송하고 저장 후 WS로 브로드캐스트하는 방식으로 **실시간성 유지 + 첨부 UX**를 추가
    

---

# 🚀 주요 기능

1. AI 업무티켓: Routing → 담당자 확정(DB 검증) → RAG+JSON 인터뷰 기반 자동 작성
2. 반복 패턴 템플릿 우선 매칭 + LLM fallback
3. RAG(knowledge_*.json): 임베딩/유사도 기반 부서별 컨텍스트 주입*
4. AI 파일조회: 규칙 기반 파싱(기간/상대/부서/송수신/긴급/키워드) + AI(JSON) 보완
5. AI 파일조회 검색전략: strict → AI 재파싱 → keyword refine → overlap 결과 제시(0건 UX 개선)
6. AI 파일조회 ACL: 티켓/채팅 파일 view/download 권한 검증 통합
7. AI 비서함 통합 UI: 채팅·업무티켓·파일조회 모달/위젯 단일 진입점 및 전환 UX
8. AI 채팅 가드/제재 + 파일첨부(Drag&Drop 포함)


---

| 구현 기능 | Front-End 담당 | Back-End 담당 | 설계 및 특징 |
| --- | --- | --- | --- |
| **AI 업무티켓 (Ticket Copilot)** | • AIChatWidget 티켓 작성 UI (대화+우측 폼 동기화)<br>• 첨부파일 선택·미리보기·전송<br>• 티켓 생성 완료 시 TICKET_PREVIEW 메시지 전송 (WS/REST fallback) | • /api/ai/ticket/chat<br>• AITicketServiceImpl 3단계 (라우팅→담당자→인터뷰)<br>• MemberRepository.findByNickname 담당자 검증·확정 | • 서버 단계형 흐름으로 안정적<br>• 담당자 실존 검증으로 오발송 방지 |
| **AI 업무티켓 시나리오 모드 (JSON)** | • 시연/반복 케이스 질문·응답 UX 검증<br>• 시나리오 흐름에 맞춘 화면 반영 | • AITicketScenarioServiceImpl + design_scenario.json<br>• stage 세션 관리 + out-of-order 입력 처리 | • 템플릿 즉시 응답 (지연·비용↓)<br>• 미매칭 시 LLM fallback |
| **AI 업무티켓 프롬프트/구조화 응답** | • aiSecretaryApi로 대화 요청/응답 처리 | • AITicketPromptUtil로 Routing/Assignee/Interview 분리<br>• Interview JSON 포맷 강제 (updatedTicket/responseToUser) | • 프롬프트 단계 분리로 안정성↑<br>• JSON 강제로 상태 꼬임 최소화 |
| **RAG (부서별 지식 주입)** | - | • AITicketRAGServiceImpl: knowledge_*.json 로드→임베딩→cosine Top-K 컨텍스트 생성 | • 부서 가이드/체크리스트를 컨텍스트로 주입해 질문 품질 강화 |
| **AI 파일조회 (자연어 파싱 + 필터)** | • AIChatWidget file 모드<br>• AIFilePanel 결과 리스트·다운로드 UI | • /api/ai/file/chat<br>• AIFileServiceImpl 규칙 파싱 (기간/상대/부서/송수신/긴급/키워드) + Komoran 토큰화 | • 규칙 1차 파싱으로 속도·안정성 확보<br>• 다중 조건 교집합 검색 |
| **AI 파일조회 AI(JSON) 보완 파싱** | • 애매한 질의도 동일 UI로 결과 표시 | • AIFilePromptUtil로 자연어→JSON 구조화<br>• 조건 부족/애매할 때만 AI 호출 (needsAIParsing) | • AI는 필요할 때만 호출 (비용·지연·오해↓) |
| **AI 파일조회 검색전략 고도화 (0건 UX)** | • 서버 aiMessage 안내 문구 렌더링 | • strict AND → AI 재파싱(strict 재시도) → keyword refine → overlap(조건 겹침 최대) 결과 제시 | • 0건 체감 감소<br>• “맞춘 조건만” 안내하고 결과 제시 |
| **AI 파일조회 검색 범위 확장 (업무 문맥)** | - | • 티켓: fileName + title/content/purpose/requirement + 참여자 닉/이메일<br>• 채팅: fileName + 방이름 + 업로더 닉네임 | • 파일명 몰라도 “업무 문맥”으로 검색 가능 |
| **AI 파일조회 ACL (다운로드/뷰 보안)** | • aiFileApi.downloadFile로 다운로드 | • AIFileController 권한 체크 후 view/download 제공 | • 검색/다운로드 동일 레이어에서 통제 (권한 누수 방지) |
| **AI 비서함 통합 UI (모달/위젯)** | • AIAssistantModal: 채팅(좌) + 방목록/연락처/그룹생성/뮤트/프리뷰(우)<br>• 업무요청서 버튼으로 AIChatWidget 전환 | - | • 채팅↔업무티켓↔파일조회 단일 진입 UX |
| **(보조) AI 채팅 가드/제재 + 파일첨부** | • 10초 2회 경고/60초 강제 ON<br>• Drag&Drop 첨부 | • 금칙어 감지 + 자동 AI 정제 ON<br>• multipart 첨부 저장 후 WS 브로드캐스트 | • 채팅 사고 방지 + 실시간성 유지 |


---



# 플로우 차트
![Image](https://github.com/user-attachments/assets/c40cf959-e03d-4b16-a38f-3feb201a6f47)


## 🛠 트러블 슈팅 (AI · RAG Search Flow)

본 항목은 RAG(Retrieval-Augmented Generation) 기반 검색 및  
AI 응답 생성 흐름을 기준으로 발생한 문제와 해결 과정을 정리한 문서입니다.

---

### 1️⃣ 검색 요청은 정상이나 결과가 반환되지 않는 문제

**문제 현상**  
- 검색 API는 정상 응답
- 검색 결과가 0건으로 반환됨

**원인 분석**  
- 검색 대상 문서가 임베딩(Vector) 생성 이전 상태
- 플로우 상 `Query → Retriever → Vector Store` 단계에서  
  검색 대상 조건 불일치

**해결 방법**  
- 검색 대상 문서를 `indexed=true` 상태로 명확히 분리
- 인덱싱 완료 문서만 Retriever가 조회하도록 제한

**결과**  
- 검색 결과 정상 반환

---

### 2️⃣ 검색 결과는 존재하지만 관련도가 낮은 문제

**문제 현상**  
- 검색 결과는 반환되나 질문과 관련성이 낮음

**원인 분석**  
- Query 임베딩 품질이 낮거나 질문이 모호함
- 플로우 상 TopK 값이 작아 핵심 문서 누락

**해결 방법**  
- Query Rewriting을 통해 검색 질의 명확화
- TopK 확장 후 재정렬(Rerank) 적용

**결과**  
- 검색 정확도 및 문서 적합도 향상

---

### 3️⃣ 검색 결과가 매 요청마다 달라지는 문제

**문제 현상**  
- 동일한 질문임에도 검색 결과 순서가 변함

**원인 분석**  
- Vector 유사도 점수 동률 시 정렬 기준 부재
- 비결정적 검색 결과가 LLM 입력에 영향

**해결 방법**  
- 점수 동률 시 문서 ID 기준 정렬 적용
- Retriever 결과를 deterministic 하게 고정

**결과**  
- 재현 가능한 검색 결과 확보

---

### 4️⃣ 검색 결과가 AI 응답에 제대로 반영되지 않는 문제

**문제 현상**  
- 검색 결과는 있으나 AI 응답이 이를 충분히 반영하지 않음

**원인 분석**  
- 플로우 상 `Retriever → Context Builder → LLM` 단계에서  
  컨텍스트 길이 제한으로 일부 문서가 제외됨

**해결 방법**  
- 검색 결과를 중요도 기준으로 선별
- 컨텍스트 구성 규칙을 명시적으로 정의

**결과**  
- 검색 기반 AI 응답 일관성 확보

---

### 5️⃣ AI가 문서에 없는 내용을 생성하는 문제

**문제 현상**  
- 검색된 문서에 없는 내용을 AI가 단정적으로 응답

**원인 분석**  
- 근거 부족 상태에서도 응답 생성을 허용
- “모르면 모른다” 정책 부재

**해결 방법**  
- 최소 인용 기준 도입
- 근거 부족 시 추가 질문 또는 응답 제한

**결과**  
- AI 환각(Hallucination) 감소
- 신뢰 가능한 응답 품질 확보

---

### 6️⃣ 권한 없는 문서가 검색 결과에 포함되는 문제

**문제 현상**  
- 사용자 권한 범위를 벗어난 문서가 검색됨

**원인 분석**  
- Vector 검색 단계에서 권한 필터 누락
- 인덱스에 ACL 메타데이터 미반영

**해결 방법**  
- 문서/Chunk 단위로 권한 메타데이터 저장
- Retriever 단계에서 ACL 필터 선적용

**결과**  
- 검색 및 AI 응답 단계에서 정보 노출 차단

---

### 7️⃣ 검색·AI 응답 성능이 점진적으로 저하되는 문제

**문제 현상**  
- 문서 수 증가에 따라 검색 지연 발생
- AI 응답 생성 시간이 증가

**원인 분석**  
- 검색 범위 과도
- 불필요한 반복 검색 및 LLM 호출

**해결 방법**  
- 메타데이터 기반 검색 범위 축소
- Query Embedding 캐싱 적용

**결과**  
- 응답 속도 개선
- 시스템 비용 안정화

---

