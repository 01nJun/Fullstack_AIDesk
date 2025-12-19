package com.desk;

import com.desk.domain.Ticket;
import com.desk.domain.TicketGrade;
import com.desk.domain.TicketPersonal;
import com.desk.domain.TicketState;
import com.desk.repository.TicketPersonalRepository;
import com.desk.repository.TicketRepository;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.Optional;

@SpringBootTest
@Log4j2
public class TicketRepositoryTests {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private TicketPersonalRepository ticketPersonalRepository;

    @Test
    public void test() {
        log.info("----------------------------");
        log.info(ticketRepository);
        log.info(ticketPersonalRepository);
    }

    @Test
    public void testInsert() {

        for (int i = 1; i <= 30; i++) {

            TicketGrade grade = switch (i % 4) {
                case 0 -> TicketGrade.LOW;
                case 1 -> TicketGrade.MIDDLE;
                case 2 -> TicketGrade.HIGH;
                default -> TicketGrade.URGENT;
            };

            Ticket ticket = Ticket.builder()
                    .tTitle("Ticket Title..." + i)
                    .tContent("Ticket Content..." + i)
                    .tPurpose("Purpose..." + (i % 5))
                    .tRequirement("Requirement..." + i)
                    .tGrade(grade)
                    .tDeadline(LocalDateTime.now().plusDays(i % 10).withSecond(0).withNano(0))
                    .tWriter("user" + String.format("%02d", (i % 3) + 1))
                    .build();

            // 수신자 1~3명
            int receiverCount = (i % 3) + 1;
            for (int r = 1; r <= receiverCount; r++) {

                boolean read = (r % 2 == 0);
                TicketState state = switch ((i + r) % 4) {
                    case 0 -> TicketState.NEW;
                    case 1 -> TicketState.IN_PROGRESS;
                    case 2 -> TicketState.NEED_INFO;
                    default -> TicketState.DONE;
                };

                TicketPersonal personal = TicketPersonal.builder()
                        .tpReceiver("receiver" + ((i + r) % 5 + 1)) // receiver1~5
                        .tpRead(read)
                        .tpState(state)
                        .build();

                ticket.addPersonal(personal); // setTicket(this) + 리스트 추가
            }

            ticketRepository.save(ticket);
        }

        log.info("Inserted 30 tickets.");
    }

    @Test
    public void testRead() {

        Long tno = 1L;

        Ticket ticket = ticketRepository.findById(tno).orElseThrow();
        log.info(ticket);

        Pageable pageable = PageRequest.of(0, 20, Sort.by("tpno").ascending());
        Page<TicketPersonal> personals =
                ticketPersonalRepository.findByTpReceiver("receiver2", pageable);

        log.info("personal count: " + personals.getTotalElements());
        personals.forEach(p -> log.info("personal: " + p));
    }

    @Test
    public void testModify() {

        Long tno = 1L;

        Optional<Ticket> result = ticketRepository.findById(tno);
        Ticket ticket = result.orElseThrow();

        ticket.changeTitle("Modified Title...");
        ticket.changeContent("Modified Content...");
        ticket.changePurpose("Modified Purpose...");
        ticket.changeRequirement("Modified Requirement...");
        ticket.changeGrade(TicketGrade.URGENT);
        ticket.changeDeadline(LocalDateTime.now().plusDays(30).withSecond(0).withNano(0));

        ticketRepository.save(ticket);

        log.info("Modified ticket: " + ticket.getTno());
    }

    @Test
    public void testDelete() {

        Long tno = 30L;

        ticketRepository.deleteById(tno);

        log.info("Deleted ticket: " + tno);
    }

    @Test
    public void testPaging() {

        Pageable pageable = PageRequest.of(
                0,
                10,
                Sort.by("tno").descending()
        );

        Page<Ticket> result = ticketRepository.findAll(pageable);

        log.info("total elements: " + result.getTotalElements());
        log.info("total pages   : " + result.getTotalPages());
        log.info("page number   : " + result.getNumber());
        log.info("page size     : " + result.getSize());

        result.getContent().forEach(ticket -> log.info(ticket));
    }

    @Test
    public void testInboxPagingByReceiver() {

        String receiver = "receiver1";

        Pageable pageable = PageRequest.of(
                0,
                10,
                Sort.by("tpno").descending()
        );

        Page<com.desk.domain.TicketPersonal> result =
                ticketPersonalRepository.findByTpReceiver(receiver, pageable);

        log.info("receiver: " + receiver);
        log.info("total elements: " + result.getTotalElements());

        result.getContent().forEach(tp -> {
            log.info("tpno=" + tp.getTpno()
                    + ", read=" + tp.isTpRead()
                    + ", state=" + tp.getTpState()
                    + ", ticket.tno=" + (tp.getTicket() != null ? tp.getTicket().getTno() : null));
        });
    }
}
