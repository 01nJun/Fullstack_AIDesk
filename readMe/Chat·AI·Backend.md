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

# 플로우 차트

### CHAT & 채팅 멤버 찾기
![Image](https://github.com/user-attachments/assets/c2a836bb-a8c4-4963-9460-eb04bff7f2f3)

### CHAT 1:1 대화 생성
![Image](https://github.com/user-attachments/assets/4c0d1644-62fc-455b-982b-50091ad435a0)

### CHAT 그룹 대화 생성
![Image](https://github.com/user-attachments/assets/97b99dca-00ac-4f17-80fa-85700b89f086)

### CHAT 1:1 대화 & 그룹대화
![Image](https://github.com/user-attachments/assets/e9e9493c-8a58-4b9f-a580-6a52070056d6)

### 티켓 단건 READ
![Image](https://github.com/user-attachments/assets/5318c07a-4723-424e-ad56-da50ac366185)

### 웹소켓 연결 및 티켓 발송
![Image](https://github.com/user-attachments/assets/25c450f5-c71d-4ce8-bc68-d05190817180)

### 웹소켓 연결 파이프라인
![Image](https://github.com/user-attachments/assets/3018471c-7e9f-4892-ab5a-6f51e51a5f46)


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

<br>
<br>
<br>

# 무한 스크롤 성능 비교
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
