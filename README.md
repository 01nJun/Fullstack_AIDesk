
<img width="1536" height="418" alt="Image" src="https://github.com/user-attachments/assets/b2e0a60a-fe9f-4442-b7ea-6b8de6af1f32" />

# 태스크플로우

 고객 응대 업무 고도화를 위한 AI 활용한 RPA 자동화 웹 챗봇

---

# 프로젝트 소개
태스크플로우(TaskFlow)는 사내 업무 대화와 요청을 기반으로 AI가 정리·기록·관리를 지원하는 협업 서비스입니다.
직장 내 부정확한 소통으로 발생하는 갈등과 업무 지연 문제를 해결하고자 기획되었습니다.
이를 위해 AI는 사용자의 자연어를 분석하여 업무 제목, 내용, 담당자, 기한을 자동으로 추출해 업무 요청 초안을 생성하고,
필수 정보가 누락된 경우 역질문을 통해 요청의 완성도를 높이고, 감정 필터링 기능으로 팀 내 불필요한 갈등을 예방합니다.
또한 회의 음성 파일을 텍스트로 변환·요약하여 즉시 실행 가능한 회의록을 PDF 형태로 자동 제공합니다.

---

# 개발 기간
25.12.19(금) ~ 26.01.16(금)


---

## 팀원


#### TOP  


