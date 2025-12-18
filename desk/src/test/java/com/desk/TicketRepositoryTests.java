package com.desk;

import java.time.LocalDateTime;

import com.desk.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.desk.domain.Ticket;

import lombok.extern.log4j.Log4j2;

@SpringBootTest
@Log4j2
public class TicketRepositoryTests {

    @Autowired
    private TicketRepository ticketRepository;

    @Test
    public void test1() {
        log.info("----------------------------");
        log.info(ticketRepository);
    }

    // 1) 더미데이터 생성
    @Test
    public void testInsert() {

        for (int i = 1; i <= 100; i++) {

            Ticket ticket = Ticket.builder()
                    .tTitle("티켓 제목..." + i)
                    .tContent("요청 요약(한 문장)..." + i)
                    .tPurpose("요청 배경/목적..." + i)
                    .tRequirement("- 요구사항1..." + i + "\n- 요구사항2..." + i)
                    .tGrade(i % 3 == 0 ? "P1" : (i % 3 == 1 ? "P2" : "P3"))
                    .tDeadline(LocalDateTime.now().plusDays(i % 10)) // 마감일 임의 분산
                    .tWriter("user00")
                    // tBirth는 @PrePersist로 자동 세팅(엔티티에 설정해둔 경우)
                    .build();

            ticketRepository.save(ticket);
        }
    }

    // 2) 단건 조회
    @Test
    public void testRead() {

        Long tno = 33L;

        java.util.Optional<Ticket> result = ticketRepository.findById(tno);

        Ticket ticket = result.orElseThrow();

        log.info(ticket);
    }

    // 3) 수정
    @Test
    public void testModify() {

        Long tno = 50L;

        java.util.Optional<Ticket> result = ticketRepository.findById(tno);

        Ticket ticket = result.orElseThrow();

        // Todo처럼 change 메서드가 있으면 그걸 쓰고,
        // 없으면 updateFromDto(...) 같은 한 방 메서드를 호출하면 됨.

        ticket.changeTitle("수정된 제목 50...");
        ticket.changeContent("수정된 요약 50...");
        ticket.changePurpose("수정된 목적 50...");
        ticket.changeRequirement("- 요구사항 수정1\n- 요구사항 수정2");
        ticket.changeGrade("P1");
        ticket.changeDeadline(LocalDateTime.now().plusDays(7));

        ticketRepository.save(ticket);

        log.info("MODIFIED: " + ticket);
    }

    // 4) 삭제
    @Test
    public void testDelete() {

        Long tno = 1L;

        ticketRepository.deleteById(tno);

        log.info("DELETED tno=" + tno);
    }

    // 5) 페이징 조회
    @Test
    public void testPaging() {

        Pageable pageable = PageRequest.of(0, 10, Sort.by("tno").descending());

        Page<Ticket> result = ticketRepository.findAll(pageable);

        log.info("TOTAL: " + result.getTotalElements());
        result.getContent().forEach(ticket -> log.info(ticket));
    }
}
