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

