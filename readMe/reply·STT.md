### 플로우 차트 (댓글, 대댓글, 수정 및 삭제 (CRUD))

<img width="8243" height="7213" alt="Image" src="https://github.com/user-attachments/assets/efd01ed7-8aa2-4e45-b799-0dec3ccd044e" />

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
