
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

---


# 플로우 차트

### CHAT & 채팅 멤버 찾기
![Image](https://github.com/user-attachments/assets/9ddc015e-a01f-4416-b614-7f3d0afba936)

### CHAT 1:1 대화 생성
![Image](https://github.com/user-attachments/assets/5e2c5afa-abca-4960-81cf-005cc1b33f05)

### CHAT 그룹 대화 생성
![Image](https://github.com/user-attachments/assets/4fd29435-1e6b-4698-a1ad-3335a8973a58)

### CHAT 1:1 대화 & 그룹대화
![Image](https://github.com/user-attachments/assets/333a0f88-53a3-4103-8388-2a235b26fc88)

### 티켓 목록
![Image](https://github.com/user-attachments/assets/9a6275a8-f951-46a4-9c6c-507ba0c8659c)

### 티켓 단건 READ
![Image](https://github.com/user-attachments/assets/5318c07a-4723-424e-ad56-da50ac366185)

### 웹소켓 연결 및 티켓 발송
![Image](https://github.com/user-attachments/assets/5b3295b8-8ada-4d1f-afdf-d04d161e950f)

### 웹소켓 연결 파이프라인
![Image](https://github.com/user-attachments/assets/e9e1d44c-55be-4716-bfb4-948f81bf4931)

---

## 🛠 트러블 슈팅 (Trouble Shooting)

실제 개발 과정에서 발생한 이슈를 분석하고, 원인과 해결 과정을 정리했습니다.  
단순 오류 해결을 넘어 **설계 개선과 구조적 문제 인식**에 중점을 두었습니다.

---

### 1️⃣ 받은 티켓 조회 권한 오류

**문제 현상**  
- ‘내가 받은 티켓’ 조회 시 정상적으로 데이터가 로드되지 않음

**원인 분석**  
- writer → receiver 순으로 권한을 검증하는 구조였으나  
- writer 검증 실패 시 `IllegalArgumentException`이 발생해도  
  예외 핸들러가 없어 정상 흐름처럼 처리됨 (Status 200 OK)

**해결 방법**  
- `CustomControllerAdvice`에 `IllegalArgumentException` 핸들러 추가

**결과**  
- 권한 오류가 정상적으로 분기 처리되며 조회 로직 안정화

---

### 2️⃣ Pin된 업무 삭제 불가 문제

**문제 현상**  
- Pin 처리된 업무가 삭제되지 않음

**원인 분석**  
- `Pin` 테이블과 `Ticket` 테이블 간 **외래키(FK) 제약 조건**
- Hard Delete 구조에서 Pin 데이터가 남아 삭제 불가

**해결 방향**  
- Soft Delete 방식이 구조적으로 더 적합하다고 판단  
- 삭제 여부 flag 추가 등 **도메인 구조 변경 필요**
- 정책 결정(목록 제외 vs 삭제 상태 표시) 필요로 후속 과제로 이관

---

### 3️⃣ Ticket 생성 시 DB 오류

**에러 로그**
```
Field 'created_at' doesn't have a default value
```
**원인 분석**
```
created_at 컬럼이 NOT NULL
INSERT 시 값 미전달 + DEFAULT 미설정
```
**해결 방법**

- 테스트 과정에서 잘못 추가된 컬럼으로 판단

- ticket 테이블 DROP 후 재생성

**결과**

- INSERT 정상 동작 확인


### 4️⃣ 티켓 상세 조회 구조의 근본적 문제
**문제 인식**

- 프론트에서 ‘보낸 티켓 / 받은 티켓’을 분기 조회

- rowWriter 의존 + try-catch 기반 흐름 제어

**문제점**

- 403 / 400 오류 빈번

- Pin 목록 DTO에는 writer 정보가 없어 구조적 한계 존재

**결론**

- 프론트가 writer/receiver를 판단하는 설계 자체가 문제

- 서버 중심의 단일 조회 구조 필요성 인식

### 5️⃣ Ollama AI 모델 인식 실패
**문제 현상**

- AI 메시지 전송 실패 (모델 없음)

**원인 분석**

- Ollama 서버는 정상이나 모델 목록 비어 있음

- 실행 계정 및 OLLAMA_MODELS 경로 차이로 인한 인식 문제

**해결 방법**

- 서버 콘솔 접근 불가 환경

- Postman을 이용해 원격 POST /api/pull로 모델 재설치

**결과**

- AI 메시지 처리 정상화

### 6️⃣ 모달 pointer-events 이슈
**문제 현상**

- 닫힌 모달이 DOM에 남아 전체 화면 클릭 차단

**원인 분석**

- pointer-events-none만 적용

- 조건부 렌더링 누락

**해결 방법**
```
jsx
코드 복사
if (!isOpen) return null;
```
**결과**

- 모달 닫힘 시 DOM 완전 제거

- UI 클릭 불가 현상 해결

### 7️⃣ WebSocket 채팅 인증 오류
**문제 현상**

- WebSocket 경로 인증 누락

**해결 방법**

- 채팅 WebSocket 경로를 JWT 필터 인증 대상에 포함

**결과**

- 핸드셰이크 및 SockJS /ws/info 정상 인증 확인

### 8️⃣ 채팅 파일 업로드 DB 오류
**에러 메시지**
```
Data truncated for column 'message_type'
```
**원인 분석**

- DB 컬럼 길이 부족

**해결 방법**
```
@Column(length = …) 명시
```
**결과**

- 파일 업로드 메시지 정상 저장

### 9️⃣ Refresh Token 재발급 이슈
**문제 현상**

- 토큰 재발급되었으나 브라우저 반영 실패

**원인 분석**

- 로컬 환경에서 Secure=true + SameSite=None

- addCookie()와 Set-Cookie 혼용

**해결 방법**

- 환경별 쿠키 옵션 분기

- ResponseCookie 단일 사용

**결과**

- 로컬 / 프로덕션 환경 모두 정상 동작

### 🔟 Access Token 재발급 Race Condition
**문제 현상**

- 동시 요청 시 REFRESH_REPLAY_DETECTED 발생

**원인 분석**

- RTR 정책 + 프론트 비동기 요청 충돌

**해결 방법**

- Subscriber 패턴 + 요청 대기열(Queue) 적용

- _retry 플래그 위치 수정

**결과**

- Refresh API 호출 1회로 감소

- 사용자 경험 개선

