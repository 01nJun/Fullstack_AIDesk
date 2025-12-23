package com.desk.service;

import com.desk.domain.TicketState;
import com.desk.dto.TicketFilterDTO;
import com.desk.dto.TicketReceivedListDTO;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.*;
import org.springframework.test.annotation.Rollback;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Log4j2
class TicketPersonalServiceTests {

    @Autowired
    private PersonalTicketService personalTicketService;

    /* =========================================================
     * DB 확인하면서 아래 전역변수 바꿔서 테스트 하시면 됩니다 ^^
     * ========================================================= */

    // 1) listRecieveTicket (received list) 테스트용
    private static String LIST_INBOX_RECEIVER = "한정연";

    // 2) readRecieveTicket 테스트용 (tpno 기준)
    private static Long READ_INBOX_TPNO = 2L;
    private static String READ_INBOX_RECEIVER = "김영희";

    // 3) readRecieveTicketByTno 테스트용 (tno 기준)
    private static Long READ_INBOX_BY_TNO = 1L;
    private static String READ_INBOX_BY_TNO_RECEIVER = "김철수";

    // 4) changeState 테스트용
    private static Long CHANGE_STATE_TPNO = 3L;
    private static String CHANGE_STATE_RECEIVER = "한정연";
    private static TicketState CHANGE_TO_STATE = TicketState.DONE;

    @Test
    @Rollback(false)
    @DisplayName("받은 티켓 목록 조회 테스트 - 필터 없음")
    void listInbox() {
        // given
        TicketFilterDTO filter = TicketFilterDTO.builder().build();
        Pageable pageable = PageRequest.of(
                0, 10,
                Sort.by(Sort.Direction.DESC, "pno")
        );

        // when
        Page<TicketReceivedListDTO> page =
                personalTicketService.listRecieveTicket(LIST_INBOX_RECEIVER, filter, pageable);

        // then
        log.info("[LIST_INBOX_PAGE] receiver={}", LIST_INBOX_RECEIVER);
        log.info(" - pageNumber     : {}", page.getNumber());
        log.info(" - pageSize       : {}", page.getSize());
        log.info(" - totalElements  : {}", page.getTotalElements());
        log.info(" - totalPages     : {}", page.getTotalPages());
        log.info(" - numberOfElements(현재페이지) : {}", page.getNumberOfElements());
        log.info(" - isFirst        : {}", page.isFirst());
        log.info(" - isLast         : {}", page.isLast());
        log.info(" - hasNext        : {}", page.hasNext());
        log.info(" - hasPrevious    : {}", page.hasPrevious());

        assertNotNull(page, "받은함 Page 결과가 null이면 안 됩니다.");
        assertTrue(page.getTotalElements() >= 0, "전체 개수는 0 이상이어야 합니다.");

        page.getContent().forEach(dto -> {
            assertEquals(LIST_INBOX_RECEIVER, dto.getReceiver(),
                    "모든 티켓의 수신인이 일치해야 합니다.");
            assertNotNull(dto.getPno(), "pno는 null이면 안 됩니다.");
            assertNotNull(dto.getTno(), "tno는 null이면 안 됩니다.");
            assertNotNull(dto.getTitle(), "제목은 null이면 안 됩니다.");
            assertNotNull(dto.getState(), "상태는 null이면 안 됩니다.");
            log.info("[INBOX_ITEM] pno={}, tno={}, title={}, receiver={}, read={}, state={}",
                    dto.getPno(),
                    dto.getTno(),
                    dto.getTitle(),
                    dto.getReceiver(),
                    dto.isIsread(),
                    dto.getState());
        });
    }

