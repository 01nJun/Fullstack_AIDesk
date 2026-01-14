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
<br>
<br>

# 플로우 차트
![Image](https://github.com/user-attachments/assets/c40cf959-e03d-4b16-a38f-3feb201a6f47)

<br>
<br>

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
