package com.desk.service;

import com.desk.domain.Ticket;
import com.desk.dto.TicketDTO;
import com.desk.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;

    @Override
    public TicketDTO register(TicketDTO dto) {
        Ticket ticket = Ticket.builder()
                .tTitle(dto.tTitle())
                .tContent(dto.tContent())
                .tPurpose(dto.tPurpose())
                .tRequirement(dto.tRequirement())
                .tGrade(dto.tGrade())
                .tDeadline(dto.tDeadline())
                .tWriter(dto.tWriter())
                // tBirth는 @PrePersist에서 자동
                .build();

        Ticket saved = ticketRepository.save(ticket);
        return toDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketDTO read(Long tno) {
        Ticket t = ticketRepository.findById(tno)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + tno));
        return toDTO(t);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketDTO> list() {
        return ticketRepository.findAll()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public TicketDTO modify(Long tno, TicketDTO dto) {
        Ticket t = ticketRepository.findById(tno)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + tno));

        // 작성자(tWriter)는 보통 변경 금지 → 기존 값 유지
        t.updateFromDto(
                dto.tTitle(),
                dto.tContent(),
                dto.tPurpose(),
                dto.tRequirement(),
                dto.tGrade(),
                dto.tDeadline()
        );

        // 영속 상태라 save 없이도 반영되지만, 명확히 하려면 save해도 됨.
        return toDTO(t);
    }

    @Override
    public void remove(Long tno) {
        if (!ticketRepository.existsById(tno)) {
            throw new IllegalArgumentException("Ticket not found: " + tno);
        }
        ticketRepository.deleteById(tno);
    }

    private TicketDTO toDTO(Ticket t) {
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
