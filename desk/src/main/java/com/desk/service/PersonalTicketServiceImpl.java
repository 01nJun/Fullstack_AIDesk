package com.desk.service;

import com.desk.domain.Ticket;
import com.desk.domain.TicketPersonal;
import com.desk.domain.TicketState;
import com.desk.dto.PersonalTicketDTO;
import com.desk.repository.TicketPersonalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional //트랜잭션 단위 관리
public class PersonalTicketServiceImpl implements PersonalTicketService {

    private final TicketPersonalRepository ticketPersonalRepository;

    @Override
    @Transactional(readOnly = true)
    // 내가 받은 티켓 receiver 기준 페이징 조회
    public Page<PersonalTicketDTO> listPersonalTicket(String receiver, Pageable pageable) {
        return ticketPersonalRepository.findWithTicketByTpReceiver(receiver, pageable)
                .map(this::toPersonalDTO);
    }

    @Override
    // 내가 받은 티켓 조회/조회 시 읽음 처리
    public PersonalTicketDTO readPersonalTicket(Long tpno, String receiver) {
        TicketPersonal tp = ticketPersonalRepository.findWithTicketByTpno(tpno)
                .orElseThrow(() -> new IllegalArgumentException("Inbox not found: " + tpno));

        if (!tp.isTpRead()) tp.changeRead(true);

        return toPersonalDTO(tp);
    }

    @Override
    // 내가 받은 티켓의 진행 상태 변경
    public PersonalTicketDTO changePersonalTicketState(Long tpno, String receiver, TicketState state) {
        TicketPersonal tp = ticketPersonalRepository.findWithTicketByTpno(tpno)
                .orElseThrow(() -> new IllegalArgumentException("Inbox not found: " + tpno));

        tp.changeState(state);

        return toPersonalDTO(tp);
    }

    private PersonalTicketDTO toPersonalDTO(TicketPersonal tp) {
        Ticket t = tp.getTicket();
        PersonalTicketDTO dto = new PersonalTicketDTO();

        dto.setTpno(tp.getTpno());
        dto.setTpReceiver(tp.getTpReceiver());
        dto.setTpRead(tp.isTpRead());
        dto.setTpState(tp.getTpState());

        dto.setTno(t.getTno());
        dto.setTTitle(t.getTTitle());
        dto.setTContent(t.getTContent());
        dto.setTPurpose(t.getTPurpose());
        dto.setTRequirement(t.getTRequirement());
        dto.setTGrade(t.getTGrade());
        dto.setTBirth(t.getTBirth());
        dto.setTDeadline(t.getTDeadline());
        dto.setTWriter(t.getTWriter());

        return dto;
    }
}
