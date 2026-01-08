package com.desk.repository;

import com.desk.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@SpringBootTest
public class TicketDummyDataTests {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @Transactional
    @Commit
    public void insertDummyTicketData() {
        // 필요한 멤버들 생성 (없으면) - user9 포함
        String[] emails = {"user1@desk.com", "user2@desk.com", "user3@desk.com", "user4@desk.com", "user5@desk.com", "user9@desk.com"};
        String[] nicknames = {"사원1", "사원2", "사원3", "사원4", "사원5", "사원9"};
        
        for (int i = 0; i < emails.length; i++) {
            createMemberIfNotExist(emails[i], nicknames[i], Department.values()[i % Department.values().length]);
        }

        // user9를 발신자 또는 수신자로 포함하여 보이도록 설정
        // 티켓 데이터
        List<TicketData> tickets = Arrays.asList(
            // 티켓 1: user9 -> 디자인팀 (UI 개선 요청)
            new TicketData(
                "고객 관리 페이지 UI 개선 요청",
                "고객 관리 페이지의 사용자 경험을 개선하기 위해 디자인 리뷰가 필요합니다.",
                "고객 피드백을 반영하여 페이지의 직관성을 높이고 사용성을 개선하고자 합니다.",
                "1. 현재 고객 관리 페이지 UI 분석\n2. 사용자 플로우 최적화\n3. 디자인 컴포넌트 재구성\n4. 프로토타입 제작 및 검토",
                emails[5], // writer: user9
                Arrays.asList(emails[3]), // receiver: user4 (디자인팀)
                TicketGrade.HIGH,
                LocalDateTime.now().plusDays(14)
            ),
            
            // 티켓 2: 기획팀 -> user9 (신규 기능 개발)
            new TicketData(
                "대시보드 통계 기능 추가 요청",
                "관리자 대시보드에 월별 매출 통계 및 사용자 분석 차트를 추가하고자 합니다.",
                "데이터 기반 의사결정을 위해 주요 지표를 시각화하여 제공하고자 합니다.",
                "1. 백엔드 API 개발 (통계 데이터 조회)\n2. 프론트엔드 차트 컴포넌트 구현\n3. 데이터 캐싱 최적화\n4. 반응형 디자인 적용",
                emails[4], // writer: user5 (기획팀)
                Arrays.asList(emails[5]), // receiver: user9
                TicketGrade.MIDDLE,
                LocalDateTime.now().plusDays(21)
            ),
            
            // 티켓 3: 영업팀 -> user9 (버그 수정)
            new TicketData(
                "주문 취소 시 재고 반영 오류 수정 요청",
                "주문 취소 시 재고가 제대로 반영되지 않는 문제가 발생했습니다. 긴급 수정이 필요합니다.",
                "고객 주문 취소 후 재고가 복구되지 않아 재고 관리에 오류가 발생하고 있습니다.",
                "1. 주문 취소 로직 확인\n2. 재고 업데이트 트랜잭션 검토\n3. 테스트 코드 작성 및 검증\n4. 스테이징 환경 배포 후 확인",
                emails[1], // writer: user2 (영업팀)
                Arrays.asList(emails[5]), // receiver: user9
                TicketGrade.HIGH,
                LocalDateTime.now().plusDays(3)
            ),
            
            // 티켓 4: user9 -> 개발팀 (시스템 연동)
            new TicketData(
                "인사 관리 시스템 외부 API 연동 요청",
                "새로운 출퇴근 관리 시스템과의 API 연동을 진행하고자 합니다.",
                "외부 출퇴근 관리 시스템의 데이터를 자동으로 연동하여 인사 관리 효율을 높이고자 합니다.",
                "1. 외부 API 문서 검토\n2. 인증 및 보안 설정\n3. 데이터 매핑 및 동기화 로직 개발\n4. 에러 처리 및 재시도 로직 구현",
                emails[5], // writer: user9
                Arrays.asList(emails[0]), // receiver: user1 (개발팀)
                TicketGrade.MIDDLE,
                LocalDateTime.now().plusDays(30)
            ),
            
            // 티켓 5: 재무팀 -> user9, 기획팀 (정산 시스템 개선)
            new TicketData(
                "월별 정산 리포트 자동화 시스템 구축",
                "매월 수동으로 작성하던 정산 리포트를 자동화하여 업무 효율을 높이고자 합니다.",
                "정산 리포트 작성에 소요되는 시간을 줄이고 데이터 정확성을 향상시키기 위함입니다.",
                "1. 정산 리포트 요구사항 정의 (기획팀)\n2. 데이터 수집 및 가공 로직 개발 (개발팀)\n3. 리포트 템플릿 설계\n4. 자동 발송 기능 구현",
                emails[2], // writer: user3 (재무팀)
                Arrays.asList(emails[5], emails[4]), // receiver: user9, user5 (기획팀)
                TicketGrade.LOW,
                LocalDateTime.now().plusDays(45)
            )
        );

        // 티켓 생성
        for (TicketData ticketData : tickets) {
            Member writer = memberRepository.findById(ticketData.writerEmail)
                    .orElseThrow(() -> new RuntimeException("Writer not found: " + ticketData.writerEmail));

            Ticket ticket = Ticket.builder()
                    .title(ticketData.title)
                    .content(ticketData.content)
                    .purpose(ticketData.purpose)
                    .requirement(ticketData.requirement)
                    .writer(writer)
                    .grade(ticketData.grade)
                    .deadline(ticketData.deadline)
                    .build();

            // 수신자 추가
            for (String receiverEmail : ticketData.receiverEmails) {
                Member receiver = memberRepository.findById(receiverEmail)
                        .orElseThrow(() -> new RuntimeException("Receiver not found: " + receiverEmail));
                ticket.addPersonal(TicketPersonal.builder().receiver(receiver).build());
            }

            ticketRepository.save(ticket);
        }

        System.out.println("✅ 티켓 더미데이터 5개 생성 완료!");
    }

    private void createMemberIfNotExist(String email, String nickname, Department department) {
        if (memberRepository.findById(email).isEmpty()) {
            Member member = Member.builder()
                    .email(email)
                    .pw(passwordEncoder.encode("1111"))
                    .nickname(nickname)
                    .department(department)
                    .social(false)
                    .isApproved(true)
                    .isDeleted(false)
                    .build();
            member.addRole(MemberRole.USER);
            memberRepository.save(member);
        }
    }

    // 티켓 데이터를 담을 내부 클래스
    private static class TicketData {
        String title;
        String content;
        String purpose;
        String requirement;
        String writerEmail;
        List<String> receiverEmails;
        TicketGrade grade;
        LocalDateTime deadline;

        TicketData(String title, String content, String purpose, String requirement,
                   String writerEmail, List<String> receiverEmails, TicketGrade grade, LocalDateTime deadline) {
            this.title = title;
            this.content = content;
            this.purpose = purpose;
            this.requirement = requirement;
            this.writerEmail = writerEmail;
            this.receiverEmails = receiverEmails;
            this.grade = grade;
            this.deadline = deadline;
        }
    }
}

