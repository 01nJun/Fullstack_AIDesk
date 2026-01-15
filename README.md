
<img width="1536" height="418" alt="Image" src="https://github.com/user-attachments/assets/b2e0a60a-fe9f-4442-b7ea-6b8de6af1f32" />

# 🧩 태스크플로우 (TaskFlow)

## 📌 프로젝트 소개

태스크플로우(TaskFlow)는 사내 업무 대화와 요청을 AI가 분석하여  
업무 정리·기록·관리를 자동화하는 협업 지원 서비스입니다.  
부정확한 소통으로 발생하는 업무 지연과 갈등을 줄이고,  
보다 명확하고 효율적인 업무 요청 환경을 제공하는 것을 목표로 합니다.

---

## 📖 개요

태스크플로우(TaskFlow)는 직장 내 다양한 업무 대화와 요청을 기반으로  
AI가 핵심 정보를 추출해 업무 요청 초안을 자동으로 생성하는 협업 서비스입니다.

사용자의 자연어를 분석하여  
업무 제목, 내용, 담당자, 기한 등의 정보를 자동으로 정리하며,  
필수 정보가 누락된 경우 AI의 역질문을 통해 요청의 완성도를 높입니다.

또한 감정 필터링 기능을 통해 공격적이거나 불필요한 표현을 완화하여  
팀 내 커뮤니케이션에서 발생할 수 있는 갈등을 예방합니다.  
회의 음성 파일을 텍스트로 변환·요약해  
즉시 실행 가능한 회의록을 PDF 형태로 자동 제공함으로써  
업무 기록과 공유 과정을 효율화합니다.

---

# 📆 개발 기간
25.12.17(수) ~ 26.01.15(목)


---

## 🤝 팀원

