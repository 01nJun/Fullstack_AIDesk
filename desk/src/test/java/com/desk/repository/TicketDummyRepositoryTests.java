package com.desk.repository;

import com.desk.domain.Department;
import com.desk.domain.Member;
import com.desk.domain.Ticket;
import com.desk.domain.TicketGrade;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

@SpringBootTest
@Log4j2
public class TicketDummyRepositoryTests {
    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    public void testInsert() {

        for (int i = 0; i < 10; i++) {
            final int idx = i;

            String email = "user" + idx + "@desk.com";

            Member member = memberRepository.findById(email)
                    .orElseGet(() -> memberRepository.save(
                            Member.builder()
                                    .email(email)
                                    .pw("1111") // (선택) 인코딩 권장
                                    .nickname("유저" + idx)
                                    .department(Department.DEVELOPMENT) // ✅ NULL 방지
                                    .social(false)
                                    .build()
                    ));

            Ticket ticket = Ticket.builder()
                    .title("제목" + idx)
                    .content("글 내용" + idx)
                    .writer(member)
                    .purpose("목적" + idx)
                    .requirement("요구사항" + idx)
                    .grade(TicketGrade.HIGH)
                    .birth(LocalDateTime.now())
                    .deadline(LocalDateTime.now().plusDays(idx))
                    .build();

            ticket.addDocumentString("document1_" + idx + ".doc");
            ticket.addDocumentString("document2_" + idx + ".doc");

            ticketRepository.save(ticket);
        }
    }


}
