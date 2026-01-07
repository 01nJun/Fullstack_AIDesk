package com.desk.service;

import com.desk.domain.Member;
import com.desk.domain.Ticket;
import com.desk.domain.TicketPin;
import com.desk.dto.TicketPinResponseDTO;
import com.desk.repository.MemberRepository;
import com.desk.repository.TicketPinRepository;
import com.desk.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class TicketPinServiceImpl implements TicketPinService {

    private final TicketPinRepository pinRepository;
    private final TicketRepository ticketRepository;
    private final MemberRepository memberRepository;

    @Override
    public List<TicketPinResponseDTO> togglePin(String email, Long tno) {
        Optional<TicketPin> found = pinRepository.findByMemberEmailAndTicketTno(email, tno);

        if (found.isPresent()) {
            pinRepository.delete(found.get());
        } else {
            Ticket ticket = ticketRepository.findById(tno).orElseThrow();
            Member member = memberRepository.findById(email).orElseThrow();
            pinRepository.save(TicketPin.builder().member(member).ticket(ticket).build());
        }
        return getPinList(email);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketPinResponseDTO> getPinList(String email) {
        return pinRepository.findByMemberEmail(email).stream()
                .map(this::entityToDTO)
                .collect(Collectors.toList());
    }

    private TicketPinResponseDTO entityToDTO(TicketPin pin) {
        return TicketPinResponseDTO.builder()
                .pino(pin.getPino())
                .tno(pin.getTicket().getTno())
                .title(pin.getTicket().getTitle())
                .grade(pin.getTicket().getGrade())
                .build();
    }
}