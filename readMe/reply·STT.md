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

---


### 플로우 차트 (댓글, 대댓글, 수정 및 삭제 (CRUD))

<img width="8243" height="7213" alt="Image" src="https://github.com/user-attachments/assets/7e562a86-4afb-4804-85dd-d5f28f337a18" />

### 플로우 차트 (STT APP (음성 to 텍스트))

![Image](https://github.com/user-attachments/assets/41ec4c7c-d543-43c4-b90c-94d049662582)

## 트러블 슈팅
### 1️⃣ 게시판 수정 / 삭제 권한 오류

**문제 현상**  
- 게시글 수정 및 삭제 시 권한 없음 오류 발생

**원인 분석**  
- `boardApi.js`에서는 인증 정보를 **localStorage**에서 조회
- 반면 `loginSlice.js`에서는 인증 정보를 **쿠키 기반**으로 관리
- 인증 저장소 불일치로 인해 토큰을 정상적으로 가져오지 못함

**해결 방법**  
- `boardApi.js`의 인증 정보 조회 방식을 **쿠키 기준으로 수정**

**결과**  
- 게시글 수정 / 삭제 권한 정상 동작 확인

---

### 2️⃣ 댓글 / 대댓글 기능 장애

**문제 현상**  
- 대댓글 추가 이후  
  댓글 **등록 / 수정 / 삭제 기능이 전부 동작하지 않음**

**원인 분석**  
- `setParentReply()` 처리 과정에서  
  `matches multiple source property hierarchies` 오류 발생
- `ModelMapper`가 중첩된 엔티티 구조를 잘못 매핑함

**해결 방법**  
- `ModelMapper` 사용 중단
- DTO 변환 로직을 **Builder 패턴 기반 수동 매핑**으로 전환

**결과**  
- 댓글 / 대댓글 CRUD 기능 정상 복구
- 매핑 로직의 예측 가능성 및 안정성 향상

---

### 3️⃣ STT(Spring AI) 버전 호환성 문제

**문제 현상**  
- STT 기능 구현 과정에서 Spring AI 정상 동작 불가

**원인 분석**  
- 프로젝트 Spring Boot 버전: `3.1.4`
- Spring AI가 해당 버전을 공식 지원하지 않음

**해결 방법**  
- 팀 내 협의 후 Spring Boot 버전을 **3.4.6으로 상향 통합**

**결과**  
- STT 기능 정상 연동
- 라이브러리 호환성 문제 해소

---

### 4️⃣ STT 음성 파일 업로드 중단 이슈

**문제 현상**  
- mp3 파일 업로드 도중 요청이 중단됨

**원인 분석**  
- Axios 설정에 `timeout: 6000` (6초 제한) 적용
- 음성 파일 업로드 시간 초과로 요청 강제 종료

**해결 방법**  
```
timeout: 0 // 시간 제한 제거
```
**결과**

- 대용량 음성 파일 업로드 안정화

- STT 처리 성공률 향상
