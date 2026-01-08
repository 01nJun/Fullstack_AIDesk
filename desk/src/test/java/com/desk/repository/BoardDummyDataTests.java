package com.desk.repository;

import com.desk.domain.Board;
import com.desk.domain.Reply;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@SpringBootTest
public class BoardDummyDataTests {

    @Autowired
    private BoardRepository boardRepository;
    
    @Autowired
    private ReplyRepository replyRepository;

    private final Random random = new Random();

    // 각 부서별 작성자 목록 (email 또는 nickname)
    private final List<String> devWriters = Arrays.asList("김개발", "박이성", "이서진");
    private final List<String> salesWriters = Arrays.asList("최영업", "정고객", "강홍보");
    private final List<String> hrWriters = Arrays.asList("임인사", "오채용", "윤인재");
    private final List<String> designWriters = Arrays.asList("홍디자", "조유아", "신유지");
    private final List<String> planningWriters = Arrays.asList("서기획", "백기진", "남주민");
    private final List<String> financeWriters = Arrays.asList("문회계", "송재무", "유예산");
    
    private final List<List<String>> allWriters = Arrays.asList(
        devWriters, salesWriters, hrWriters, designWriters, planningWriters, financeWriters
    );

    @Test
    @Transactional
    @Commit
    public void insertDummyBoardData() {
        // 공지사항 10개
        createBoardsWithReplies("공지사항", 10, getNoticeTitles(), getNoticeContents());
        
        // 가이드 10개
        createBoardsWithReplies("가이드", 10, getGuideTitles(), getGuideContents());
        
        // FAQ 10개
        createBoardsWithReplies("FAQ", 10, getFaqTitles(), getFaqContents());
        
        System.out.println("✅ 총 30개의 게시글과 댓글 더미데이터 생성 완료!");
    }

    private void createBoardsWithReplies(String category, int count, List<String> titles, List<String> contents) {
        for (int i = 0; i < count; i++) {
            // 랜덤 부서 선택
            List<String> writers = allWriters.get(random.nextInt(allWriters.size()));
            String writer = writers.get(random.nextInt(writers.size()));
            
            Board board = Board.builder()
                    .title(titles.get(i))
                    .content(contents.get(i))
                    .writer(writer)
                    .category(category)
                    .regDate(LocalDateTime.now().minusDays(random.nextInt(30)))
                    .modDate(LocalDateTime.now().minusDays(random.nextInt(30)))
                    .build();

            Board savedBoard = boardRepository.save(board);

            // 댓글 1-3개 추가
            int replyCount = 1 + random.nextInt(3);
            for (int j = 0; j < replyCount; j++) {
                List<String> replyWriters = allWriters.get(random.nextInt(allWriters.size()));
                String replyWriter = replyWriters.get(random.nextInt(replyWriters.size()));
                
                // Reply는 BaseEntity를 상속받아 regDate, modDate가 자동 관리되므로 빌더에서 제외
                Reply reply = Reply.builder()
                        .board(savedBoard)
                        .replyText(getRandomReply())
                        .replyer(replyWriter)
                        .build();
                
                replyRepository.save(reply);
            }
        }
    }

    // 공지사항 제목
    private List<String> getNoticeTitles() {
        return Arrays.asList(
            "2025년 상반기 팀워크 데이 안내",
            "신규 프로젝트 팀 구성 발표",
            "개발 환경 업데이트 및 권한 변경 공지",
            "월례 전체회의 일정 안내 (12월)",
            "연말연시 업무 일정 변경 안내",
            "새로운 협업 도구 도입 안내",
            "개인정보 보호 정책 개정 안내",
            "연차 및 휴가 사용 정책 안내",
            "재택근무 규정 변경 사항 공지",
            "사내 행사 및 이벤트 안내"
        );
    }