    @Test
    @Rollback(false)
    @DisplayName("받은 티켓 목록 조회 테스트 - 상태 필터 적용")
    void listInboxWithStateFilter() {
        // given
        TicketFilterDTO filter = TicketFilterDTO.builder()
                .state(TicketState.NEW)
                .build();
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "pno"));

        // when
        Page<TicketReceivedListDTO> page =
                personalTicketService.listRecieveTicket(LIST_INBOX_RECEIVER, filter, pageable);

        // then
        log.info("[LIST_INBOX_WITH_STATE_FILTER] receiver={}, state={}, totalElements={}",
                LIST_INBOX_RECEIVER, filter.getState(), page.getTotalElements());

        assertNotNull(page, "필터링된 받은함 Page 결과가 null이면 안 됩니다.");

        page.getContent().forEach(dto -> {
            assertEquals(filter.getState(), dto.getState(),
                    "필터링된 결과의 상태가 일치해야 합니다.");
            assertEquals(LIST_INBOX_RECEIVER, dto.getReceiver(),
                    "수신인이 일치해야 합니다.");
            log.info("[FILTERED_INBOX_ITEM] pno={}, state={}", dto.getPno(), dto.getState());
        });
    }

    @Test
    @Rollback(false)
    @DisplayName("받은 티켓 목록 조회 테스트 - 읽음 여부 필터 적용")
    void listInboxWithReadFilter() {
        // given
        TicketFilterDTO filter = TicketFilterDTO.builder()
                .read(true)
                .build();
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "pno"));

        // when
        Page<TicketReceivedListDTO> page =
                personalTicketService.listRecieveTicket(LIST_INBOX_RECEIVER, filter, pageable);

        // then
        log.info("[LIST_INBOX_WITH_READ_FILTER] receiver={}, read={}, totalElements={}",
                LIST_INBOX_RECEIVER, filter.getRead(), page.getTotalElements());

        assertNotNull(page, "필터링된 받은함 Page 결과가 null이면 안 됩니다.");

        page.getContent().forEach(dto -> {
            assertEquals(filter.getRead(), dto.isIsread(),
                    "필터링된 결과의 읽음 상태가 일치해야 합니다.");
            log.info("[FILTERED_INBOX_ITEM] pno={}, read={}", dto.getPno(), dto.isIsread());
        });
    }

    @Test
    @Rollback(false)
    @DisplayName("받은 티켓 단일 조회 테스트 - tpno 기준, 읽음 처리 포함")
    void readInbox() {
        // given
        Long tpno = READ_INBOX_TPNO;
        String receiver = READ_INBOX_RECEIVER;
        boolean markAsRead = true;

        // when & then
        try {
            TicketReceivedListDTO dto = personalTicketService.readRecieveTicket(tpno, receiver, markAsRead);

            log.info("[READ_INBOX] pno={}, receiver={}, tno={}, title={}, read={}, state={}",
                    tpno, receiver, dto.getTno(), dto.getTitle(),
                    dto.isIsread(), dto.getState());

            assertNotNull(dto, "readRecieveTicket 결과가 null이면 안 됩니다.");
            assertEquals(tpno, dto.getPno(), "조회한 pno가 일치해야 합니다.");
            assertEquals(receiver, dto.getReceiver(), "수신인이 일치해야 합니다.");
            assertNotNull(dto.getTno(), "tno는 null이면 안 됩니다.");
            assertNotNull(dto.getTitle(), "제목은 null이면 안 됩니다.");
            if (markAsRead) {
                assertTrue(dto.isIsread(), "markAsRead=true일 때 읽음 상태가 true여야 합니다.");
            }

        } catch (IllegalArgumentException e) {
            log.warn("[READ_INBOX_FAIL] tpno={}, receiver={}, msg={}",
                    tpno, receiver, e.getMessage());
            fail("받은 티켓 조회 실패: " + e.getMessage());
        }
    }

    @Test
    @Rollback(false)
    @DisplayName("받은 티켓 단일 조회 테스트 - tpno 기준, 읽음 처리 없음")
    void readInbox_WithoutMarkAsRead() {
        // given
        Long tpno = READ_INBOX_TPNO;
        String receiver = READ_INBOX_RECEIVER;
        boolean markAsRead = false;

        // when & then
        try {
            TicketReceivedListDTO dto = personalTicketService.readRecieveTicket(tpno, receiver, markAsRead);

            assertNotNull(dto, "readRecieveTicket 결과가 null이면 안 됩니다.");
            assertEquals(tpno, dto.getPno(), "조회한 pno가 일치해야 합니다.");
            // 읽음 상태는 기존 상태 유지 (변경되지 않음)

        } catch (IllegalArgumentException e) {
            log.warn("[READ_INBOX_FAIL] tpno={}, receiver={}, msg={}",
                    tpno, receiver, e.getMessage());
            fail("받은 티켓 조회 실패: " + e.getMessage());
        }
    }

    @Test
    @Rollback(false)
    @DisplayName("받은 티켓 단일 조회 테스트 - tno 기준")
    void readInboxByTno() {
        // given
        Long tno = READ_INBOX_BY_TNO;
        String receiver = READ_INBOX_BY_TNO_RECEIVER;
        boolean markAsRead = true;

        // when & then
        try {
            TicketReceivedListDTO dto = personalTicketService.readRecieveTicketByTno(tno, receiver, markAsRead);

            log.info("[READ_INBOX_BY_TNO] tno={}, receiver={}, pno={}, title={}, read={}, state={}",
                    tno, receiver, dto.getPno(), dto.getTitle(),
                    dto.isIsread(), dto.getState());

            assertNotNull(dto, "readRecieveTicketByTno 결과가 null이면 안 됩니다.");
            assertEquals(tno, dto.getTno(), "조회한 tno가 일치해야 합니다.");
            assertEquals(receiver, dto.getReceiver(), "수신인이 일치해야 합니다.");
            assertNotNull(dto.getPno(), "pno는 null이면 안 됩니다.");
            assertNotNull(dto.getTitle(), "제목은 null이면 안 됩니다.");

        } catch (IllegalArgumentException e) {
            log.warn("[READ_INBOX_BY_TNO_FAIL] tno={}, receiver={}, msg={}",
                    tno, receiver, e.getMessage());
            fail("받은 티켓 조회 실패: " + e.getMessage());
        }
    }

    @Test
    @Rollback(false)
    @DisplayName("받은 티켓 단일 조회 테스트 - 권한 없는 사용자 접근 시 예외 발생")
    void readInbox_Unauthorized() {
        // given
        Long tpno = READ_INBOX_TPNO;
        String unauthorizedReceiver = "권한없는사용자";

        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            personalTicketService.readRecieveTicket(tpno, unauthorizedReceiver, true);
        }, "권한 없는 사용자가 조회할 때 예외가 발생해야 합니다.");
    }

    @Test
    @Rollback(false)
    @DisplayName("받은 티켓 진행상태 변경 테스트")
    void changeState() {
        // given
        Long tpno = CHANGE_STATE_TPNO;
        String receiver = CHANGE_STATE_RECEIVER;
        TicketState targetState = CHANGE_TO_STATE;

        // when & then
        try {
            TicketReceivedListDTO dto = personalTicketService.changeState(tpno, receiver, targetState);

            log.info("[CHANGE_STATE] pno={}, receiver={}, changedState={}",
                    tpno, receiver, dto.getState());

            assertNotNull(dto, "changeState 결과가 null이면 안 됩니다.");
            assertEquals(tpno, dto.getPno(), "pno가 일치해야 합니다.");
            assertEquals(receiver, dto.getReceiver(), "수신인이 일치해야 합니다.");
            assertEquals(targetState, dto.getState(), 
                    "state가 " + targetState + "로 변경되어야 합니다.");

        } catch (IllegalArgumentException e) {
            log.warn("[CHANGE_STATE_FAIL] pno={}, receiver={}, targetState={}, msg={}",
                    tpno, receiver, targetState, e.getMessage());
            fail("상태 변경 실패: " + e.getMessage());
        }
    }

    @Test
    @Rollback(false)
    @DisplayName("받은 티켓 진행상태 변경 테스트 - 권한 없는 사용자 변경 시 예외 발생")
    void changeState_Unauthorized() {
        // given
        Long tpno = CHANGE_STATE_TPNO;
        String unauthorizedReceiver = "권한없는사용자";
        TicketState targetState = TicketState.DONE;

        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            personalTicketService.changeState(tpno, unauthorizedReceiver, targetState);
        }, "권한 없는 사용자가 상태를 변경할 때 예외가 발생해야 합니다.");
    }

    @Test
    @Rollback(false)
    @DisplayName("존재하지 않는 받은 티켓 조회 시 예외 발생")
    void readInbox_NotFound() {
        // given
        Long nonExistentTpno = Long.valueOf(999999L);
        String receiver = READ_INBOX_RECEIVER;

        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            personalTicketService.readRecieveTicket(nonExistentTpno, receiver, true);
        }, "존재하지 않는 받은 티켓 조회 시 예외가 발생해야 합니다.");
    }

    @Test
    @Rollback(false)
    @DisplayName("존재하지 않는 받은 티켓 상태 변경 시 예외 발생")
    void changeState_NotFound() {
        // given
        Long nonExistentTpno = Long.valueOf(999999L);
        String receiver = CHANGE_STATE_RECEIVER;
        TicketState targetState = TicketState.DONE;

        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            personalTicketService.changeState(nonExistentTpno, receiver, targetState);
        }, "존재하지 않는 받은 티켓 상태 변경 시 예외가 발생해야 합니다.");
    }
}