|                               (팀장) [김민식](#팀장-김민식)                               |                                 [한정연](#한정연)                                  |                                        [박태오](#박태오)                                         |                                 [오인준](#오인준)                                 |                 [박건영](#박건영)                  |
| :-------------------------------------------------------------------------------------------------------: | :---------------------------------------------------------------------------------------------------: | :-------------------------------------------------------------------------------------------------------------------------------: | :-------------------------------------------------------------------------------------------------: | :---------------------------------------------------------------------------------------------------------------: |
| [<img src="https://avatars.githubusercontent.com/minsik321" width="160" />](https://github.com/minsik321) | [<img src="https://avatars.githubusercontent.com/DOT-SOY" width="160" />](https://github.com/DOT-SOY) | [<img src="https://avatars.githubusercontent.com/teomichaelpark-glitch" width="160" />](https://github.com/teomichaelpark-glitch) | [<img src="https://avatars.githubusercontent.com/01nJun" width="160" />](https://github.com/01nJun) | [<img src="https://avatars.githubusercontent.com/keonyeong4550" width="160" />](https://github.com/keonyeong4550) |

[주요 기능 보기](#주요기능)

## 팀장 김민식 

## 🧑‍💻 담당한 기능 요약

- **로그인 및 회원가입**
   
    JWT를 이용한 보안 처리 및 백엔드 Spring Security 사용하여 구현
    
- **소셜 로그인 통합**
    
    카카오 로그인을 JWT 기반 인증으로 통합하고 관리자 승인 정책을 동일하게 적용
    
- **얼굴 인식 로그인**
    
    임베딩 벡터 기반 얼굴 인식 로그인 구현 및 MariaDB + PostgreSQL DB 분리 설계
    
- **Refresh Token Rotation (RTR)**
    
    Redis를 이용한 Refresh Token Rotation 정책 구현 및 화이트리스트 기반 안전한 토큰 재발급
    
- **로그인 실패 계정 잠금**
    
    Redis를 이용한 일정 횟수 이상 로그인 실패 시 해당 계정이 일정 시간 로그인이 잠기는 기능 구현
    
- **관리자 페이지**
    
    승인 처리, 삭제 처리, 검색 및 필터 처리, 페이징 처리 기능 구현
    
- **업무 관리 시스템**
    
    업무 요청서 목록, 필터링, 중요업무(Pin) 기능 구현
    
- **파일 관리 시스템**
    
    파일 업로드/다운로드, 파일함 기능 구현
    
- **메인 대시보드**
    
    중요 업무, 읽지 않은 업무 등 사용자 정보 통합 대시보드 구현
    
- **전역 상태 관리**
    
    Redux Toolkit을 활용한 중요업무(Pin) 전역 상태 관리 및 사용자별 동기화
    

---

## 🚀 주요 기능

1. JWT 기반 로그인 및 사용자 인증
2. 소셜 로그인 연동
3. 관리자 페이지 구현
4. 업무 관리 시스템 개발
5. 파일 업로드 / 다운로드 기능
6. 사용자 중심 메인 대시보드
7. 전역 상태 관리 적용

---

## 👥 구현 기능 & 역할

| 구현 기능 | Front-End 담당 | Back-End 담당 | 설계 및 특징 |
| --- | --- | --- | --- |
| **로그인 및 회원가입** | • 소셜 로그인 API 연동 및 일반 회원가입 처리• JWT 토큰을 쿠키에 저장하여 보안성과 사용자 편의성 향상• Access Token 자동 재발급 로직 구현 (Subscriber 패턴) | • JWT 토큰 생성 및 Spring Security 보안 설정• 데이터베이스 및 서버 통신 보안 강화• Refresh Token Rotation (RTR) 정책 구현 | • JWT 토큰을 Authorization 헤더에 포함하여 로그인 필수 페이지 인증 검증• Access Token 유효기간이 임계값 이하일 경우 Refresh Token을 통한 Access Token 재발급 |
| **소셜 로그인 통합** | • 카카오 로그인 API 연동• 소셜 로그인 사용자 부서 정보 입력 유도• 승인 대기 상태 안내 메시지 표시 | • 카카오 API 호출 및 사용자 정보 조회• 소셜 회원 자동 생성• 일반 로그인과 동일한 JWT 토큰 발급 프로세스 적용 | • 카카오 로그인도 JWT 기반 인증 구조로 통합• 일반 로그인과 동일한 관리자 승인 프로세스 적용• 최초 가입 시 부서 정보 입력 유도 |
| **얼굴 인식 로그인** | • 웹캠을 통한 얼굴 촬영 UI• 얼굴 인식 설정 페이지 구현 | • Python AI 서버 연동 (WebClient)• 얼굴 임베딩 벡터 추출• PostgreSQL 벡터 검색 (pgvector)• Spring Security Authentication Filter 통합 | • 단순 이미지 비교가 아닌 임베딩 벡터 기반 인증• MariaDB(일반 데이터) + PostgreSQL(벡터 데이터) DB 분리 설계• L2 거리 기반 유사도 검색으로 사용자 식별 |
| **업무 관리 시스템** | • 업무 현황 리스트 (전체/받은/보낸)• 검색 및 필터 기능• 중요업무(Pin) 토글 기능• 페이지네이션 처리 | • 업무(Ticket) 목록 조회 API• 동적 쿼리 기반 검색 및 필터링• 중요업무(Pin) 관리 API• 읽음/안읽음 상태 관리 | • 업무 요청서 작성 시 파일 첨부 가능• 중요도, 마감일 기준 필터링• 서버 기준 읽음/안읽음 상태 관리• Redux를 통한 중요업무 전역 상태 관리 |
| **파일 관리 시스템** | • 파일 업로드 UI (이미지 미리보기)• 파일함 페이지 (전체/발신/수신)• 파일 다운로드 및 삭제 기능 | • 파일 업로드/다운로드 API• UUID 기반 파일명 저장• 파일 삭제 시 DB 및 파일 시스템 동기화 | • 이미지 파일은 미리보기 제공• Blob 형태로 파일 다운로드 처리• 파일명 인코딩 처리 (한글 파일명 지원) |
| **메인 대시보드** | • 중요 업무 개수 표시• 읽지 않은 업무 개수 표시• 최근 받은 업무 3건 표시• 최근 공지사항 3건 표시 | • 사용자별 통계 데이터 조회 API• 읽지 않은 업무 개수 조회 API• 최근 업무/공지 조회 API | • 실시간 데이터 동기화• 클릭 시 해당 페이지로 이동 및 필터링• 중요업무 슬라이드 Drawer 연동 |
| **전역 상태 관리 (Pin)** | • Redux Toolkit을 활용한 Pin 상태 관리• useCustomPin 커스텀 훅 구현• 로그아웃 시 상태 초기화• 사용자 변경 시 자동 동기화 | • Pin 토글 API• 사용자별 Pin 목록 조회 API | • 전역 상태로 모든 페이지에서 접근 가능• 사용자 단위로 Pin 목록 분리 관리• 로그인 사용자 변경 시 자동으로 해당 사용자 Pin 목록 로드 |
| **Refresh Token Rotation (RTR)** | • Access Token 만료 시 자동 재발급 요청• Subscriber 패턴을 통한 동시 요청 처리• 화이트리스트 기반 안전한 토큰 재발급 | • Refresh Token 회전 로직 구현• Redis 기반 Refresh Token 저장 및 관리• Token Family 개념을 통한 세션 관리• Replay 공격 방지 및 used 플래그 관리 | • Refresh Token은 1회성 사용 원칙 (RTR 정책)• 동시 요청 시 Race Condition 방지 (Subscriber 패턴)• Token Family 단위로 세션 무효화 가능 |
| **로그인 실패 계정 잠금** | • 로그인 실패 횟수 안내 메시지• 계정 잠금 시 남은 시간 표시• 잠금 상태 안내 | • Redis를 이용한 실패 횟수 관리• 5회 실패 시 30분 자동 잠금• TTL 기반 자동 잠금 해제• 로그인 성공 시 실패 횟수 초기화 | • Redis TTL을 활용한 자동 잠금 해제• 실패 횟수 실시간 추적 및 관리• 잠금 상태 조회 및 남은 시간 계산• 보안 강화를 위한 자동 계정 보호 |
| **관리자 페이지** | • 승인 대기 사용자 목록 표시• 전체 직원 목록 표시• 검색 및 필터 UI• 승인/삭제 버튼• 페이지네이션 UI | • 승인 대기 사용자 조회 API• 전체 직원 조회 API• 사용자 승인 처리 API• 사용자 삭제 처리 API (Soft Delete)• 검색 및 필터 로직• 페이지네이션 처리 | • 관리자 권한 기반 접근 제어• 이메일, 부서 기준 검색 및 필터링• 승인/삭제 처리 시 DB 상태 변경• 페이징을 통한 대량 데이터 효율적 관리 |


<br><br>
## 🛡️ 고도화된 JWT 인증 & Redis 세션 보안 아키텍처

### 1. 기술적 요약 (Core Identity)

> "NIST SP 800-63B 및 OWASP 가이드라인을 준수하여 최소 길이 중심의 패스워드 정책을 수립하고, Redis 화이트리스트 기반의 RTR(Refresh Token Rotation) 구조를 통해 분산 환경에서도 실시간 위협 탐지가 가능한 고성능 인증 시스템을 구현했습니다."
> 

---

### 2. 상세 보안 로직 및 설계 (Deep Dive)

**① 화이트리스트 기반의 '서버 기억형' 세션 관리**

- **화이트리스트 검증:** JWT의 한계인 '제어권 상실'을 보완하기 위해, 발급된 유효 Refresh Token을 Redis에서 **화이트리스트**로 관리합니다. 서버는 서명 유효성뿐만 아니라 "서버가 현재 승인한 토큰인가?"를 대조하여 즉각적인 세션 무효화를 수행합니다.
- **Token Family & 기기 격리:** `family_id` 클레임을 도입해 사용자 ID가 아닌 '로그인 세션 그룹' 단위로 관리합니다. 이를 통해 다중 기기 접속 환경에서 **특정 기기의 의심 세션만 정밀 타격하여 차단**함으로써 정상 사용자의 UX를 보호합니다.
- **Context Binding:** 토큰에 `User-Agent Hash`와 `IP 힌트`를 결합하여, 화이트리스트에 있더라도 접속 환경이 급격히 변할 경우 즉시 검증을 요청합니다.

**② RTR(Refresh Token Rotation) 및 재사용 감지 (Replay Detection)**

- **일회성 토큰 원칙:** 토큰 재발급 시마다 새로운 Refresh Token을 발행하여 탈취된 토큰의 생명주기를 극도로 단축했습니다.
- **실시간 재사용 탐지:** 이미 사용된(폐기된) 토큰이 유입되면 이를 **Replay Attack**으로 즉시 판단합니다. 이 경우 해당 토큰만 막는 것이 아니라, 관련된 **Token Family 전체를 화이트리스트에서 삭제**하여 잠재적 피해를 차단합니다.

**③ 인프라 성능 및 운영 최적화**

- **무상태(Stateless) 확장성:** 공유 저장소인 Redis를 활용하여 서버 증설(Scale-out) 시에도 세션 불일치 문제 없이 안정적인 인증 서비스를 유지합니다.
- **TTL 기반 자동 관리:** 로그인 실패 기록 및 만료 세션 정보를 Redis의 **TTL(Time-To-Live)** 기능을 통해 관리합니다. 별도의 DB 배치 작업 없이도 데이터가 자동 정리되어 메모리 누수를 방지하고 운영 부담을 줄였습니다.
- **성능 고도화:** Redis **Connection Pool** 활성화 및 **Pipelining(배치 명령)**을 활용해 네트워크 왕복 시간을 최소화했으며, RDBMS 대비 인증 처리 속도를 획기적으로 개선했습니다.

**④ 보안 위협 선제적 대응**

- **Brute-force 방어:** Redis의 `Atomic Increment`를 활용해 로그인 5회 실패 시 30분을 자동 차단합니다. 차단 상태에서 재시도 시 남은 제한 시간을 실시간으로 반환하여 사용자 가이드를 제공합니다.
- **토큰 혼용 공격 방지:** `tokenType` 클레임을 명시하여 Access/Refresh 토큰이 본래 용도와 다르게 사용되는 것을 원천 차단했습니다.
- **XSS 및 CSRF 대응:** Refresh Token은 브라우저 JS가 접근할 수 없는 `HttpOnly`, `Secure`, `SameSite` 쿠키로만 전달하여 토큰 탈취 가능성을 최소화했습니다.

---

### 3. 글로벌 보안 표준 준수 (Standard Alignment)

- **NIST SP 800-63B 준수:** 사용자 경험을 저해하는 과도한 특수문자 조합 강요 대신, **충분한 최소 길이(8자 이상)** 확보와 FE/BE 이중 유효성 검증을 통해 실질적인 보안성을 강화했습니다.
- **OWASP ASVS 대응:** 전송 계층 보안뿐만 아니라 세션 관리(Session Management) 항목의 권고사항인 '강력한 세션 파기' 및 '기기 컨텍스트 검증'을 아키텍처 레벨에서 구현했습니다.

---

### 🚀 프로젝트의 기술적 차별점 (최종 정리)

1. **정밀한 세션 통제:** Token Family 전략으로 전체 로그아웃 없이 위협 기기만 선택적 차단 가능.
2. **능동적 공격 대응:** RTR과 화이트리스트를 결합하여 토큰 탈취 시도를 실시간으로 탐지하고 즉각 무효화.
3. **고성능·저비용 운영:** Redis 특성을 100% 활용하여 성능과 운영 편의성(자동 삭제 등)을 동시에 확보.



### [▲▲▲TOP▲▲▲](#TOP)

---

<br>
<br>

## 한정연

# 🧑‍💻 담당 기능 요약 (Chat · AI · Ticket Back-End)

- **업무 관리 시스템 백엔드 CRUD**
    - 업무 요청서의 생성 / 조회 / 수정 / 삭제 API 초기 구현
    - 이후 업무 요청서 단건 조회 프론트 연동
- **채팅 도메인 설계**
    - ChatRoom / ChatMessage / Participant 설계
    - 사용자 기준 1:1 / 그룹 채팅 확장 가능한 구조
- **WebSocket 실시간 통신**
    - SockJS + STOMP 기반 실시간 채팅 구현
    - JWT 기반 인증이 적용된 WebSocket 통신 구현
- **채팅 무한 스크롤**
    - 최신 메시지 기준 역방향 로딩 방식
    - 페이지 단위 조회 + 성능 최적화 적용
- **AI 메시지 필터링**
    - Ollama API 연동을 통한 메시지 필터링
    - On / Off 토글 가능하도록 사용자 설정 구조로 구현
- **WebClient 기반 AI 비동기 처리**
    - Spring WebClient로 Ollama API 비동기 호출
    - 채팅 흐름을 막지 않는 논블로킹 구조 적용
- **Ollama 서버 구축**
    - 로컬 Ollama 서버 구성 및 연동
    - 내부 AI 필터링용 모델 운영 환경 구성

---

# 🚀 주요 기능

1. WebSocket 기반 실시간 채팅
2. JWT 인증이 적용된 STOMP 메시지 통신
3. 채팅 메시지 무한 스크롤
4. Ollama 연동 AI 메시지 필터링
5. Ticket CRUD 및 단건 조회 API
6. 비동기 AI 처리 기반 사용자 경험 개선

---

# 👥 구현 기능 & 역할

| 구현 기능 | Front-End 담당 | Back-End 담당 | 설계 및 특징 |
| --- | --- | --- | --- |
| **티켓 CRUD** | • 티켓 목록 / 상세 UI• 상태별 필터링 UI | • 티켓 생성 / 조회 / 수정 / 삭제 API• 단건 조회 로직 분리 | • 업무 요청 단위 도메인 설계• 채팅과 연계 가능한 구조 |
| **채팅 도메인 설계** | • 채팅 UI 구성• 메시지 렌더링 | • ChatRoom / ChatMessage 엔티티 설계• 참여자 기반 권한 구조 | • 1:1 / 그룹 확장 고려한 구조• 메시지 단위 저장 |
| **WebSocket 실시간 통신** | • SockJS + STOMP 연결• 메시지 송수신 처리 | • WebSocketConfig 구성• STOMP 엔드포인트 설계 | • HTTP + 메시지 레벨 이중 인증 구조 |
| **JWT 인증 WebSocket** | • 토큰 포함 연결 요청 | • JWTFilter + ChannelInterceptor 적용 | • SockJS / WebSocket / STOMP 단계별 인증 |
| **채팅 무한 스크롤** | • 스크롤 이벤트 처리• 이전 메시지 로딩 | • 페이지 기반 메시지 조회 API | • 성능 최적화• DOM 최소화 |
| **AI 메시지 필터링** | • 필터 On/Off UI | • Ollama API 호출• 메시지 전처리 | • 사용자 설정 기반 필터링• 논블로킹 처리 |
| **Ollama 연동** | • 필터 결과 표시 | • WebClient 비동기 호출 | • 채팅 흐름 차단 없는 AI 처리 |

---

# 🔄 채팅 전체 동작 흐름 요약 (WebSocket 인증 포함)

| 단계 | 구간 | 설명 |
| --- | --- | --- |
| 1 | Client → SockJS 연결 | SockJS를 통해 WebSocket 연결 시도 |
| 2 | `/ws/info` | SockJS 사전 요청 → **JWTFilter 인증 수행** |
| 3 | `/ws/websocket` | 실제 WebSocket HTTP 핸드셰이크 |
| 4 | JWTFilter 재실행 | WebSocket도 HTTP 요청이므로 재인증 |
| 5 | STOMP CONNECT | WebSocket 위에서 STOMP 프로토콜 연결 |
| 6 | ChannelInterceptor | STOMP 메시지 레벨 인증 수행 |
| 7 | Principal 설정 | 이후 모든 메시지는 인증 사용자 기준 처리 |

---

# 🧩 WebSocket / SockJS / STOMP 역할 정리

| 구성 요소 | 역할 | 한 줄 설명 |
| --- | --- | --- |
| WebSocket | 통신 | 실제 양방향 실시간 연결 |
| SockJS | 연결 추상화 | WebSocket 미지원 환경 대응 |
| STOMP | 메시지 규약 | 구독 기반 메시지 라우팅 제공 |

---

# ❓ WebSocket만 쓰지 않고 STOMP를 사용하는 이유

### WebSocket 단독 사용 시 한계

| 한계 | 설명 |
| --- | --- |
| 메시지 구조 없음 | 포맷 직접 정의 필요 |
| 라우팅 개념 없음 | 목적지 분기 로직 직접 구현 |
| 구독 개념 없음 | pub/sub 직접 구현 필요 |
| 브로드캐스트 부담 | 다수 전송 로직 직접 작성 |

### STOMP 사용 효과

| 제공 기능 | 효과 |
| --- | --- |
| destination | 메시지 라우팅 단순화 |
| SUBSCRIBE | 구독 기반 통신 |
| 프레임 구조 | 메시지 규칙 명확 |
| 브로커 모델 | 브로드캐스트 자동 처리 |

---

# 🔐 인증을 3단계로 수행하는 이유

| 단계 | 인증 위치 | 이유 |
| --- | --- | --- |
| 1 | `/ws/info` | SockJS 사전 연결 확인 요청 |
| 2 | `/ws/websocket` | 실제 WebSocket 핸드셰이크 |
| 3 | STOMP CONNECT | STOMP는 HTTP Filter를 타지 않음 |

---

# ✅ 여러 번 인증해도 괜찮은 이유

| 이유 | 설명 |
| --- | --- |
| JWT Stateless | 요청마다 검증해도 서버 부담 없음 |
| 요청 분리 | `/info`와 `/websocket`은 서로 다른 요청 |
| 보안 강화 | 중간 단계 우회 차단 |



### [▲▲▲TOP▲▲▲](#TOP)

---

<br>
<br>

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



### [▲▲▲TOP▲▲▲](#TOP)

---
<br>
<br>

## 오인준

# 댓글,수정 및 삭제(CRUD) & STT APP(음성 to 텍스트)

# 🧑‍💻  구현 기능 요약
## 📢 공지사항 관리 시스템

1. 공지사항 목록 조회 (검색·필터·페이징)

&emsp;&emsp; • 공지사항 메뉴에서 전체 목록 조회 가능

 &emsp;&emsp;• 페이징 처리로 페이지당 일정 개수 표시

 &emsp;&emsp;• 카테고리별 필터링 지원 (전체 / 공지사항 / 가이드 / FAQ)

 &emsp;&emsp;• QueryDSL 기반 동적 쿼리로 목록 조회 및 검색 처리
   → BoardServiceImpl.getQuerydsl() 활용

2. 카테고리 구분 및 시각적 표현

&emsp;&emsp; • 카테고리별 색상 구분으로 가독성 강화

&emsp;&emsp; • board.category.eq(category) 조건으로 필터링 처리

3. 제목 기반 검색

&emsp;&emsp; • 제목 키워드 검색 지원

&emsp;&emsp; • list() → search1() 호출 구조로 QueryDSL 동적 검색 수행


# 📄 공지사항 상세 조회 & 댓글 시스템

1 .공지사항 상세 조회 (Read)

&emsp;&emsp; • 게시글 클릭 시 상세 내용 확인

&emsp;&emsp; • 작성자, 작성일, 카테고리 정보 표시

&emsp;&emsp; • @GetMapping(bno) 기반 상세 조회 처리

2. 댓글 기능

&emsp;&emsp; • 댓글 등록 / 수정 / 삭제 / 답글(대댓글) 지원

&emsp;&emsp; • 댓글 등록: @PostMapping("/")

&emsp;&emsp; • 대댓글 처리:

&emsp;&emsp;&emsp;&emsp;     • replyRepository.findById(parentRno)로 부모 댓글 조회

&emsp;&emsp;&emsp;&emsp;     • reply.setParent(parent)로 연관관계 설정

3. 댓글 권한 관리

&emsp;&emsp; • 수정: 본인 댓글만 가능

&emsp;&emsp; • ReplyServiceImpl.modify()에서 권한 체크

&emsp;&emsp; • SecurityContextHolder를 통해 현재 로그인 사용자 닉네임 추출

4. 삭제:

&emsp;&emsp; • 본인 또는 ADMIN 권한 가능

&emsp;&emsp; • ADMIN은 타인의 댓글 삭제 가능 (수정은 불가)

&emsp;&emsp; • checkAdminRole()로 ROLE_ADMIN 여부 확인

# 🔐 공지사항 등록 · 수정 · 삭제 (관리자 전용)

1. 권한 제어

&emsp;&emsp; • ADMIN 권한만 등록 / 수정 / 삭제 가능

&emsp;&emsp; • @PreAuthorize를 통한 보안 처리

2. 프론트엔드 관리자 UI 제어

&emsp;&emsp; • 목록 화면:

&emsp;&emsp;&emsp;&emsp;     • ListComponent.js에서 <br>
&emsp;&emsp;&emsp;&emsp;       loginState?.roleNames?.includes("ADMIN")로 관리자 여부 확인

&emsp;&emsp;&emsp;&emsp;     • 관리자에게만 + 버튼 노출

&emsp;&emsp; • 상세 화면:

&emsp;&emsp;&emsp;&emsp;     • ReadComponent.js에서 <br>
&emsp;&emsp;&emsp;&emsp;       관리자 또는 작성자일 경우에만 수정 버튼 노출

# 🎙️ 회의록(STT) 기능

1. 음성 파일 업로드

&emsp;&emsp; • AI 비서 채팅 화면에서 📜 버튼을 통해 MP3 파일 업로드

&emsp;&emsp; • AIChatWidget.js의 handleAudioUpload()에서 처리

2. Speech-to-Text 변환

&emsp;&emsp; • 업로드된 MP3 파일을 음성 → 텍스트로 변환

&emsp;&emsp; • SpringAI + OpenAI Whisper(STT) 모델 활용

&emsp;&emsp; • Whisper-1 모델 기반 한국어 음성 인식

3. 백엔드 처리 구조

&emsp;&emsp; • sttService.stt(file.getBytes())로 음성 데이터 전달

&emsp;&emsp; • SttServiceImpl에서 OpenAiAudioTranscriptionModel 사용

&emsp;&emsp; • 음성 파일을 ByteArrayResource로 변환 후 STT 요청


### [▲▲▲TOP▲▲▲](#TOP)



---

<br>
<br>

## 박건영

# 회의록 자동 변환 PDF

# 🧑‍💻 담당한 기능 요약
- 회의록 PDF 자동변환 및 생성
Ollama(Qwen 3:8b) 모델을 활용하여 MP3 음성 파일을 분석하고, 구조화된 JSON 데이터를 기반으로 회의록 PDF를 자동 생성
- 대용량 음성 파일 처리 및 UX 최적화
50MB 이상의 대용량 MP3 업로드를 지원하도록 서버 최적화 및 STT 변환 간 로딩 상태(Loading State) 관리 구현
- 지능형 텍스트 전처리 파이프라인
정규화(Normalization)와 불용어 제거(Stopword Filtering)의 2단계 파이프라인을 구축하여 텍스트 품질 향상
- 규칙 기반 텍스트 정제 시스템
외부 설정 파일(normalize-rules.txt, stopwords.txt)을 통한 유연한 정규식 관리 및 타임스탬프/무의미한 문장 자동 제거 로직 구현

# 🚀 주요 기능
1. AI 기반 회의록 자동 생성 및 PDF 변환
2. 대용량 MP3 파일 업로드 및 비동기 처리
3. 텍스트 정규화(Normalization) 시스템
4. 불용어(Stopword) 필터링 및 문장 최적화
5. 결과 파일 자동 첨부 및 다운로드 시스템


# 👥 구현 기능 & 역할

| 구현 기능	| Front-End 담당	| Back-End 담당	| 	설계 및 특징	| 
| --- 	|  --- 	|  --- 	|  --- 	| 
| **회의록 PDF 생성**		|  • 파일 업로드 UI 및 유효성 검사<br>• 진행 상황 로딩(Loading State) 시각화<br>• 결과 PDF 뷰어 및 다운로드 UI		|  • Ollama(Qwen 3:8b) 모델 연동<br>• STT 결과 DTO 매핑<br>• iText/PDFBox 등을 활용한 PDF 생성<br>• 이메일 ➔ 닉네임 변환 로직		|  • 제목, 목적, 상세, 요약, 결론으로 구조화된 JSON 데이터 추출<br>• 표(Table) 형태의 가독성 높은 PDF 레이아웃 설계	| 
| **파일 자동 처리**		|  • 대용량 파일 청크 업로드 처리<br>• 비동기 요청에 따른 UI 상태 관리		| • 50MB 이상 대용량 파일 서버 설정 최적화<br>• 생성된 PDF의 자동 첨부파일 등록 로직<br>• MP3 내 담당자 이메일 자동 추출		| • STT 변환 등 장시간 소요 작업에 대한 사용자 피드백 강화<br>• 티켓 전송 후 파일함 연동 자동화	| 
| **텍스트 정규화**		| (Back-End 처리 결과 표시)	| 	• normalize-rules.txt 로딩 및 파싱<br>• 정규식(Regex) 기반 문자열 치환 엔진 구현	| 	• 정규화 규칙을 외부 파일로 분리하여 유지보수성 향상<br>• 코드 수정 없이 규칙 추가/수정 가능	| 
| **불용어 제거**	| 	(Back-End 처리 결과 표시)	| 	• stopwords.txt 기반 필터링 로직<br>• 타임스탬프/단답형 문장 제거 알고리즘<br>• 문장 압축 및 공백 정리	| 	• 의미 없는 의성어, 존댓말 어미 압축으로 핵심 정보 보존<br>• 라인 단위 분석을 통한 데이터 경량화	| 

<br><br>

# 🛡️ 고도화된 텍스트 전처리 및 AI 파이프라인 아키텍처
### 1. 기술적 요약 (Core Logic)
```
"Ollama(Qwen 3:8b) 모델을 통해 추출된 비정형 데이터를 정교한 2단계 전처리(Normalization
→ Stopword Filtering) 파이프라인을 거쳐 처리함으로써, 단순 요약을 넘어선 '업무·결정 중심'
의 고품질 회의록을 생성합니다."
```
### 2. 상세 로직 및 설계 (Deep Dive)
① AI 모델 기반의 구조화된 데이터 추출 (Structure)
- DTO 기반 PDF 생성: 단순히 텍스트를 나열하는 것이 아니라, AI 모델(Qwen 3:8b)이 생성한 결과물을 DTO(Data Transfer Object)로 매핑합니다. 이를 통해 제목, 목적(Overview), 상세(Details), 요약(ShortSummary), 결론(Conclusion) 등으로 섹션이 명확히 구분된 데이터를 확보합니다.
- 사용자 친화적 데이터 변환: 시스템 식별자인 이메일 주소를 PDF 출력 단계에서 사람이 식별하기 편한 '이름(닉네임)'으로 매핑하여 문서의 가독성을 높였습니다.
② 정규화(Normalization) 단계 - 규칙의 외부화
- 유연한 규칙 관리: 정규화 규칙을 하드코딩하지 않고 normalize-rules.txt 파일로 외부화했습니다. "정규식 => 치환 문자열" 형식을 사용하여, 개발자의 재배포 없이도 텍스트 처리 규칙을 실시간으로 튜닝할 수 있습니다.
③ 불용어 제거(Stopword Filtering) 및 라인 최적화
- 필수 패턴 전처리: 회의 녹음 시 불필요한 00:12 형태의 타임스탬프를 제거하고, 문장 끝의 과도한 존댓말이나 완충 어미를 압축하여 정보 밀도를 높였습니다.
- 문맥 기반 라인 삭제: "네", "알겠습니다", "맞아요" 등 단독으로 쓰일 때 정보가치가 없는 리액션 라인을 통째로 제거하는 로직을 구현했습니다.
- 최종 정제: 연속된 공백과 의미 없는 빈 줄을 제거하여, 결과적으로 텍스트 길이는 줄이되 업무, 결정사항, 기술 정보 등 핵심 내용만 남도록 구현했습니다.




### [▲▲▲TOP▲▲▲](#TOP)


---

# 주요기능

1. JWT 기반 로그인 및 사용자 인증
2. 소셜 로그인 연동
3. 관리자 페이지 구현
4. 업무 관리 시스템 개발
5. 파일 업로드 / 다운로드 기능
6. 사용자 중심 메인 대시보드
7. 전역 상태 관리 적용
8. WebSocket 기반 실시간 채팅
9. JWT 인증이 적용된 STOMP 메시지 통신
10. 채팅 메시지 무한 스크롤
11. Ollama 연동 AI 메시지 필터링
12. Ticket CRUD 및 단건 조회 API
13. 비동기 AI 처리 기반 사용자 경험 개선
14. AI 업무티켓: Routing → 담당자 확정(DB 검증) → RAG+JSON 인터뷰 기반 자동 작성
15. 반복 패턴 템플릿 우선 매칭 + LLM fallback
16. RAG(knowledge_*.json): 임베딩/유사도 기반 부서별 컨텍스트 주입*
17. AI 파일조회: 규칙 기반 파싱(기간/상대/부서/송수신/긴급/키워드) + AI(JSON) 보완
18. AI 파일조회 검색전략: strict → AI 재파싱 → keyword refine → overlap 결과 제시(0건 UX 개선)
19. AI 파일조회 ACL: 티켓/채팅 파일 view/download 권한 검증 통합
20. AI 비서함 통합 UI: 채팅·업무티켓·파일조회 모달/위젯 단일 진입점 및 전환 UX
21. AI 채팅 가드/제재 + 파일첨부(Drag&Drop 포함)
22. 공지사항 목록 조회 (검색·필터·페이징)
23. 공지사항 상세 조회 & 댓글 시스템
24. 카테고리 구분 및 시각적 표현
25. 제목 기반 검색
26. 회의록(STT) 기능
27. AI 기반 회의록 자동 생성 및 PDF 변환
28. 대용량 MP3 파일 업로드 및 비동기 처리
29. 텍스트 정규화(Normalization) 시스템
30. 불용어(Stopword) 필터링 및 문장 최적화
31. 결과 파일 자동 첨부 및 다운로드 시스템

<br>

### 📚사용 스택

<div>
<img src="https://img.shields.io/badge/ollama-%23000000.svg?style=for-the-badge&logo=ollama&logoColor=white">
<img src="https://img.shields.io/badge/chatGPT-74aa9c?style=for-the-badge&logo=openai&logoColor=white">
<img src="https://img.shields.io/badge/MariaDB-003545?style=for-the-badge&logo=mariadb&logoColor=white">
<img src="https://img.shields.io/badge/postgres-%23316192.svg?style=for-the-badge&logo=postgresql&logoColor=white">
<img src="https://img.shields.io/badge/github-%23121011.svg?style=for-the-badge&logo=github&logoColor=white">

<img src="https://img.shields.io/badge/node.js-6DA55F?style=for-the-badge&logo=node.js&logoColor=white">
<img src="https://img.shields.io/badge/NPM-%23CB3837.svg?style=for-the-badge&logo=npm&logoColor=white">
<img src="https://img.shields.io/badge/react-%2320232a.svg?style=for-the-badge&logo=react&logoColor=%2361DAFB">
<br>
<img src="https://img.shields.io/badge/React_Router-CA4245?style=for-the-badge&logo=react-router&logoColor=white">

<img src="https://img.shields.io/badge/redux-%23593d88.svg?style=for-the-badge&logo=redux&logoColor=white">
<img src="https://img.shields.io/badge/Socket.io-black?style=for-the-badge&logo=socket.io&badgeColor=010101">
<img src="https://img.shields.io/badge/spring-%236DB33F.svg?style=for-the-badge&logo=spring&logoColor=white">
<img src="https://img.shields.io/badge/tailwindcss-%2338B2AC.svg?style=for-the-badge&logo=tailwind-css&logoColor=white">
<img src="https://img.shields.io/badge/AWS-%23FF9900.svg?style=for-the-badge&logo=amazon-aws&logoColor=white">

<img src="https://img.shields.io/badge/Gradle-02303A.svg?style=for-the-badge&logo=Gradle&logoColor=white">
<br>
<img src="https://img.shields.io/badge/Postman-FF6C37?style=for-the-badge&logo=postman&logoColor=white">

<img src="https://img.shields.io/badge/kakaotalk-ffcd00.svg?style=for-the-badge&logo=kakaotalk&logoColor=000000">
<img src="https://img.shields.io/badge/css-%23663399.svg?style=for-the-badge&logo=css&logoColor=white">
<img src="https://img.shields.io/badge/html5-%23E34F26.svg?style=for-the-badge&logo=html5&logoColor=white">

<img src="https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white">
<img src="https://img.shields.io/badge/javascript-%23323330.svg?style=for-the-badge&logo=javascript&logoColor=%23F7DF1E">
<img src="https://img.shields.io/badge/python-3670A0?style=for-the-badge&logo=python&logoColor=ffdd54">
 <img src="https://img.shields.io/badge/figma-%23F24E1E.svg?style=for-the-badge&logo=figma&logoColor=white">
<br/><br/> 
</div>

💻 OS : windows

🖥️ Front-end : React

🗄️ back-end : Spring Boot

💾 Database : MariaDB

📡 Server : AWS

---


## 🎥 [유튜브 시연영상 링크 바로보기]

## 📋 [PDF 보기 링크 바로 보기

# ERD 구조

<img width="1389" height="964" alt="Image" src="https://github.com/user-attachments/assets/6d476b6d-aed0-48b7-ac3d-1c6014dc3a30" />

# 계층구조

<img width="4585" height="2368" alt="Image" src="https://github.com/user-attachments/assets/33966a9e-9a88-49fa-b5da-0da7e751115e" />


---

# 성능 비교 분석

---

# 🔥 JWT Refresh Token 저장 성능 분석

JWT 기반 인증 구조에서 Refresh Token 저장소의 성능을 비교했습니다.

테스트 조건은 다음과 같습니다.

- 더미 데이터: **12,000개 Refresh Token**
- 비교 대상:
    1. **Redis** (메모리 기반)
    2. **DB** (RDBMS)
- 성능 기준: **쓰기/읽기 평균 처리 시간**
- 목적: 실시간 인증/재발급 과정에서 가장 효율적인 저장소 선택

---

## 1️⃣ 테스트 환경 및 코드 개요

### Redis

- 메모리 기반 key-value 저장소
- TTL 설정 가능 → 만료 처리 자동
- 연결 풀 사용 → 다중 요청에서도 빠른 처리

```java
// Redis 저장 예시
redisTemplate.opsForValue().set(tokenKey, refreshToken, Duration.ofDays(7));

```

### DB

- RDBMS에 테이블 생성 후 Insert / Select
- 더미 데이터 12,000건 저장 후 조회 테스트

```java
// DB 저장 예시
refreshTokenRepository.save(newRefreshToken(userId, token, expiry));

```

---

## 2️⃣ 성능 결과 (사진 삽입)

### Redis

- 평균 처리 시간: **34 ms**
- **사진1: Redis 성능 테스트 결과**

<img width="1045" height="414" alt="Image" src="https://github.com/user-attachments/assets/7eb99567-76cc-4382-9e49-b454a9ea0da8" />

- 12,000건 처리 시 매우 빠름
- TTL 기반 자동 만료 → 운영 부담 최소화

### DB

- 평균 처리 시간: **119 ms**
- **사진2: DB 성능 테스트 결과**

<img width="1050" height="356" alt="Image" src="https://github.com/user-attachments/assets/de202402-e44a-4173-a99a-749766338dac" />

- 12,000건 처리 시 상대적으로 응답 지연 발생
- TTL 처리 불가 → 별도 배치 작업 필요
- 다중 요청 처리 시 I/O 병목 발생 가능

---

## 3️⃣ 성능 분석 및 이유

| 저장소 | 평균 처리 시간 | 특징 |
| --- | --- | --- |
| Redis | 34 ms | 메모리 기반 → 디스크 I/O 없음 → 빠른 접근, TTL 자동 관리 |
| DB | 119 ms | 디스크 I/O 발생 → 느린 읽기/쓰기, TTL 불가, 배치 필요 |
- Redis가 약 **3.5배 더 빠름**
- DB는 디스크 I/O 때문에 대량 처리 시 지연 발생

---

### 🔹 Refresh Token 용도에서 Redis가 효율적인 이유

1. 실시간 검증과 재발급이 핵심 → 빠른 접근 필요
2. TTL 기반 만료 관리 → Redis 자동 지원
3. 다중 세션/다중 기기 로그인 시 **실시간 조회 및 삭제 가능**
4. DB는 영구 저장용 → 디스크 I/O + 배치 작업 필요 → 운영 부담 증가

---

## 4️⃣ 결론

- Redis는 **실시간 인증/세션 관리에 최적화**된 저장소
- DB는 영구 기록용으로 보조 활용 가능
- 12,000개 테스트 결과, **Redis가 압도적으로 빠름**
- JWT Refresh Token 관리 용도로는 **Redis 사용 권장**



### [▲▲▲TOP▲▲▲](#TOP)
---

# 🔥 무한 스크롤 성능 비교
<br>
<br>
## 성능 테스트 요약 (Infinite Scroll 적용 전/후)

<img width="495" height="348" alt="Image" src="https://github.com/user-attachments/assets/f6746f1c-065c-431e-b608-440c0ec32dee" />

- **테스트 환경**
    - CPU Throttling: **4× slowdown**
    - Network Throttling: **Slow 4G**
    - 채팅 더미데이터 10,000 개로 테스트
- **핵심 결론**
    - 무한스크롤 적용 후 **전체 성능이 압도적으로 개선됨**
    - 특히 **Scripting Time, Rendering Time, DOM 개수**가 극적으로 감소
    - 대량 메시지를 한 번에 렌더링하는 방식은 **브라우저 성능에 매우 치명적이다.**
    - 무한스크롤 적용으로 DOM 개수를 23,655 → 224로 줄였고, Total Time은 약 72%, Scripting Time은 약 96% 감소했다.

<img width="939" height="625" alt="Image" src="https://github.com/user-attachments/assets/72cfbfa1-02b9-4ec2-8590-f682a960a500" />

무한스크롤 미적용의 퍼포먼스 탭 화면

<img width="858" height="84" alt="Image" src="https://github.com/user-attachments/assets/3c934bd5-a6d0-4cef-a7ce-73bca7b013b7" />

FPS 그래프와 CPU 차트가 심각하다.

<img width="940" height="650" alt="Image" src="https://github.com/user-attachments/assets/bd9fa403-4791-43ab-b29f-9f1811bbcfa1" />

무한스크롤을 적용한 퍼포먼스 탭

<img width="873" height="89" alt="Image" src="https://github.com/user-attachments/assets/93684e78-fc48-44c7-822f-6f4a589200c4" />

FPS 그래프와 CPU 차트가 이전보다 훨씬 안정된 것을 볼 수 있다.

## 성능 비교 표 (무한스크롤 적용 전 vs 후)

| 항목 | 무한스크롤 없음 | 무한스크롤 적용 |
| --- | --- | --- |
| 메시지 로드 방식 | 10,000개 한 번에 로드 | 20개씩 분할 로드 |
| Total Time | **18,193 ms** | **5,134 ms** |
| Scripting Time | **6,805 ms** | **282 ms** |
| Rendering Time | **3,304 ms** | **47 ms** |
| DOM Nodes | **23,655개** | **224개** |
| 체감 성능 | 매우 느림 / 끊김 발생 | 부드러움 |

## 수치가 의미하는 것 (핵심 지표 설명)

### Scripting Time

- JavaScript 실행 시간
- 메시지 렌더링, 이벤트 처리, 상태 업데이트 등 포함
- **DOM이 많을수록 JS가 처리해야 할 대상이 기하급수적으로 증가**

무한스크롤 적용 후

`6,805 ms → 282 ms`

- **JS 실행 부담이 거의 사라짐**

---

### Rendering Time

브라우저가 화면을 그리는 데 사용한 시간

포함 작업:

- **Style Calculation**: CSS 규칙 계산
- **Layout (Reflow)**: 요소 크기·위치 계산
- **Recalculate Style**: DOM 변경 시 재계산
- **Pre-Paint / Paint**

무한스크롤 적용 후

`3,304 ms → 47 ms`

- **레이아웃 재계산 비용 대폭 감소**

---

### DOM Nodes

- 브라우저가 관리해야 하는 **HTML 객체 개수**
- DOM 트리가 클수록 성능 비용 증가

<img width="765" height="857" alt="Image" src="https://github.com/user-attachments/assets/4264ad36-19b4-4254-b90e-a7d30a9a3ba5" />

가상 스크롤링 또한 적용되어 DOM 갯수를 일정하게 유지하고 있는 모습이다.

---
[▲▲▲TOP▲▲▲](#TOP)
## 구현 기능

### 반응형
![Image](https://github.com/user-attachments/assets/6e8811fa-6965-4fb5-9da9-aa9102d9ab15)


## 로그인 & 소셜로그인 & 보안

# [영상첨부예정]

### [플로우차트 및 트러블 슈팅 내역서](https://github.com/01nJun/Fullstack_AIDesk/blob/main/readMe/LogIn·SNS·Security.md)

## Chat·AI & UI/UX
### UI/UX
![Image](https://github.com/user-attachments/assets/ce802dad-5f0e-4d38-a2e6-fd6b5ec56afe)

### Chat·AI

# [영상첨부예정]

### [플로우 차트 및 트러블 슈팅 내역서](https://github.com/01nJun/Fullstack_AIDesk/blob/main/readMe/Chat·AI·Backend.md)

---

## AI·RAG·FileSearch

# [영상첨부예정]

### [플로우 차트 및 트러블 슈팅 내역서](https://github.com/01nJun/Fullstack_AIDesk/blob/main/readMe/AI·RAG·FileSearch.md)

---

## 댓글, 대댓글, 수정 및 삭제 (CRUD) 및 STT(음성 to 텍스트)

# [영상첨부예정]

### [플로우 차트 및 트러블 슈팅 내역서](https://github.com/01nJun/Fullstack_AIDesk/blob/main/readMe/reply·STT.md)


---

## 회의록 PDF 자동변환

![Image](https://github.com/user-attachments/assets/eca48042-9d4b-4ea1-898d-24375c3a02d5)


### [플로우 차트 및 트러블 슈팅 내역서](https://github.com/01nJun/Fullstack_AIDesk/blob/main/readMe/meetingPDF.md)


<br>

[▲▲▲TOP▲▲▲](#TOP)

<br>
<br>
<br>
---

