package com.desk.service;

import com.desk.domain.Ticket;
import com.desk.domain.TicketGrade;
import com.desk.domain.TicketPersonal;
import com.desk.domain.TicketState;
import com.desk.dto.TicketDTO;
import com.desk.repository.TicketPersonalRepository;
import com.desk.repository.TicketRepository;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.*;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Log4j2
class TicketServiceTests {

    @Autowired
    private TicketService ticketService;

    // 검증/seed용 (Service 테스트지만, 검증은 repo로 하는 게 가장 확실함)
    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private TicketPersonalRepository ticketPersonalRepository;

    @Test
    @DisplayName("0) DI 확인")
    void wiring() {
        assertNotNull(ticketService);
        assertNotNull(ticketRepository);
        assertNotNull(ticketPersonalRepository);
    }

    @Test
    @Transactional
    @DisplayName("1) registerNewTicket: 티켓 등록")
    void registerNewTicket() {

        String writer = "user01";

        TicketDTO dto = new TicketDTO();
        dto.setTTitle("Service Register Title");
        dto.setTContent("Service Register Content");
        dto.setTPurpose("Purpose");
        dto.setTRequirement("Requirement");
        dto.setTGrade(TicketGrade.HIGH);
        dto.setTDeadline(LocalDateTime.now().plusDays(3).withSecond(0).withNano(0));
        dto.setTWriter(writer);

        TicketDTO saved = ticketService.registerNewTicket(dto);

        assertNotNull(saved);
        assertNotNull(saved.getTno());

        // writer 권한 체크 포함 조회
        TicketDTO read = ticketService.readSendTicket(saved.getTno(), writer);
        assertEquals(saved.getTno(), read.getTno());
        assertEquals(writer, read.getTWriter());

        log.info("registered tno={}", saved.getTno());
    }

    @Test
    @Transactional
    @DisplayName("2) listSendTicket: 내가 보낸 티켓 목록 페이징")
    void listSendTicket() {

        String tWriter = "user02";

        // seed 3건
        for (int i = 0; i < 3; i++) {
            ticketRepository.save(seedTicketEntity(tWriter, "listSendTicket-" + i, List.of("receiver1")));
        }

        Pageable pageable = PageRequest.of(0, 10, Sort.by("tno").descending());
        Page<TicketDTO> page = ticketService.listSendTicket(tWriter, pageable);

        assertNotNull(page);
        assertTrue(page.getTotalElements() >= 3);

        log.info("tWriter={}, totalElements={}", tWriter, page.getTotalElements());
        page.getContent().forEach(it -> log.info("item={}", it));
    }

    @Test
    @Transactional
    @DisplayName("3) readSendTicket: 내가 보낸 티켓 단건 조회(권한 writer)")
    void readSendTicket() {

        String writer = "user03";
        Ticket ticket = ticketRepository.save(seedTicketEntity(writer, "readSendTicket", List.of("receiver2")));
        Long tno = ticket.getTno();

        TicketDTO dto = ticketService.readSendTicket(tno, writer);

        assertNotNull(dto);
        assertEquals(tno, dto.getTno());
        assertEquals(writer, dto.getTWriter());

        log.info("read tno={}, writer={}", tno, writer);
    }

    @Test
    @Transactional
    @DisplayName("4) modifySendTicket: 내가 보낸 티켓 수정(권한 writer)")
    void modifySendTicket() {

        String writer = "user04";
        Ticket ticket = ticketRepository.save(seedTicketEntity(writer, "before-modify", List.of("receiver3")));
        Long tno = ticket.getTno();

        TicketDTO modify = new TicketDTO();
        modify.setTTitle("after-modify-title");
        modify.setTContent("after-modify-content");
        modify.setTPurpose("after-modify-purpose");
        modify.setTRequirement("after-modify-requirement");
        modify.setTGrade(TicketGrade.URGENT);
        modify.setTDeadline(LocalDateTime.now().plusDays(30).withSecond(0).withNano(0));
        modify.setTWriter(writer); // 서비스에서 writer 비교할 수 있으니 동일하게

        TicketDTO updated = ticketService.modifySendTicket(tno, writer, modify);

        assertNotNull(updated);
        assertEquals(tno, updated.getTno());
        assertEquals("after-modify-title", updated.getTTitle());
        assertEquals(TicketGrade.URGENT, updated.getTGrade());

        log.info("modified tno={}", tno);
    }

    @Test
    @DisplayName("5) listAllTicket: 전체 티켓 조회(관리자용) 페이징")
    void listAllTicket() {

        Pageable pageable = PageRequest.of(0, 10, Sort.by("tno").descending());
        Page<TicketDTO> page = ticketService.listAllTicket(pageable);

        assertNotNull(page);
        log.info("totalElements={}", page.getTotalElements());
    }

    @Test
    @Transactional
    @DisplayName("6) removeTicket: 티켓 삭제 시 개인 티켓도 같이 삭제되는지")
    void removeTicket() {

        String writer = "user05";
        String receiverKey = "receiver_del_x";

        Ticket ticket = ticketRepository.save(
                seedTicketEntity(writer, "delete-target", List.of(receiverKey, receiverKey))
        );
        Long tno = ticket.getTno();

        // 삭제 전: receiver로 개인티켓 존재 확인 (ticket까지 fetch해서 tno 매칭 확인)
        Page<TicketPersonal> beforePage =
                ticketPersonalRepository.findWithTicketByTpReceiver(receiverKey, PageRequest.of(0, 100));

        long beforeForThisTicket = beforePage.getContent().stream()
                .filter(tp -> tp.getTicket() != null && tno.equals(tp.getTicket().getTno()))
                .count();

        assertTrue(beforeForThisTicket >= 1);

        // 서비스 삭제
        ticketService.removeTicket(tno);

        assertTrue(ticketRepository.findById(tno).isEmpty());

        Page<TicketPersonal> afterPage =
                ticketPersonalRepository.findWithTicketByTpReceiver(receiverKey, PageRequest.of(0, 100));

        long afterForThisTicket = afterPage.getContent().stream()
                .filter(tp -> tp.getTicket() != null && tno.equals(tp.getTicket().getTno()))
                .count();

        assertEquals(0, afterForThisTicket);

        log.info("deleted tno={}, personals(before)={}, personals(after)={}", tno, beforeForThisTicket, afterForThisTicket);
    }

    // -------------------------
    // seed helper
    // -------------------------
    private Ticket seedTicketEntity(String writer, String titleSuffix, List<String> receivers) {

        Ticket ticket = Ticket.builder()
                .tTitle("Seed-" + titleSuffix)
                .tContent("Seed content")
                .tPurpose("Seed purpose")
                .tRequirement("Seed requirement")
                .tGrade(TicketGrade.MIDDLE)
                .tDeadline(LocalDateTime.now().plusDays(5).withSecond(0).withNano(0))
                .tWriter(writer)
                .build();

        if (receivers != null) {
            for (String r : receivers) {
                TicketPersonal tp = TicketPersonal.builder()
                        .tpReceiver(r)
                        .tpRead(false)
                        .tpState(TicketState.NEW)
                        .build();
                ticket.addPersonal(tp);
            }
        }

        return ticket;
    }
}
