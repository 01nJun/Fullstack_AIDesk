package com.desk.service;

import com.desk.domain.Ticket;
import com.desk.domain.TicketPersonal;
import com.desk.domain.TicketState;
import com.desk.dto.PageRequestDTO;
import com.desk.dto.PageResponseDTO;
import com.desk.dto.TicketFilterDTO;
import com.desk.dto.TicketReceivedListDTO;
import com.desk.repository.TicketPersonalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PersonalTicketServiceImpl implements PersonalTicketService {
    /*
    * 받은 티켓함을 담당하는 서비스입니다.
    * 내가 받은 티켓 목록 조회
    * 받은 티켓 상세 조회(읽음처리 동시에)
    * 받은 티켓 상태 변경(진행중 -> 완료 등...)
    */

    // 리포지토리 불러와서 JPA 사용 (동적쿼리도...)
    private final TicketPersonalRepository ticketPersonalRepository;

    // 받은 목록 조회
    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<TicketReceivedListDTO> listRecieveTicket(String receiver, TicketFilterDTO filter, PageRequestDTO pageRequestDTO) {
        // 기본 정렬을 pno,desc로 설정
        Pageable pageable = pageRequestDTO.getPageable("pno");
        Page<TicketPersonal> result = ticketPersonalRepository.findAllWithTicket(receiver, filter, pageable);

        List<TicketReceivedListDTO> dtoList = result.getContent().stream()
                .map(this::toRecieveTicketDTO)
                .collect(Collectors.toList());

        return PageResponseDTO.<TicketReceivedListDTO>withAll()
                .dtoList(dtoList)
                .pageRequestDTO(pageRequestDTO)
                .totalCount(result.getTotalElements())
                .build();
    }

    // 단건 조회, 읽음처리, 권한체크(receiver로)
    @Override
    public TicketReceivedListDTO readRecieveTicket(Long tpno, String receiver, boolean markAsRead) {
        // QueryDSL로 ticket fetch join (N+1 방지)
        TicketPersonal tp = ticketPersonalRepository.findWithTicketByPno(tpno)
                .orElseThrow(() -> new IllegalArgumentException("Inbox not found: " + tpno));

        if (!receiver.equals(tp.getReceiver().getEmail())) {
            throw new IllegalArgumentException("Not allowed to read this inbox ticket.");
        }

        // 이게 읽음처리입니다
        // 처음 read하면 무조건 읽음되니까...
        if (markAsRead && !tp.isIsread()) {
            tp.changeRead(true);
        }

        // DTO 변환 메서드는 맨 아래에
        return toRecieveTicketDTO(tp);
    }

    // tno로 읽고 싶을 때
    // receiver + tno 조합으로 tpno를 구해서 읽어옴
    // 실제 읽는 로직은 위의 readRecieveTicket
    @Override
    public TicketReceivedListDTO readRecieveTicketByTno(Long tno, String receiver, boolean markAsRead) {
        Long tpno = ticketPersonalRepository.findPnoByReceiverAndTno(receiver, tno)
                .orElseThrow(() -> new IllegalArgumentException("Inbox not found by receiver+tno. receiver=" + receiver + ", tno=" + tno));
        
        return this.readRecieveTicket(tpno, receiver, markAsRead);
    }

    // 상태변경
    @Override
    public TicketReceivedListDTO changeState(Long tpno, String receiver, TicketState state) {
        // QueryDSL로 ticket fetch join (N+1 방지)
        // 트랜잭션 중 아래 코드 실행되면 tp는 영속 상태가 됨
        TicketPersonal tp = ticketPersonalRepository.findWithTicketByPno(tpno)
                .orElseThrow(() -> new IllegalArgumentException("Inbox not found: " + tpno));

        if (!receiver.equals(tp.getReceiver().getEmail())) {
            throw new IllegalArgumentException("Not allowed to change state.");
        }

        // 메모리에서 객체 필드 값 바꾸기
        tp.changeState(state);
        // dto로 변환
        return toRecieveTicketDTO(tp);
    } // 트랜잭션 끝나면 더티체킹(트랜잭션, 영속, 값 바뀜) 으로 update 됨

    private TicketReceivedListDTO toRecieveTicketDTO(TicketPersonal tp) {
        Ticket t = tp.getTicket();
        return TicketReceivedListDTO.builder()
                .pno(tp.getPno())
                .receiver(tp.getReceiver().getEmail())
                .isread(tp.isIsread())
                .state(tp.getState())
                .tno(t.getTno())
                .title(t.getTitle())
                .content(t.getContent())
                .purpose(t.getPurpose())
                .requirement(t.getRequirement())
                .grade(t.getGrade())
                .birth(t.getBirth())
                .deadline(t.getDeadline())
                .writer(t.getWriter().getEmail())
                .build();
    }
}