    // 공지사항 내용
    private List<String> getNoticeContents() {
        return Arrays.asList(
            "안녕하세요. 인사팀입니다.\n\n2025년 상반기 팀워크 데이 일정을 안내드립니다.\n\n- 일시: 2025년 1월 15일(월) 14:00~18:00\n- 장소: 본사 대회의실 및 야외 공원\n- 참석 대상: 전 직원\n- 준비물: 편한 복장\n\n많은 참석 부탁드립니다.",
            "안녕하세요. 기획팀입니다.\n\n새로운 프로젝트 팀 구성에 대해 공지드립니다.\n\n1. 프로젝트명: 고객 관리 시스템 고도화\n2. 프로젝트 기간: 2024년 1월 ~ 6월\n3. 팀 구성: 개발팀 3명, 디자인팀 2명, 기획팀 2명\n4. 프로젝트 리더: 서기획님\n\n관련 문의사항은 기획팀으로 연락 바랍니다.",
            "개발팀에서 개발 환경 업데이트 및 권한 변경 사항을 공지드립니다.\n\n- 개발 서버 접속 URL 변경\n- Git 브랜치 전략 변경\n- 코드 리뷰 프로세스 개선\n- 배포 자동화 도구 업데이트\n\n상세 내용은 개발팀 위키 페이지를 참고해주세요.",
            "12월 월례 전체회의 일정을 안내드립니다.\n\n- 일시: 12월 28일(목) 15:00\n- 장소: 본사 대회의실\n- 안건: 4분기 실적 보고, 내년 계획 발표\n\n모든 직원 필수 참석입니다. 미참석 시 사전 연락 부탁드립니다.",
            "연말연시 업무 일정 변경 안내입니다.\n\n- 12월 29일(금): 반일 근무\n- 12월 30일(토) ~ 1월 1일(월): 연말연시 휴무\n- 1월 2일(화): 정상 근무 재개\n\n긴급 업무 발생 시 담당자에게 연락 바랍니다.",
            "새로운 협업 도구 도입 안내입니다.\n\n업무 효율 향상을 위해 다음 도구를 도입합니다:\n- 프로젝트 관리: Notion\n- 디자인 협업: Figma\n- 코드 공유: GitHub\n\n교육 세션은 다음 주 화요일 2시에 진행됩니다.",
            "개인정보 보호 정책 개정 안내입니다.\n\n개인정보보호법 개정에 따라 사내 개인정보 처리 방침을 변경했습니다.\n\n주요 변경 사항:\n- 데이터 보관 기간 변경\n- 개인정보 열람 절차 개선\n- 보안 강화 조치\n\n상세 내용은 인사팀으로 문의 바랍니다.",
            "연차 및 휴가 사용 정책 안내입니다.\n\n2025년 연차 사용 규정을 안내드립니다:\n- 입사일 기준 1년 이상 근무자: 15일\n- 3년 이상 근무자: 16일\n- 5년 이상 근무자: 20일\n\n연차 사용은 최소 3일 전 사전 신청 바랍니다.",
            "재택근무 규정 변경 사항을 공지드립니다.\n\n- 재택근무 신청: 주 1회 가능\n- 사전 신청 필수 (최소 1주 전)\n- 출근일 포함 주 5일 근무 유지\n- 재택근무 중에도 정상 근무 시간 준수\n\n상세 문의는 인사팀으로 연락 바랍니다.",
            "사내 행사 및 이벤트 안내입니다.\n\n- 12월 생일자 축하행사: 매월 마지막 금요일\n- 사내 체육대회: 2025년 봄 예정\n- 회식 및 네트워킹: 분기별 1회\n- 자격증 취득 지원금 지급 안내\n\n많은 참여 부탁드립니다."
        );
    }

    // 가이드 제목
    private List<String> getGuideTitles() {
        return Arrays.asList(
            "신입사원 온보딩 가이드",
            "개발 환경 구축 가이드",
            "Git 브랜치 전략 및 워크플로우 가이드",
            "코드 리뷰 작성 가이드",
            "프로젝트 일정 관리 가이드",
            "디자인 시스템 사용 가이드",
            "고객 응대 매뉴얼",
            "경비 정산 및 지출 절차 가이드",
            "회의실 예약 시스템 사용법",
            "비상 연락망 및 업무 연락 가이드"
        );
    }

