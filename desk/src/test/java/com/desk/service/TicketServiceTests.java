package com.desk.service;

import com.desk.domain.Member;
import com.desk.domain.TicketGrade;
import com.desk.dto.TicketCreateDTO;
import com.desk.dto.TicketFilterDTO;
import com.desk.dto.TicketSentListDTO;
import com.desk.repository.MemberRepository;
import com.desk.repository.TicketPersonalRepository;
import com.desk.repository.TicketRepository;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.*;
import org.springframework.test.annotation.Rollback;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Log4j2
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TicketServiceTests {

    @Autowired
    private TicketService ticketService;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private TicketPersonalRepository ticketPersonalRepository;

    @Autowired
    private MemberRepository memberRepository;

    // create()에서 생성한 티켓 정보를 이후 테스트에서 공유
    private Long createdTno;
    private String createdWriterEmail;
    private List<String> createdReceiverEmails;

    /**
     * DB에서 실제 존재하는 Member들을 골라 writer/receivers로 사용
     * - 최소 2명(권장 3~4명) 있어야 create 테스트가 안정적입니다.
     */
    private void prepareTestMembers() {
        List<Member> members = memberRepository.findAll(Sort.by(Sort.Direction.ASC, "email"));

        // 최소 writer 1 + receiver 1 이상 필요
        assertTrue(members.size() >= 2,
                "테스트를 위해 Member가 최소 2명 이상 DB에 존재해야 합니다. (writer 1 + receiver 1)");

        Member writer = members.get(0);
        createdWriterEmail = writer.getEmail();

        // receivers는 writer 제외하고 1~3명 정도 선택
        createdReceiverEmails = members.stream()
                .map(Member::getEmail)
                .filter(email -> !email.equals(createdWriterEmail))
                .limit(Math.min(3, members.size() - 1))
                .toList();

        assertFalse(createdReceiverEmails.isEmpty(),
                "테스트 receivers가 비어있습니다. Member 데이터가 충분한지 확인하세요.");

        log.info("[TEST_MEMBERS] writerEmail={}, receivers={}", createdWriterEmail, createdReceiverEmails);
    }

    @Test
    @Order(1)
    @Rollback(false)
    @DisplayName("티켓 생성 테스트 - Ticket과 TicketPersonal이 함께 생성되는지 확인 (email 기반)")
    void create() {
        // given
        prepareTestMembers();

        TicketCreateDTO req = TicketCreateDTO.builder()
                .title("업무 요청: API 연동 확인")
                .content("외부 API 연동/응답 스펙 확인 부탁드립니다.")
                .purpose("연동 점검")
                .requirement("응답 예시 + 에러 케이스 정리")
                .grade(TicketGrade.MIDDLE)
                .deadline(LocalDateTime.now().plusDays(3))
                .receivers(createdReceiverEmails)   // ★ 이메일 리스트
                .build();

        // when
        TicketSentListDTO created = ticketService.create(req, createdWriterEmail); // ★ 이메일

        // then
        assertNotNull(created, "create 결과 DTO가 null이면 안 됩니다.");
        assertNotNull(created.getTno(), "생성된 티켓 tno는 null이면 안 됩니다.");

        createdTno = created.getTno(); // 이후 테스트에서 사용

        assertEquals(createdWriterEmail, created.getWriter(), "writer(email)가 일치해야 합니다.");
        assertEquals(req.getTitle(), created.getTitle());
        assertEquals(req.getContent(), created.getContent());
        assertEquals(req.getPurpose(), created.getPurpose());
        assertEquals(req.getRequirement(), created.getRequirement());
        assertEquals(req.getGrade(), created.getGrade());
        assertNotNull(created.getBirth(), "생성 시간(birth)이 null이면 안 됩니다.");

        assertNotNull(created.getPersonals(), "personals 리스트가 null이면 안 됩니다.");
        assertEquals(createdReceiverEmails.size(), created.getPersonals().size(),
                "수신인 개수가 일치해야 합니다.");

        List<String> dtoReceivers = created.getPersonals().stream()
                .map(p -> p.getReceiver())
                .toList();

        assertTrue(dtoReceivers.containsAll(createdReceiverEmails),
                "모든 수신인(email)이 포함되어야 합니다.");

        log.info("[CREATE] createdTno={}, writer={}, receivers={}, personals.size={}",
                createdTno, createdWriterEmail, createdReceiverEmails, created.getPersonals().size());
    }

//    @Test
//    @Order(2)
//    @DisplayName("보낸 티켓 목록 조회 테스트 - 필터 없음 (email 기반)")
//    void listSent() {
//        assertNotNull(createdWriterEmail, "create 테스트가 먼저 실행되어 writerEmail이 세팅되어야 합니다.");
//
//        TicketFilterDTO filter = TicketFilterDTO.builder().build();
//        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "tno"));
//
//        Page<TicketSentListDTO> page = ticketService.listSent(createdWriterEmail, filter, pageable);
//
//        assertNotNull(page, "listSent 결과 page가 null이면 안 됩니다.");
//
//        log.info("[LIST_SENT] writer={}, totalElements={}, totalPages={}, pageSize={}",
//                createdWriterEmail, page.getTotalElements(), page.getTotalPages(), page.getSize());
//
//        page.getContent().forEach(dto -> {
//            assertEquals(createdWriterEmail, dto.getWriter(),
//                    "모든 티켓의 작성자(email)가 일치해야 합니다.");
//            assertNotNull(dto.getTno());
//            assertNotNull(dto.getTitle());
//        });
//    }

//    @Test
//    @Order(3)
//    @DisplayName("보낸 티켓 목록 조회 테스트 - 등급 필터 적용 (email 기반)")
//    void listSentWithFilter() {
//        assertNotNull(createdWriterEmail, "create 테스트가 먼저 실행되어 writerEmail이 세팅되어야 합니다.");
//
//        TicketFilterDTO filter = TicketFilterDTO.builder()
//                .grade(TicketGrade.HIGH)
//                .build();
//        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "tno"));
//
//        Page<TicketSentListDTO> page = ticketService.listSent(createdWriterEmail, filter, pageable);
//
//        assertNotNull(page);
//
//        log.info("[LIST_SENT_WITH_FILTER] writer={}, grade={}, totalElements={}",
//                createdWriterEmail, filter.getGrade(), page.getTotalElements());
//
//        page.getContent().forEach(dto -> {
//            assertEquals(createdWriterEmail, dto.getWriter());
//            assertEquals(filter.getGrade(), dto.getGrade(),
//                    "필터링된 결과의 grade가 일치해야 합니다.");
//        });
//    }

    @Test
    @Order(4)
    @DisplayName("보낸 티켓 단일 조회 테스트 - 정상 케이스 (create에서 만든 tno 사용)")
    void readSent() {
        assertNotNull(createdTno, "create 테스트가 먼저 실행되어 createdTno가 세팅되어야 합니다.");

        TicketSentListDTO dto = ticketService.readSent(createdTno, createdWriterEmail);

        assertNotNull(dto);
        assertEquals(createdTno, dto.getTno());
        assertEquals(createdWriterEmail, dto.getWriter());
        assertNotNull(dto.getPersonals());
        assertTrue(dto.getPersonals().size() > 0);

        log.info("[READ_SENT] tno={}, writer={}, title={}, personals={}",
                createdTno, createdWriterEmail, dto.getTitle(), dto.getPersonals().size());
    }

    @Test
    @Order(5)
    @DisplayName("보낸 티켓 단일 조회 테스트 - 권한 없는 사용자 접근 시 예외 발생")
    void readSent_Unauthorized() {
        assertNotNull(createdTno, "create 테스트가 먼저 실행되어 createdTno가 세팅되어야 합니다.");

        String unauthorizedEmail = "unauthorized-" + UUID.randomUUID() + "@test.com";

        assertThrows(IllegalArgumentException.class, () ->
                ticketService.readSent(createdTno, unauthorizedEmail)
        );
    }

    @Test
    @Order(6)
    @Rollback(false)
    @DisplayName("보낸 티켓 삭제 테스트 - Ticket과 TicketPersonal이 함께 삭제되는지 확인 (create에서 만든 tno 사용)")
    void deleteSent() {
        assertNotNull(createdTno, "create 테스트가 먼저 실행되어 createdTno가 세팅되어야 합니다.");

        boolean ticketExistsBefore = ticketRepository.existsById(createdTno);
        assertTrue(ticketExistsBefore, "삭제 전 Ticket이 존재해야 합니다.");

        long personalCountBefore = ticketPersonalRepository.countByTicket_Tno(createdTno);
        log.info("[DELETE_BEFORE] tno={}, personalCount={}", createdTno, personalCountBefore);

        ticketService.deleteSent(createdTno, createdWriterEmail);

        boolean ticketExistsAfter = ticketRepository.existsById(createdTno);
        assertFalse(ticketExistsAfter, "Ticket이 삭제되어야 합니다.");

        long personalCountAfter = ticketPersonalRepository.countByTicket_Tno(createdTno);
        assertEquals(0, personalCountAfter,
                "Ticket 삭제 시 TicketPersonal도 모두 삭제되어야 합니다. (cascade/orphanRemoval 확인)");

        log.info("[DELETE_AFTER] tno={}, remainingPersonalCount={}", createdTno, personalCountAfter);
    }

    @Test
    @Order(7)
    @DisplayName("보낸 티켓 삭제 테스트 - 권한 없는 사용자 삭제 시 예외 발생")
    void deleteSent_Unauthorized() {
        // 삭제는 이미 (Order 6)에서 수행했으니, 여기서는 "존재하는 티켓"을 직접 하나 더 만들어도 되지만
        // 간단히 "Not allowed" 케이스만 보려면 새로 생성 후 삭제 시도하는 방식이 안전합니다.
        prepareTestMembers();

        TicketCreateDTO req = TicketCreateDTO.builder()
                .title("권한 테스트용 티켓")
                .content("권한 테스트")
                .purpose("권한")
                .requirement("권한")
                .grade(TicketGrade.LOW)
                .deadline(LocalDateTime.now().plusDays(1))
                .receivers(createdReceiverEmails)
                .build();

        TicketSentListDTO created = ticketService.create(req, createdWriterEmail);
        Long tno = created.getTno();

        String unauthorizedEmail = "unauthorized-" + UUID.randomUUID() + "@test.com";

        assertThrows(IllegalArgumentException.class, () ->
                ticketService.deleteSent(tno, unauthorizedEmail)
        );
    }

    @Test
    @Order(8)
    @DisplayName("존재하지 않는 티켓 조회 시 예외 발생")
    void readSent_NotFound() {
        assertNotNull(createdWriterEmail, "writerEmail 세팅 필요");

        Long nonExistentTno = 999999L;

        assertThrows(IllegalArgumentException.class, () ->
                ticketService.readSent(nonExistentTno, createdWriterEmail)
        );
    }

    @Test
    @Order(9)
    @DisplayName("존재하지 않는 티켓 삭제 시 예외 발생")
    void deleteSent_NotFound() {
        assertNotNull(createdWriterEmail, "writerEmail 세팅 필요");

        Long nonExistentTno = 999999L;

        assertThrows(IllegalArgumentException.class, () ->
                ticketService.deleteSent(nonExistentTno, createdWriterEmail)
        );
    }
}
