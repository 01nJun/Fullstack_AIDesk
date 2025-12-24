package com.desk.service;

import com.desk.domain.Member;
import com.desk.domain.TicketGrade;
import com.desk.domain.TicketState;
import com.desk.dto.TicketCreateDTO;
import com.desk.dto.TicketFilterDTO;
import com.desk.dto.TicketReceivedListDTO;
import com.desk.dto.TicketSentListDTO;
import com.desk.repository.MemberRepository;
import com.desk.repository.TicketPersonalRepository;
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
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TicketPersonalServiceTests {

    @Autowired
    private PersonalTicketService personalTicketService;

    @Autowired
    private TicketService ticketService; // ★ 받은함 테스트용 데이터(티켓+퍼스널) 생성에 사용

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TicketPersonalRepository ticketPersonalRepository;

    // 테스트 데이터(한 번 생성해서 공유)
    private String writerEmail;
    private String receiverEmail;      // 메인 receiver (받은함 주인)
    private String receiverEmail2;     // 보조 receiver
    private Long createdTno;
    private Long createdTpno;          // receiverEmail + createdTno 로 찾아낸 tpno

    /**
     * 테스트 전제:
     * - Member가 최소 2명 이상 있어야 함 (writer 1 + receiver 1)
     * - receiverEmail은 실제 DB에 존재하는 member email이어야 함
     */
    private void prepareTestData() {
        if (createdTno != null && createdTpno != null) return;

        List<Member> members = memberRepository.findAll(Sort.by(Sort.Direction.ASC, "email"));
        assertTrue(members.size() >= 2,
                "테스트를 위해 Member가 최소 2명 이상 DB에 존재해야 합니다. (writer 1 + receiver 1)");

        writerEmail = members.get(0).getEmail();
        receiverEmail = members.get(1).getEmail();
        receiverEmail2 = (members.size() >= 3) ? members.get(2).getEmail() : null;

        List<String> receivers = (receiverEmail2 == null)
                ? List.of(receiverEmail)
                : List.of(receiverEmail, receiverEmail2);

        TicketCreateDTO req = TicketCreateDTO.builder()
                .title("받은함 테스트용 티켓")
                .content("받은함 서비스 테스트를 위한 본문")
                .purpose("테스트")
                .requirement("테스트")
                .grade(TicketGrade.MIDDLE)
                .deadline(LocalDateTime.now().plusDays(2))
                .receivers(receivers)
                .build();

        TicketSentListDTO created = ticketService.create(req, writerEmail);
        createdTno = created.getTno();

        createdTpno = ticketPersonalRepository.findPnoByReceiverAndTno(receiverEmail, createdTno)
                .orElseThrow(() -> new IllegalStateException(
                        "생성된 티켓에 대해 receiver+tno로 tpno를 찾지 못했습니다. receiver=" + receiverEmail + ", tno=" + createdTno
                ));

        log.info("[SETUP] writerEmail={}, receiverEmail={}, tno={}, tpno={}",
                writerEmail, receiverEmail, createdTno, createdTpno);
    }

//    @Test
//    @Order(1)
//    @Rollback(false)
//    @DisplayName("받은 티켓 목록 조회 테스트 - 필터 없음 (email 기반)")
//    void listInbox() {
//        prepareTestData();
//
//        TicketFilterDTO filter = TicketFilterDTO.builder().build();
//        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "pno"));
//
//        Page<TicketReceivedListDTO> page =
//                personalTicketService.listRecieveTicket(receiverEmail, filter, pageable);
//
//        assertNotNull(page);
//        assertTrue(page.getTotalElements() >= 0);
//
//        // 최소 방금 만든 티켓은 있어야 정상
//        assertTrue(
//                page.getContent().stream().anyMatch(dto -> dto.getTno().equals(createdTno)),
//                "받은함 목록에 방금 생성한 티켓(tno=" + createdTno + ")이 포함되어야 합니다."
//        );
//
//        page.getContent().forEach(dto -> {
//            assertEquals(receiverEmail, dto.getReceiver(), "receiver(email)가 일치해야 합니다.");
//            assertNotNull(dto.getPno());
//            assertNotNull(dto.getTno());
//            assertNotNull(dto.getTitle());
//            assertNotNull(dto.getState());
//        });
//
//        log.info("[LIST_INBOX] receiver={}, totalElements={}", receiverEmail, page.getTotalElements());
//    }

//    @Test
//    @Order(2)
//    @Rollback(false)
//    @DisplayName("받은 티켓 목록 조회 테스트 - 상태 필터 적용")
//    void listInboxWithStateFilter() {
//        prepareTestData();
//
//        TicketFilterDTO filter = TicketFilterDTO.builder()
//                .state(TicketState.NEW)
//                .build();
//        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "pno"));
//
//        Page<TicketReceivedListDTO> page =
//                personalTicketService.listRecieveTicket(receiverEmail, filter, pageable);
//
//        assertNotNull(page);
//
//        page.getContent().forEach(dto -> {
//            assertEquals(receiverEmail, dto.getReceiver());
//            assertEquals(filter.getState(), dto.getState());
//        });
//
//        log.info("[LIST_INBOX_STATE] receiver={}, state={}, totalElements={}",
//                receiverEmail, filter.getState(), page.getTotalElements());
//    }

    @Test
    @Order(3)
    @Rollback(false)
    @DisplayName("받은 티켓 단일 조회 테스트 - tpno 기준, 읽음 처리 포함")
    void readInbox_markAsRead() {
        prepareTestData();

        TicketReceivedListDTO dto =
                personalTicketService.readRecieveTicket(createdTpno, receiverEmail, true);

        assertNotNull(dto);
        assertEquals(createdTpno, dto.getPno());
        assertEquals(receiverEmail, dto.getReceiver());
        assertEquals(createdTno, dto.getTno());
        assertTrue(dto.isIsread(), "markAsRead=true면 읽음 상태가 true여야 합니다.");

        log.info("[READ_INBOX] tpno={}, receiver={}, tno={}, read={}",
                createdTpno, receiverEmail, createdTno, dto.isIsread());
    }

    @Test
    @Order(4)
    @Rollback(false)
    @DisplayName("받은 티켓 단일 조회 테스트 - tpno 기준, 읽음 처리 없음")
    void readInbox_withoutMarkAsRead() {
        prepareTestData();

        // 읽음 처리 하지 않는 호출 (상태 변화 강제 X)
        TicketReceivedListDTO dto =
                personalTicketService.readRecieveTicket(createdTpno, receiverEmail, false);

        assertNotNull(dto);
        assertEquals(createdTpno, dto.getPno());
        assertEquals(receiverEmail, dto.getReceiver());

        log.info("[READ_INBOX_NO_MARK] tpno={}, receiver={}, read={}",
                createdTpno, receiverEmail, dto.isIsread());
    }

    @Test
    @Order(5)
    @Rollback(false)
    @DisplayName("받은 티켓 단일 조회 테스트 - tno 기준")
    void readInboxByTno() {
        prepareTestData();

        TicketReceivedListDTO dto =
                personalTicketService.readRecieveTicketByTno(createdTno, receiverEmail, true);

        assertNotNull(dto);
        assertEquals(createdTno, dto.getTno());
        assertEquals(receiverEmail, dto.getReceiver());
        assertNotNull(dto.getPno());
        assertTrue(dto.isIsread(), "markAsRead=true면 읽음 상태가 true여야 합니다.");

        log.info("[READ_BY_TNO] tno={}, receiver={}, tpno={}, read={}",
                createdTno, receiverEmail, dto.getPno(), dto.isIsread());
    }

    @Test
    @Order(6)
    @Rollback(false)
    @DisplayName("받은 티켓 단일 조회 테스트 - 권한 없는 사용자 접근 시 예외 발생")
    void readInbox_Unauthorized() {
        prepareTestData();

        String unauthorizedEmail = "unauthorized-" + UUID.randomUUID() + "@test.com";

        assertThrows(IllegalArgumentException.class, () ->
                personalTicketService.readRecieveTicket(createdTpno, unauthorizedEmail, true)
        );
    }

    @Test
    @Order(7)
    @Rollback(false)
    @DisplayName("받은 티켓 진행상태 변경 테스트")
    void changeState() {
        prepareTestData();

        TicketState targetState = TicketState.DONE;

        TicketReceivedListDTO dto =
                personalTicketService.changeState(createdTpno, receiverEmail, targetState);

        assertNotNull(dto);
        assertEquals(createdTpno, dto.getPno());
        assertEquals(receiverEmail, dto.getReceiver());
        assertEquals(targetState, dto.getState());

        log.info("[CHANGE_STATE] tpno={}, receiver={}, state={}",
                createdTpno, receiverEmail, dto.getState());
    }

    @Test
    @Order(8)
    @Rollback(false)
    @DisplayName("받은 티켓 진행상태 변경 테스트 - 권한 없는 사용자 변경 시 예외 발생")
    void changeState_Unauthorized() {
        prepareTestData();

        String unauthorizedEmail = "unauthorized-" + UUID.randomUUID() + "@test.com";

        assertThrows(IllegalArgumentException.class, () ->
                personalTicketService.changeState(createdTpno, unauthorizedEmail, TicketState.DONE)
        );
    }

    @Test
    @Order(9)
    @Rollback(false)
    @DisplayName("존재하지 않는 받은 티켓 조회 시 예외 발생")
    void readInbox_NotFound() {
        prepareTestData();

        Long nonExistentTpno = 999999L;

        assertThrows(IllegalArgumentException.class, () ->
                personalTicketService.readRecieveTicket(nonExistentTpno, receiverEmail, true)
        );
    }

    @Test
    @Order(10)
    @Rollback(false)
    @DisplayName("존재하지 않는 받은 티켓 상태 변경 시 예외 발생")
    void changeState_NotFound() {
        prepareTestData();

        Long nonExistentTpno = 999999L;

        assertThrows(IllegalArgumentException.class, () ->
                personalTicketService.changeState(nonExistentTpno, receiverEmail, TicketState.DONE)
        );
    }
}
