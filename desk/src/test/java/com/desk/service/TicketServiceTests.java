package com.desk.service;

import com.desk.domain.TicketGrade;
import com.desk.dto.TicketCreateDTO;
import com.desk.dto.TicketFilterDTO;
import com.desk.dto.TicketSentListDTO;
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

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Log4j2
class TicketServiceTests {

    @Autowired
    private TicketService ticketService;
    
    @Autowired
    private TicketRepository ticketRepository;
    
    @Autowired
    private TicketPersonalRepository ticketPersonalRepository;

    /* =========================================================
     * DB 확인하면서 아래 전역변수 바꿔서 테스트 하시면 됩니다 ^^
     * ========================================================= */

    // 1) create 테스트용
    private static String CREATE_WRITER = "홍길동";
    private static List<String> CREATE_RECEIVERS = List.of("김철수", "김영희", "한정연");

    // 2) listSent 테스트용
    private static String LIST_SENT_WRITER = "홍길동";

    // 3) readSent 테스트용
    private static Long READ_SENT_TNO = 1L;
    private static String READ_SENT_WRITER = "홍길동"; // DB 확인 후 바꿔가며 테스트

    // 4) deleteSent 테스트용
    private static Long DELETE_TNO = 1L;
    private static String DELETE_WRITER = "홍길동"; // DB 확인 후 바꿔가며 테스트



    @Test
    @Rollback(false)
    @DisplayName("티켓 생성 테스트 - Ticket과 TicketPersonal이 함께 생성되는지 확인")
    void create() {
        // given
        TicketCreateDTO req = TicketCreateDTO.builder()
                .title("업무 요청: API 연동 확인")
                .content("외부 API 연동/응답 스펙 확인 부탁드립니다.")
                .purpose("연동 점검")
                .requirement("응답 예시 + 에러 케이스 정리")
                .grade(TicketGrade.MIDDLE)
                .deadline(LocalDateTime.now().plusDays(3))
                .receivers(CREATE_RECEIVERS)
                .build();

        // when
        TicketSentListDTO created = ticketService.create(req, CREATE_WRITER);

        // then
        log.info("[CREATE] writer={}, receivers={}, created.tno={}",
                CREATE_WRITER, CREATE_RECEIVERS, created.getTno());

        assertNotNull(created, "create 결과 DTO가 null이면 안 됩니다.");
        assertNotNull(created.getTno(), "생성된 티켓 tno는 null이면 안 됩니다.");
        assertEquals(CREATE_WRITER, created.getWriter(), "writer가 일치해야 합니다.");
        assertEquals(req.getTitle(), created.getTitle(), "제목이 일치해야 합니다.");
        assertEquals(req.getContent(), created.getContent(), "내용이 일치해야 합니다.");
        assertEquals(req.getPurpose(), created.getPurpose(), "목적이 일치해야 합니다.");
        assertEquals(req.getRequirement(), created.getRequirement(), "요구사항이 일치해야 합니다.");
        assertEquals(req.getGrade(), created.getGrade(), "등급이 일치해야 합니다.");
        assertNotNull(created.getBirth(), "생성 시간(birth)이 null이면 안 됩니다.");

        // 수신인 개수 확인
        assertNotNull(created.getPersonals(), "personals 리스트가 null이면 안 됩니다.");
        assertEquals(CREATE_RECEIVERS.size(), created.getPersonals().size(), 
                "수신인 개수가 일치해야 합니다.");
        log.info("[CREATE] personals.size={}", created.getPersonals().size());

        // 각 수신인 확인
        List<String> createdReceivers = created.getPersonals().stream()
                .map(p -> p.getReceiver())
                .toList();
        assertTrue(createdReceivers.containsAll(CREATE_RECEIVERS), 
                "모든 수신인이 포함되어야 합니다.");
    }