|                               (팀장) [김민식](https://github.com/minsik321)                               |                                 [한정연](https://github.com/DOT-SOY)                                  |                                        [박태오](https://github.com/teomichaelpark-glitch)                                         |                                 [오인준](https://github.com/01nJun)                                 |                 [박건영](https://github.com/keonyeong4550/one-of-team-project-20251217-20260116)                  |
| :-------------------------------------------------------------------------------------------------------: | :---------------------------------------------------------------------------------------------------: | :-------------------------------------------------------------------------------------------------------------------------------: | :-------------------------------------------------------------------------------------------------: | :---------------------------------------------------------------------------------------------------------------: |
| [<img src="https://avatars.githubusercontent.com/minsik321" width="160" />](https://github.com/minsik321) | [<img src="https://avatars.githubusercontent.com/DOT-SOY" width="160" />](https://github.com/DOT-SOY) | [<img src="https://avatars.githubusercontent.com/teomichaelpark-glitch" width="160" />](https://github.com/teomichaelpark-glitch) | [<img src="https://avatars.githubusercontent.com/01nJun" width="160" />](https://github.com/01nJun) | [<img src="https://avatars.githubusercontent.com/keonyeong4550" width="160" />](https://github.com/keonyeong4550) |

## 🚀 주요 기능 (Key Features)

### 1. 보안 인증 및 계정 관리
- JWT 기반 일반/소셜 로그인 및 임베딩 벡터 방식의 얼굴 인식 생체 인증
- Redis 활용 RTR(Refresh Token Rotation) 및 로그인 실패 시 계정 자동 잠금(Security)
- 관리자 전용 사용자 가입 승인 및 권한 관리 시스템

### 2. AI 업무 티켓 코파일럿 (Ticket Copilot)
- RAG(부서 지식 주입) 및 인터뷰 로직을 통한 업무 요청서(Ticket) 자동 작성
- AI 기반 담당자 자동 배정(Routing) 및 대화형 시나리오 기반 템플릿 지원
- AI 비서 통합 위젯/모달 UI를 통한 유기적인 업무 전환

### 3. 실시간 소통 및 AI 에티켓 가드
- WebSocket/STOMP 기반 실시간 1:1·그룹 채팅
- 대용량 메시지 최적화(가상 스크롤)
- Ollama AI 연동 실시간 메시지 필터링 및 부적절한 대화 자동 정제 가드

### 4. 지능형 파일 검색 및 보안 (Retrieval)
- 자연어 질의 기반 하이브리드 파일 검색
- 검색 결과 최적화 전략(0건 UX 개선)
- 파일 접근 권한 관리(ACL) 및 티켓/채팅 내 첨부파일 통합 보안 관리

### 5. AI 회의록 자동화 시스템
- OpenAI Whisper 기반 음성(MP3) 데이터 텍스트 변환(STT)
- AI 요약 및 텍스트 정규화 엔진을 통한 구조화된 회의록 PDF 자동 생성

### 6. 업무 협업 및 관리 도구
- 업무 티켓 CRUD, 필터링, 중요 업무 핀(Pin) 고정 및 전역 상태 동기화
- 공지사항 게시판(QueryDSL 검색) 및 계층형 댓글 시스템
- 사용자별 통계 및 실시간 업무 현황을 제공하는 통합 대시보드

---

## ‍💻 팀원별 담당 기능

### 🧭 김민식 (팀장)
- **인증 및 보안**: JWT 기반 보안 처리, 카카오 소셜 로그인 통합, 얼굴 인식 로그인 시스템 설계  
  (MariaDB / PostgreSQL DB 분리)
- **세션 및 계정 관리**: Redis 기반 Refresh Token Rotation(RTR) 정책 및 로그인 실패 계정 잠금 기능 구현
- **백오피스**: 관리자 페이지(승인, 검색, 페이징), 업무 관리 시스템(필터링, 중요업무), 파일함 기능 개발
- **프론트엔드 아키텍처**: Redux Toolkit 기반 전역 상태 관리(Pin) 및 사용자 맞춤형 메인 대시보드 구현

---

### 💬 한정연
- **실시간 채팅**: WebSocket, SockJS, STOMP 기반 채팅 도메인 설계 및 서버 인프라 구축
- **AI 연동**: Spring WebClient 기반 Ollama AI 서버 비동기 통신 및 실시간 메시지 필터링 로직 구현
- **채팅 최적화**: 최신 메시지 기준 역방향 무한 스크롤 및 페이지네이션 처리
- **업무 시스템**: 티켓 관리 시스템 백엔드 CRUD API 초기 설계 및 단건 조회 연동

---

### 🤖 박태오
- **AI 티켓 코파일럿**: AI 업무 티켓 라우팅, 담당자 DB 검증, RAG + JSON 인터뷰 기반 대화형 티켓 자동 작성 플로우 구현
- **지능형 파일 검색**: 자연어 질의 파싱 기반 파일 조회, 검색 전략 고도화(0건 UX 개선), 파일 접근 권한(ACL) 보안 통합
- **통합 인터페이스**: 채팅·업무 티켓·파일 조회가 결합된 AI 비서함 통합 모달/위젯 UI 개발
- **보안 및 편의**: AI 채팅 가드 및 제재 시스템, 드래그 앤 드롭 파일 첨부 기능 구현

---

### 🧩 오인준
- **커뮤니티 시스템**: QueryDSL 기반 공지사항 목록 조회(검색, 필터, 페이징) 및 카테고리별 시각화
- **댓글 시스템**: 본인 확인 및 권한 로직이 포함된 댓글/대댓글 CRUD 시스템 개발
- **음성 인식**: Spring AI + OpenAI Whisper 연동 회의 음성(MP3) 텍스트 변환(STT) 기능 구현

---

### 📄 박건영
- **문서 자동화**: Ollama(Qwen) 기반 회의 내용 분석 및 iText/PDFBox 활용 구조화된 회의록 PDF 자동 생성
- **텍스트 전처리**: 텍스트 정규화(Normalization) 엔진 및 불용어 제거(Stopword Filtering) 파이프라인 구축
- **인프라 최적화**: 대용량 음성 파일 업로드 서버 설정 및 변환 작업 비동기 로딩 상태 관리


---





### 📚사용 스택

<div>
<img src="https://img.shields.io/badge/ollama-%23000000.svg?style=for-the-badge&logo=ollama&logoColor=white">
<img src="https://img.shields.io/badge/MariaDB-003545?style=for-the-badge&logo=mariadb&logoColor=white">
<img src="https://img.shields.io/badge/postgres-%23316192.svg?style=for-the-badge&logo=postgresql&logoColor=white">
<img src="https://img.shields.io/badge/github-%23121011.svg?style=for-the-badge&logo=github&logoColor=white">

<img src="https://img.shields.io/badge/react-%2320232a.svg?style=for-the-badge&logo=react&logoColor=%2361DAFB">
<br>
<img src="https://img.shields.io/badge/React_Router-CA4245?style=for-the-badge&logo=react-router&logoColor=white">

<img src="https://img.shields.io/badge/redux-%23593d88.svg?style=for-the-badge&logo=redux&logoColor=white">
<img src="https://img.shields.io/badge/Socket.io-black?style=for-the-badge&logo=socket.io&badgeColor=010101">
<img src="https://img.shields.io/badge/spring-%236DB33F.svg?style=for-the-badge&logo=spring&logoColor=white">
<img src="https://img.shields.io/badge/tailwindcss-%2338B2AC.svg?style=for-the-badge&logo=tailwind-css&logoColor=white">

<br>
<img src="https://img.shields.io/badge/html5-%23E34F26.svg?style=for-the-badge&logo=html5&logoColor=white">

<img src="https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white">
<img src="https://img.shields.io/badge/javascript-%23323330.svg?style=for-the-badge&logo=javascript&logoColor=%23F7DF1E">
<img src="https://img.shields.io/badge/python-3670A0?style=for-the-badge&logo=python&logoColor=ffdd54">
<br/><br/> 
</div>

### 🎨 Frontend
- React 18
- Redux Toolkit (전역 상태 관리)
- Tailwind CSS
- WebSocket (SockJS + STOMP)
- HTML5, JavaScript

---

### ⚙️ Backend
- Spring Boot 3.4.6
- Java 21
- Spring Security + JWT
- WebSocket / STOMP 기반 실시간 통신

---

### 🗄️ Database & Cache
- MariaDB
- PostgreSQL (pgvector)
- Redis (Refresh Token Rotation, 계정 보안)

---

### 🤖 AI
- Spring AI
- OpenAI Whisper (음성 → 텍스트 변환)
- Ollama (Qwen 3:8b)

---


## 🎥 [유튜브 시연영상 링크 바로보기]()

## 📋 [PDF 보기 링크 바로 보기]()

# ERD 구조

<img width="1389" height="964" alt="Image" src="https://github.com/user-attachments/assets/ef31f1f9-6f57-418e-983b-b78f06e0c516" />

# 계층구조

![Image](https://github.com/user-attachments/assets/33966a9e-9a88-49fa-b5da-0da7e751115e)


---

## ⚡ 성능 개선 

### 🔐 Refresh Token 저장소 성능 비교 (12,000건 기준)

| 저장소 | 평균 처리 시간 | 결과 |
|------|--------------|------|
| Redis | **34 ms** | ✅ 가장 빠름 |
| DB | 119 ms | ❌ 상대적으로 느림 |

- Redis가 DB 대비 **약 3.5배 빠른 처리 성능**
- 메모리 기반 접근으로 디스크 I/O 제거
- TTL 기반 자동 만료 지원 → 운영 부담 최소화

> 🔎 **결론**: JWT Refresh Token 관리에는 Redis가 가장 효율적

---

### 💬 채팅 무한 스크롤 성능 최적화 (10,000건 기준)

| 항목 | 적용 전 | 적용 후 |
|----|-------|-------|
| 메시지 로드 방식 | 전체 로드 | 분할 로드 (20개) |
| Total Time | 18,193 ms | **5,134 ms** |
| Scripting Time | 6,805 ms | **282 ms** |
| Rendering Time | 3,304 ms | **47 ms** |
| DOM Nodes | 23,655 | **224** |

- DOM 개수 **약 99% 감소**
- JS 실행 시간 **약 96% 감소**
- 대량 메시지 렌더링 시 발생하던 끊김 현상 제거

> 🔎 **결론**: 무한 스크롤 + 가상 스크롤 적용으로 대규모 채팅에서도 안정적인 UX 확보

---

<details>
<summary><b>📊 성능 테스트 상세 결과 보기</b></summary>

### Redis 성능 테스트 결과
<img width="1045" height="414" alt="Image" src="https://github.com/user-attachments/assets/1e3a78bb-6adf-4577-b782-eabbb4f08580" />

### DB 성능 테스트 결과
<img width="1050" height="356" alt="Image" src="https://github.com/user-attachments/assets/a3ea0a21-d1a4-4246-a635-330a92f616d8" />

### 무한 스크롤 미적용
<img width="939" height="625" alt="Image" src="https://github.com/user-attachments/assets/34a5f5c1-7fb6-4001-8a39-5f02a91d7c92" />

### 무한 스크롤 적용
<img width="940" height="650" alt="Image" src="https://github.com/user-attachments/assets/f9f6fcc1-c0ba-4cb1-87d7-ac56d96f48bb" />

</details>


---

## 구현 기능

### 반응형
![Image](https://github.com/user-attachments/assets/d3c5daf2-54ba-4726-9b58-85943d2b44b1)


## 로그인 & 소셜로그인 & 보안

![Image](https://github.com/user-attachments/assets/8e8fc266-5de4-46aa-8e06-85aac73362bd)

## 얼굴 인식 로그인(임베딩)

![Image](https://github.com/user-attachments/assets/6acc6508-b79d-402f-8205-a4df925c2cf8)

### [플로우차트 및 트러블 슈팅 내역서](https://github.com/01nJun/Fullstack_AIDesk/blob/main/readMe/LogIn·SNS·Security.md)

## Chat·AI

![Image](https://github.com/user-attachments/assets/b47646bc-d3ce-4a71-816c-ee2633445cc7)

### [플로우 차트 및 트러블 슈팅 내역서](https://github.com/01nJun/Fullstack_AIDesk/blob/main/readMe/Chat·AI·Backend.md)

---

## AI·RAG·FileSearch

![Image](https://github.com/user-attachments/assets/68306e79-1fce-4c53-a139-2eb74b4820f5)

### [플로우 차트 및 트러블 슈팅 내역서](https://github.com/01nJun/Fullstack_AIDesk/blob/main/readMe/AI·RAG·FileSearch.md)

---

## 댓글, 대댓글, 수정 및 삭제 (CRUD) 및 STT(음성 to 텍스트)

![Image](https://github.com/user-attachments/assets/12765adb-a50c-448b-a1bf-f964e2b795f1)

### [플로우 차트 및 트러블 슈팅 내역서](https://github.com/01nJun/Fullstack_AIDesk/blob/main/readMe/reply·STT.md)


---

## 회의록 PDF 자동변환

![Image](https://github.com/user-attachments/assets/0fbc3b61-f3a0-4e37-8c45-0fa9031ae369)

### [플로우 차트 및 트러블 슈팅 내역서](https://github.com/01nJun/Fullstack_AIDesk/blob/main/readMe/meetingPDF.md)




<br>



<br>
<br>
<br>



