package com.desk.service;

import com.desk.domain.Member;
import com.desk.domain.Ticket;
import com.desk.domain.TicketPersonal;
import com.desk.dto.*;
import com.desk.repository.MemberRepository;
import com.desk.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final MemberRepository memberRepository;

    @Override
    public TicketSentListDTO create(TicketCreateDTO req, String writer) {
        // writer email로 Member 조회
        Member writerMember = memberRepository.findById(writer)
                .orElseThrow(() -> new IllegalArgumentException("Writer not found: " + writer));

        Ticket ticket = Ticket.builder()
                .title(req.getTitle())
                .content(req.getContent())
                .purpose(req.getPurpose())
                .requirement(req.getRequirement())
                .grade(req.getGrade())
                .deadline(req.getDeadline())
                .writer(writerMember)
                .build();

        // 수신인마다 TicketPersonal 1개씩 생성해서 연결
        for (String receiverEmail : req.getReceivers()) {
            Member receiverMember = memberRepository.findById(receiverEmail)
                    .orElseThrow(() -> new IllegalArgumentException("Receiver not found: " + receiverEmail));
            
            TicketPersonal tp = TicketPersonal.builder()
                    .receiver(receiverMember)
                    .build();
            ticket.addPersonal(tp); // setTicket(this)까지 같이 처리
        }

        Ticket saved = ticketRepository.save(ticket);
        return toSentDetailDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<TicketSentListDTO> listSent(String writer, TicketFilterDTO filter, PageRequestDTO pageRequestDTO) {
        // 기본 정렬을 tno,desc로 하되, DTO에 sort값이 있으면 그걸 우선 사용함
        Pageable pageable = pageRequestDTO.getPageable("tno");
        Page<Ticket> result = ticketRepository.findAllWithPersonalList(writer, filter, pageable);

        List<TicketSentListDTO> dtoList = result.getContent().stream()
                .map(this::toSentDetailDTO)
                .collect(Collectors.toList());

        return PageResponseDTO.<TicketSentListDTO>withAll()
                .dtoList(dtoList)
                .pageRequestDTO(pageRequestDTO)
                .totalCount(result.getTotalElements())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<TicketSentListDTO> listAll(String email, TicketFilterDTO filter, PageRequestDTO pageRequestDTO) {
        Pageable pageable = pageRequestDTO.getPageable("tno");
        Page<Ticket> result = ticketRepository.findAllAll(email, filter, pageable);

        List<TicketSentListDTO> dtoList = result.getContent().stream()
                .map(this::toSentDetailDTO)
                .collect(Collectors.toList());

        return PageResponseDTO.<TicketSentListDTO>withAll()
                .dtoList(dtoList)
                .pageRequestDTO(pageRequestDTO)
                .totalCount(result.getTotalElements())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public TicketSentListDTO readSent(Long tno, String writer) {
        // QueryDSL로 personalList fetch join (N+1 방지)
        Ticket ticket = ticketRepository.findWithPersonalListById(tno)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + tno));

        // 보낸 사람 검증
        if (!writer.equals(ticket.getWriter().getEmail())) {
            throw new IllegalArgumentException("Not allowed to read this ticket.");
        }

        return toSentDetailDTO(ticket);
    }

    @Override
    public void deleteSent(Long tno, String writer) {
        Ticket ticket = ticketRepository.findById(tno)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + tno));

        if (!writer.equals(ticket.getWriter().getEmail())) {
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
                .writer(t.getWriter().getEmail())
                .personals(
                        t.getPersonalList().stream()
                                .map(p -> TicketStateDTO.builder()
                                        .pno(p.getPno())
                                        .receiver(p.getReceiver().getEmail())
                                        .isread(p.isIsread())
                                        .state(p.getState())
                                        .build())
                                .collect(Collectors.toList())
                )
                .build();
    }
}
