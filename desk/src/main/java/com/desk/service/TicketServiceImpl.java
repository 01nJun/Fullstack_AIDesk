package com.desk.service;

import com.desk.domain.Ticket;
import com.desk.dto.TicketDTO;
import com.desk.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional // 기본적으로 서비스 메서드들이 트랜잭션 범위 안에서 실행되도록 보장(DB 일관성 유지)
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<TicketDTO> listSendTicket(String writer, Pageable pageable) {
        // 작성자 기준 페이징 조회
        return ticketRepository.findBytWriter(writer, pageable)
                .map(this::toTicketDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketDTO readSendTicket(Long tno, String writer) {
        // 내가 보낸 티켓 조회
        Ticket t = ticketRepository.findById(tno)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + tno));

        return toTicketDTO(t);
    }

    @Override
    public TicketDTO modifySendTicket(Long tno, String writer, TicketDTO dto) {
        // 내가 보낸 티켓 내용을 수정
        Ticket t = ticketRepository.findById(tno)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + tno));

        t.changeTitle(dto.getTTitle());
        t.changeContent(dto.getTContent());
        t.changePurpose(dto.getTPurpose());
        t.changeRequirement(dto.getTRequirement());
        t.changeGrade(dto.getTGrade());
        t.changeDeadline(dto.getTDeadline());

        return toTicketDTO(t);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TicketDTO> listAllTicket(Pageable pageable) {
        // 전체 티켓 목록 페이징 조회
        // 나중에는 관리자만...
        return ticketRepository.findAll(pageable).map(this::toTicketDTO);
    }

    @Override
    public TicketDTO registerNewTicket(TicketDTO dto) {
        // 새 티켓 등록
        Ticket t = Ticket.builder()
                .tTitle(dto.getTTitle())
                .tContent(dto.getTContent())
                .tPurpose(dto.getTPurpose())
                .tRequirement(dto.getTRequirement())
                .tGrade(dto.getTGrade())
                .tDeadline(dto.getTDeadline())
                .tWriter(dto.getTWriter())
                .build();

        return toTicketDTO(ticketRepository.save(t));
    }

    @Override
    public void removeTicket(Long tno) {
        // 티켓 삭제
        ticketRepository.deleteById(tno);
    }

    private TicketDTO toTicketDTO(Ticket t) {
        return new TicketDTO(
                t.getTno(),
                t.getTTitle(),
                t.getTContent(),
                t.getTPurpose(),
                t.getTRequirement(),
                t.getTGrade(),
                t.getTBirth(),
                t.getTDeadline(),
                t.getTWriter()
        );
    }
}
