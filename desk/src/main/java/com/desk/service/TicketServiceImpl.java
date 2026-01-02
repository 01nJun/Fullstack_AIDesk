package com.desk.service;

import com.desk.domain.Member;
import com.desk.domain.Ticket;
import com.desk.domain.TicketFile;
import com.desk.domain.TicketPersonal;
import com.desk.dto.*;
import com.desk.repository.MemberRepository;
import com.desk.repository.TicketFileRepository;
import com.desk.repository.TicketRepository;
import com.desk.util.CustomFileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
@Transactional
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final MemberRepository memberRepository;
    private final TicketFileRepository ticketFileRepository;
    private final CustomFileUtil fileUtil;

    // 기존 create (파일 없는 버전 - 호환성 유지)
    @Override
    public TicketSentListDTO create(TicketCreateDTO req, String writer) {
        return createWithFiles(req, writer, null);
    }

    @Override
    @Transactional
    public TicketSentListDTO createWithFiles(TicketCreateDTO req, String writer, List<MultipartFile> files) {
        try {
            log.info("--- 티켓 저장 프로세스 시작 ---");

            // 1. 작성자 체크
            Member writerMember = memberRepository.findById(writer)
                    .orElseThrow(() -> new IllegalArgumentException("작성자를 찾을 수 없음: " + writer));

            // 2. 티켓 기본 정보 생성 및 저장
            Ticket ticket = Ticket.builder()
                    .title(req.getTitle()).content(req.getContent())
                    .purpose(req.getPurpose()).requirement(req.getRequirement())
                    .grade(req.getGrade()).deadline(req.getDeadline())
                    .writer(writerMember).build();

            // 수신자 추가
            List<String> receiverEmails = req.getReceivers();
            if (req.getReceivers() != null) {
                for (String email : req.getReceivers()) {
                    if(email == null || email.isBlank()) continue;
                    Member receiver = memberRepository.findById(email)
                            .orElseThrow(() -> new IllegalArgumentException("수신자 없음: " + email));
                    ticket.addPersonal(TicketPersonal.builder().receiver(receiver).build());
                }
            }

            Ticket savedTicket = ticketRepository.save(ticket);
            log.info("티켓 기본 저장 완료: tno = {}", savedTicket.getTno());

            // 3. 파일 처리
            if (files != null && !files.isEmpty()) {
                String allReceiversStr = (receiverEmails != null) ? String.join(", ", receiverEmails) : "";
                for (int i = 0; i < files.size(); i++) {
                    MultipartFile file = files.get(i);
                    if (file.isEmpty()) continue;

                    String savedFileName = fileUtil.saveFile(file); // 물리 저장
                    log.info("파일 물리 저장 완료: {}", savedFileName);


                    TicketFile ticketFile = TicketFile.builder()
                            .uuid(savedFileName)
                            .fileName(file.getOriginalFilename())
                            .fileSize(file.getSize())
                            .ord(i).writer(writer)
                            .receiver(allReceiversStr).ticket(savedTicket)
                            .build();

                    ticketFileRepository.save(ticketFile); // DB 기록
                    log.info("파일 DB 기록 완료: {}", i);
                }
            }

            log.info("--- 모든 저장 로직 성공, DTO 변환 시작 ---");
            return toSentDetailDTO(savedTicket);

        } catch (Exception e) {
            log.error("!!! 티켓 저장 중 에러 발생 (롤백됨) !!!");
            log.error("에러 메시지: {}", e.getMessage());
            e.printStackTrace(); // 에러 스택을 전체 다 출력해서 어디서 터졌는지 확인
            throw e; // 다시 던져줘야 트랜잭션이 정상 롤백됨
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<TicketSentListDTO> listSent(String writer, TicketFilterDTO filter, PageRequestDTO pageRequestDTO) {
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
        Ticket ticket = ticketRepository.findWithPersonalListById(tno)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + tno));

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

        List<TicketFile> fileList = ticket.getFileList();
        if (fileList != null && !fileList.isEmpty()) {
            fileList.forEach(file -> {
//                log.info("파일 삭제 시도: {}", file.getUuid());
                fileUtil.deleteFile(file.getUuid()); // CustomFileUtil을 사용하여 실제 파일 삭제
            });
        }

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
                .files(
                        t.getFileList() != null ? t.getFileList().stream()
                                .map(f -> TicketFileDTO.builder()
                                        .uuid(f.getUuid())
                                        .fileName(f.getFileName())
                                        .fileSize(f.getFileSize())
                                        .createdAt(f.getCreatedAt())
                                        .writer(f.getWriter())
                                        .receiver(f.getReceiver())
                                        .build())
                                .collect(Collectors.toList()) : new ArrayList<>()
                )
                .build();
    }
}