    // 가이드 내용
    private List<String> getGuideContents() {
        return Arrays.asList(
            "신입사원 온보딩 가이드입니다.\n\n1. 첫 주 체크리스트\n- 개발 환경 설정\n- 사내 시스템 계정 발급\n- 팀 멤버 인사\n- 회사 규정 숙지\n\n2. 필수 교육\n- 보안 교육\n- 협업 도구 사용법\n- 코드 컨벤션\n\n3. 멘토 지정 및 정기 미팅\n\n문의: 인사팀 임인사님",
            "개발 환경 구축 가이드입니다.\n\n필수 설치 항목:\n1. IDE: IntelliJ IDEA 또는 VS Code\n2. JDK 17 이상\n3. Node.js 18 이상\n4. Docker Desktop\n5. Git\n6. DB 클라이언트: DBeaver\n\n상세 설정 방법은 개발팀 위키를 참고하세요.\n\n문의: 개발팀 김개발님",
            "Git 브랜치 전략 및 워크플로우 가이드입니다.\n\n브랜치 전략:\n- main: 운영 배포 브랜치\n- develop: 개발 통합 브랜치\n- feature/기능명: 기능 개발 브랜치\n- hotfix/수정명: 긴급 수정 브랜치\n\n워크플로우:\n1. develop에서 feature 브랜치 생성\n2. 작업 후 커밋 및 푸시\n3. Pull Request 생성\n4. 코드 리뷰 후 머지\n\n문의: 개발팀 박코드님",
            "코드 리뷰 작성 가이드입니다.\n\n리뷰 작성 시 체크리스트:\n1. 코드 가독성 확인\n2. 네이밍 컨벤션 준수 여부\n3. 에러 처리 로직 확인\n4. 테스트 코드 작성 여부\n5. 보안 취약점 검토\n\n리뷰어 태도:\n- 건설적인 피드백 제공\n- 이유를 명확히 설명\n- 긍정적인 어조 유지\n\n문의: 개발팀 이알고님",
            "프로젝트 일정 관리 가이드입니다.\n\n1. 프로젝트 생성\n- Notion에서 새 프로젝트 페이지 생성\n- 목표 및 일정 설정\n\n2. 태스크 관리\n- 할 일 목록 작성\n- 우선순위 설정\n- 담당자 지정\n\n3. 일정 추적\n- 주간 리뷰 미팅\n- 마일스톤 체크\n- 이슈 기록 및 해결\n\n문의: 기획팀 서기획님",
            "디자인 시스템 사용 가이드입니다.\n\n1. 컴포넌트 라이브러리\n- Figma 디자인 시스템 참고\n- 재사용 가능한 컴포넌트 우선 사용\n\n2. 컬러 팔레트\n- 프라이머리: #007BFF\n- 세컨더리: #6C757D\n- 에러: #DC3545\n\n3. 타이포그래피\n- 제목: Noto Sans KR Bold\n- 본문: Noto Sans KR Regular\n\n문의: 디자인팀 홍디자인님",
            "고객 응대 매뉴얼입니다.\n\n응대 원칙:\n1. 친절하고 정확한 답변\n2. 응답 시간 준수 (24시간 이내)\n3. 고객 정보 보안 준수\n\n응대 절차:\n1. 고객 문의 접수\n2. 내용 파악 및 분류\n3. 담당자 배정\n4. 응답 작성 및 검토\n5. 고객 응답 전송\n\n문의: 영업팀 최영업님",
            "경비 정산 및 지출 절차 가이드입니다.\n\n1. 경비 신청\n- 경비 신청서 작성\n- 영수증 첨부\n- 결제 승인 요청\n\n2. 승인 프로세스\n- 팀장 1차 승인\n- 재무팀 최종 승인\n\n3. 정산\n- 월 2회 정산 (15일, 말일)\n- 계좌 이체\n\n문의: 재무팀 문회계님",
            "회의실 예약 시스템 사용법입니다.\n\n1. 예약 방법\n- 사내 포털 > 회의실 예약 메뉴\n- 회의실 선택 및 시간 지정\n- 참석자 초대\n\n2. 예약 규칙\n- 최대 2시간 예약 가능\n- 최소 1시간 전 예약\n- 취소 시 사전 통지 필수\n\n3. 회의실 위치\n- 1층: 대회의실, 중회의실\n- 2층: 소회의실 A, B\n\n문의: 인사팀 윤인재님",
            "비상 연락망 및 업무 연락 가이드입니다.\n\n1. 비상 연락망\n- 총무팀: 010-1234-5678\n- IT 지원: 010-2345-6789\n- 보안팀: 010-3456-7890\n\n2. 업무 연락 방법\n- 긴급: 전화 또는 슬랙 DM\n- 일반: 이메일 또는 슬랙 채널\n- 비공개: 개인 메시지\n\n3. 응답 시간\n- 긴급: 즉시\n- 일반: 업무 시간 내 4시간 이내\n\n문의: 인사팀 오채용님"
        );
    }

    // FAQ 제목
    private List<String> getFaqTitles() {
        return Arrays.asList(
            "연차 신청은 어떻게 하나요?",
            "출퇴근 기록은 어떻게 확인하나요?",
            "개발 서버 접속이 안 됩니다",
            "디자인 리소스는 어디서 받나요?",
            "프로젝트 예산 승인 절차는?",
            "회식비 지급 기준은 어떻게 되나요?",
            "코드 리뷰는 언제까지 해야 하나요?",
            "신규 입사자 교육은 필수인가요?",
            "재택근무 중에도 출근 기록을 해야 하나요?",
            "사내 시스템 비밀번호를 잊어버렸습니다"
        );
    }

