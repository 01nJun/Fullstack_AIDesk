package com.desk.repository;

import com.desk.domain.Ticket;
import com.desk.domain.TicketGrade;
import com.desk.domain.TicketPersonal;
import com.desk.domain.Member;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@SpringBootTest
public class TicketRepositoryTests {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @Transactional
    @Commit
    public void testInsert60Tickets() {
        // 1. 멤버가 없으면 생성
        createMemberIfNotExist("user1@desk.com", "User1");
        createMemberIfNotExist("user9@desk.com", "User9");

        Member user1 = memberRepository.findById("user1@desk.com").get();
        Member user9 = memberRepository.findById("user9@desk.com").get();

        // 2. user1 -> user9 (30개)
        for (int i = 1; i <= 30; i++) {
            Ticket ticket = Ticket.builder()
                    .title("보낸 티켓 " + i)
                    .content("내용 " + i)
                    .writer(user1)
                    .grade(TicketGrade.MIDDLE)
                    .deadline(LocalDateTime.now().plusDays(i)) // 마감일 다양하게 설정
                    .build();
            ticket.addPersonal(TicketPersonal.builder().receiver(user9).build());
            ticketRepository.save(ticket);
        }

        // 3. user9 -> user1 (30개)
        for (int i = 1; i <= 30; i++) {
            Ticket ticket = Ticket.builder()
                    .title("받은 티켓 " + i)
                    .content("내용 " + i)
                    .writer(user9)
                    .grade(TicketGrade.HIGH)
                    .deadline(LocalDateTime.now().plusDays(i))
                    .build();
            ticket.addPersonal(TicketPersonal.builder().receiver(user1).build());
            ticketRepository.save(ticket);
        }
    }

    private void createMemberIfNotExist(String email, String nickname) {
        if (memberRepository.findById(email).isEmpty()) {
            Member member = Member.builder()
                    .email(email)
                    .pw("1111")
                    .nickname(nickname)
                    .build();
            memberRepository.save(member);
        }
    }
}