    @Test
    @Rollback(false)
    @DisplayName("보낸 티켓 목록 조회 테스트 - 필터 없음")
    void listSent() {
        // given
        TicketFilterDTO filter = TicketFilterDTO.builder().build();
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "tno"));

        // when
        Page<TicketSentListDTO> page = ticketService.listSent(LIST_SENT_WRITER, filter, pageable);

        // then
        log.info("[LIST_SENT] writer={}, totalElements={}, totalPages={}, pageSize={}",
                LIST_SENT_WRITER, page.getTotalElements(), page.getTotalPages(), page.getSize());

        assertNotNull(page, "listSent 결과 page가 null이면 안 됩니다.");
        assertTrue(page.getTotalElements() >= 0, "전체 개수는 0 이상이어야 합니다.");

        page.getContent().forEach(dto -> {
            assertEquals(LIST_SENT_WRITER, dto.getWriter(), 
                    "모든 티켓의 작성자가 일치해야 합니다.");
            assertNotNull(dto.getTno(), "tno는 null이면 안 됩니다.");
            assertNotNull(dto.getTitle(), "제목은 null이면 안 됩니다.");
            log.info("[LIST_SENT_ITEM] tno={}, title={}, writer={}, personals={}",
                    dto.getTno(),
                    dto.getTitle(),
                    dto.getWriter(),
                    (dto.getPersonals() == null ? "null" : dto.getPersonals().size()));
        });
    }
    
    @Test
    @Rollback(false)
    @DisplayName("보낸 티켓 목록 조회 테스트 - 등급 필터 적용")
    void listSentWithFilter() {
        // given
        TicketFilterDTO filter = TicketFilterDTO.builder()
                .grade(TicketGrade.HIGH)
                .build();
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "tno"));

        // when
        Page<TicketSentListDTO> page = ticketService.listSent(LIST_SENT_WRITER, filter, pageable);

        // then
        log.info("[LIST_SENT_WITH_FILTER] writer={}, grade={}, totalElements={}",
                LIST_SENT_WRITER, filter.getGrade(), page.getTotalElements());

        assertNotNull(page, "필터링된 listSent 결과 page가 null이면 안 됩니다.");

        // 필터링된 결과의 모든 항목이 해당 grade인지 확인
        page.getContent().forEach(dto -> {
            assertEquals(filter.getGrade(), dto.getGrade(), 
                    "필터링된 결과의 grade가 일치해야 합니다.");
            assertEquals(LIST_SENT_WRITER, dto.getWriter(),
                    "작성자가 일치해야 합니다.");
            log.info("[FILTERED_ITEM] tno={}, grade={}", dto.getTno(), dto.getGrade());
        });
    }

    @Test
    @Rollback(false)
    @DisplayName("보낸 티켓 단일 조회 테스트 - 정상 케이스")
    void readSent() {
        // given
        Long tno = READ_SENT_TNO;
        String writer = READ_SENT_WRITER;

        // when & then
        try {
            TicketSentListDTO dto = ticketService.readSent(tno, writer);

            log.info("[READ_SENT] tno={}, writer={}, title={}, personals={}",
                    tno, writer, dto.getTitle(),
                    (dto.getPersonals() == null ? "null" : dto.getPersonals().size()));

            assertNotNull(dto, "readSent 결과가 null이면 안 됩니다.");
            assertEquals(tno, dto.getTno(), "조회한 tno가 일치해야 합니다.");
            assertEquals(writer, dto.getWriter(), "작성자가 일치해야 합니다.");
            assertNotNull(dto.getPersonals(), "personals 리스트가 null이면 안 됩니다.");
            assertTrue(dto.getPersonals().size() > 0, "수신인이 최소 1명 이상 있어야 합니다.");

        } catch (IllegalArgumentException e) {
            log.warn("[READ_SENT_FAIL] tno={}, writer={}, msg={}",
                    tno, writer, e.getMessage());
            fail("티켓 조회 실패: " + e.getMessage());
        }
    }

    @Test
    @Rollback(false)
    @DisplayName("보낸 티켓 단일 조회 테스트 - 권한 없는 사용자 접근 시 예외 발생")
    void readSent_Unauthorized() {
        // given
        Long tno = READ_SENT_TNO;
        String unauthorizedWriter = "권한없는사용자";

        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            ticketService.readSent(tno, unauthorizedWriter);
        }, "권한 없는 사용자가 조회할 때 예외가 발생해야 합니다.");
    }

    @Test
    @Rollback(false)
    @DisplayName("보낸 티켓 삭제 테스트 - Ticket과 TicketPersonal이 함께 삭제되는지 확인")
    void deleteSent() {
        // given
        Long tno = DELETE_TNO;
        String writer = DELETE_WRITER;

        // 삭제 전 확인
        boolean ticketExistsBefore = ticketRepository.existsById(tno);
        assertTrue(ticketExistsBefore, "삭제 전 Ticket이 존재해야 합니다.");

        long ticketPersonalCountBefore = ticketPersonalRepository.countByTicket_Tno(tno);
        log.info("[DELETE_SENT_BEFORE] tno={}, ticketPersonalCount={}", tno, ticketPersonalCountBefore);

        // when
        try {
            ticketService.deleteSent(tno, writer);
            log.info("[DELETE_SENT] deleted tno={}, writer={}", tno, writer);

            // then
            // Ticket 삭제 확인
            boolean ticketExistsAfter = ticketRepository.existsById(tno);
            assertFalse(ticketExistsAfter, "Ticket이 삭제되어야 합니다.");

            // TicketPersonal도 모두 삭제되었는지 확인 (cascade-orphanRemoval 동작 확인)
            long remainingCount = ticketPersonalRepository.countByTicket_Tno(tno);
            assertEquals(0, remainingCount, 
                    "Ticket 삭제 시 cascade-orphanRemoval로 TicketPersonal도 모두 삭제되어야 합니다.");
            log.info("[DELETE_SENT_AFTER] remainingTicketPersonalCount={}", remainingCount);

        } catch (IllegalArgumentException e) {
            log.warn("[DELETE_SENT_FAIL] tno={}, writer={}, msg={}",
                    tno, writer, e.getMessage());
            fail("삭제 테스트 실패: " + e.getMessage());
        }
    }

    @Test
    @Rollback(false)
    @DisplayName("보낸 티켓 삭제 테스트 - 권한 없는 사용자 삭제 시 예외 발생")
    void deleteSent_Unauthorized() {
        // given
        Long tno = DELETE_TNO;
        String unauthorizedWriter = "권한없는사용자";

        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            ticketService.deleteSent(tno, unauthorizedWriter);
        }, "권한 없는 사용자가 삭제할 때 예외가 발생해야 합니다.");
    }

    @Test
    @Rollback(false)
    @DisplayName("존재하지 않는 티켓 조회 시 예외 발생")
    void readSent_NotFound() {
        // given
        Long nonExistentTno = Long.valueOf(999999L);
        String writer = READ_SENT_WRITER;

        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            ticketService.readSent(nonExistentTno, writer);
        }, "존재하지 않는 티켓 조회 시 예외가 발생해야 합니다.");
    }

    @Test
    @Rollback(false)
    @DisplayName("존재하지 않는 티켓 삭제 시 예외 발생")
    void deleteSent_NotFound() {
        // given
        Long nonExistentTno = Long.valueOf(999999L);
        String writer = DELETE_WRITER;

        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            ticketService.deleteSent(nonExistentTno, writer);
        }, "존재하지 않는 티켓 삭제 시 예외가 발생해야 합니다.");
    }
}

