package com.desk.service;

import com.desk.domain.Ticket;
import com.desk.domain.TicketPersonal;
import com.desk.dto.*;
import com.desk.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;

    @Override
    public TicketSentListDTO create(TicketCreateDTO req, String writer) {

        Ticket ticket = Ticket.builder()
                .title(req.getTitle())
                .content(req.getContent())
                .purpose(req.getPurpose())
                .requirement(req.getRequirement())
                .grade(req.getGrade())
                .deadline(req.getDeadline())
                .writer(writer)
                .build();

        // 수신인마다 TicketPersonal 1개씩 생성해서 연결
        for (String r : req.getReceivers()) {
            TicketPersonal tp = TicketPersonal.builder()
                    .receiver(r)
                    .build();
            ticket.addPersonal(tp); // setTicket(this)까지 같이 처리
        }

        Ticket saved = ticketRepository.save(ticket);
        return toSentDetailDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TicketSentListDTO> listSent(String writer, TicketFilterDTO filter, Pageable pageable) {
        // QueryDSL로 동적 필터링 + fetch join (N+1 방지)
        Page<Ticket> page = ticketRepository.findAllWithPersonalList(writer, filter, pageable);
        return page.map(this::toSentDetailDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketSentListDTO readSent(Long tno, String writer) {
        // QueryDSL로 personalList fetch join (N+1 방지)
        Ticket ticket = ticketRepository.findWithPersonalListById(tno)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + tno));

        // 보낸 사람 검증
        if (!writer.equals(ticket.getWriter())) {
            throw new IllegalArgumentException("Not allowed to read this ticket.");
        }

        return toSentDetailDTO(ticket);
    }

    @Override
    public void deleteSent(Long tno, String writer) {
        Ticket ticket = ticketRepository.findById(tno)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + tno));

        if (!writer.equals(ticket.getWriter())) {
            throw new IllegalArgumentException("Not allowed to delete this ticket.");
        }

        // Ticket 삭제 시 TicketPersonal도 함께 삭제
        ticketRepository.delete(ticket);
    }

    private TicketSentListDTO toSentDetailDTO(Ticket t) {
        return TicketSentListDTO.builder()
                .tno(t.getTno())
                .title(t.getTitle())
                .content(t.getContent())
                .purpose(t.getPurpose())
                .requirement(t.getRequirement())
                .grade(t.getGrade())
                .birth(t.getBirth())
                .deadline(t.getDeadline())
                .writer(t.getWriter())
                .personals(
                        t.getPersonalList().stream()
                                .map(p -> TicketStateDTO.builder()
                                        .pno(p.getPno())
                                        .receiver(p.getReceiver())
                                        .isread(p.isIsread())
                                        .state(p.getState())
                                        .build())
                                .collect(Collectors.toList())
                )
                .build();
    }
}