    // FAQ 내용
    private List<String> getFaqContents() {
        return Arrays.asList(
            "Q: 연차 신청은 어떻게 하나요?\n\nA: 사내 포털 > 인사 > 연차 신청 메뉴에서 신청 가능합니다.\n최소 3일 전 신청 필수이며, 팀장 승인 후 확정됩니다.\n긴급한 경우 팀장과 협의 후 신청 가능합니다.\n\n문의: 인사팀 임인사님",
            "Q: 출퇴근 기록은 어떻게 확인하나요?\n\nA: 사내 포털 > 근태 > 출퇴근 기록에서 월별로 확인 가능합니다.\n출퇴근 시간, 외출/복귀 내역, 연장근무 시간 등이 표시됩니다.\n이의 신청은 해당 월 말일까지 가능합니다.\n\n문의: 인사팀 오채용님",
            "Q: 개발 서버 접속이 안 됩니다\n\nA: 다음 사항을 확인해주세요:\n1. VPN 연결 여부\n2. 계정 권한 확인\n3. 방화벽 설정 확인\n\n여전히 접속이 안 되면 IT 지원팀으로 문의 바랍니다.\n긴급한 경우 슬랙 #it-support 채널에 문의하세요.\n\n문의: 개발팀 김개발님",
            "Q: 디자인 리소스는 어디서 받나요?\n\nA: Figma 팀 라이브러리에서 다운로드 가능합니다.\n- 아이콘: Design System > Icons\n- 이미지: Design System > Assets\n- 폰트: Design System > Typography\n\n접근 권한이 없으면 디자인팀에 요청하세요.\n\n문의: 디자인팀 조UI님",
            "Q: 프로젝트 예산 승인 절차는?\n\nA: 프로젝트 예산 승인 절차:\n1. 예산 계획서 작성 (기획팀 양식)\n2. 팀장 1차 검토\n3. 기획팀 검토 및 수정\n4. 재무팀 최종 승인\n\n예산 계획서에는 상세 항목별 금액과 근거를 명시해야 합니다.\n\n문의: 재무팀 송재무님",
            "Q: 회식비 지급 기준은 어떻게 되나요?\n\nA: 회식비 지급 기준:\n- 부서 회식: 1인당 5만원\n- 전체 회식: 1인당 7만원\n- 프로젝트 완료 축하: 1인당 3만원\n\n영수증 필수이며, 사전 신청 및 승인 필요합니다.\n\n문의: 재무팀 유예산님",
            "Q: 코드 리뷰는 언제까지 해야 하나요?\n\nA: 코드 리뷰는 Pull Request 생성 후 24시간 이내 완료를 권장합니다.\n긴급한 경우 4시간 이내 리뷰 요청 가능합니다.\n리뷰어가 없으면 개발팀 채널에 @here로 공지하세요.\n\n문의: 개발팀 박코드님",
            "Q: 신규 입사자 교육은 필수인가요?\n\nA: 네, 필수입니다.\n입사 후 첫 주에 다음 교육을 수료해야 합니다:\n1. 보안 교육 (2시간)\n2. 협업 도구 교육 (3시간)\n3. 코드 컨벤션 교육 (2시간)\n\n교육 일정은 인사팀에서 개별 안내합니다.\n\n문의: 인사팀 윤인재님",
            "Q: 재택근무 중에도 출근 기록을 해야 하나요?\n\nA: 네, 재택근무일에도 정상적으로 출근 기록을 해야 합니다.\n사내 포털 > 근태 > 출근 기록에서 재택근무로 체크 후 기록하세요.\n재택근무 시간도 정상 근무 시간(9시~18시)을 준수해야 합니다.\n\n문의: 인사팀 임인사님",
            "Q: 사내 시스템 비밀번호를 잊어버렸습니다\n\nA: 비밀번호 재설정 방법:\n1. 로그인 화면에서 '비밀번호 찾기' 클릭\n2. 등록된 이메일로 재설정 링크 수신\n3. 링크 클릭 후 새 비밀번호 설정\n\n이메일도 기억나지 않으면 IT 지원팀으로 문의하세요.\n\n문의: IT 지원팀 (슬랙 #it-support)"
        );
    }

    // 댓글 템플릿
    private String getRandomReply() {
        List<String> replies = Arrays.asList(
            "감사합니다. 잘 확인했습니다!",
            "추가로 궁금한 점이 있어서 질문드립니다.",
            "도움이 많이 되었어요. 고맙습니다!",
            "제 경우에는 조금 다르게 적용했는데, 공유해도 될까요?",
            "이 부분은 조금 더 자세히 설명해주실 수 있나요?",
            "완벽하게 이해했습니다. 좋은 정보 감사합니다.",
            "저도 같은 경험이 있어서 공감이 가네요.",
            "다음에 적용해보겠습니다. 유용한 정보 감사합니다.",
            "혹시 예외 케이스는 어떻게 처리하나요?",
            "좋은 가이드네요. 팀원들에게 공유하겠습니다.",
            "추가 자료가 있으면 공유 부탁드립니다.",
            "바로 적용 가능할 것 같습니다. 감사합니다!"
        );
        return replies.get(random.nextInt(replies.size()));
    }
}

