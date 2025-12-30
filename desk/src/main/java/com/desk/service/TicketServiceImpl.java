package com.desk.service;

import com.desk.domain.Member;
import com.desk.domain.Ticket;
import com.desk.domain.TicketPersonal;
import com.desk.domain.UploadTicketFile;
import com.desk.dto.*;
import com.desk.repository.MemberRepository;
import com.desk.repository.TicketRepository;
import com.desk.util.CustomFileUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final MemberRepository memberRepository;
    private final CustomFileUtil fileUtil;

    @Override
    public TicketSentListDTO create(TicketCreateDTO req, String writer
            , List<MultipartFile> files ) {
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
        // 건영 S
        // 2) 업로드 파일명 연결 (ElementCollection documentList)
        List<UploadTicketFile> uploadFiles = fileUtil.saveFiles(files);

        if (uploadFiles != null) {
            uploadFiles.forEach(ticket::addDocument); // ✅ 여기!
        }
        // 건영 E
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


    @Override
    public TicketCreateDTO get(Long tno) {

        Optional<Ticket> result = ticketRepository.selectOne(tno);

        Ticket ticket = result.orElseThrow();

        TicketCreateDTO ticketCreateDTO = entityToDTO(ticket);

        return ticketCreateDTO;

    }

    private TicketCreateDTO entityToDTO(Ticket ticket){

        TicketCreateDTO ticketCreateDTO = TicketCreateDTO.builder()
                .tno(ticket.getTno())
                .title(ticket.getTitle())
                .content(ticket.getContent())
                .purpose(ticket.getPurpose())
                .requirement(ticket.getRequirement())
                .grade(ticket.getGrade())
                .deadline(ticket.getDeadline())
                .writer(ticket.getWriter().getEmail())
                .build();

        List<UploadTicketFile> documentFileList = ticket.getDocumentList();

        if(documentFileList == null || documentFileList.size() == 0 ){
            return ticketCreateDTO;
        }

        ticketCreateDTO.setUploadFileNames(documentFileList);

        return ticketCreateDTO;
    }

    @Override
    public List<FileItemDTO> getTicketFiles(Long tno) {
        Ticket ticket = ticketRepository.findById(tno)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + tno));

        List<UploadTicketFile> files = ticket.getDocumentList();
        if (files == null || files.isEmpty()) return List.of();

        return files.stream()
                .sorted(Comparator.comparingInt(UploadTicketFile::getOrd))
                .map(f -> {
                    // ✅ 여기서 파일명을 조합합니다.
                    String savedName = f.getLink();

                    return FileItemDTO.builder()
                            .ord(f.getOrd())
                            .uuid(f.getUuid())
                            .originalName(f.getOriginalName())
                            .ext(f.getExt())
                            .size(f.getSize())
                            .image(f.isImage())
                            .savedName(savedName) // DTO에는 프론트엔드를 위해 넣어줌
                            .viewUrl(fileUtil.makeViewUrl(savedName))
                            .previewUrl(fileUtil.makePreviewUrl(f))
                            .downloadUrl(fileUtil.makeDownloadUrl(savedName))
                            .build();
                })
                .toList();
    }
    // [NEW] 개별 파일 삭제 로직
    @Override
    @Transactional
    public void removeFile(String uuid, String writerEmail) {
        // 1. UUID로 티켓을 먼저 찾습니다 (역추적)
        Ticket ticket = ticketRepository.findByFileUuid(uuid)
                .orElseThrow(() -> new IllegalArgumentException("File not found with uuid: " + uuid));

        // 2. 작성자 본인이 맞는지 권한 체크
        if (!ticket.getWriter().getEmail().equals(writerEmail)) {
            throw new IllegalArgumentException("No permission to delete file.");
        }

        // 3. 파일 리스트에서 해당 파일 찾기
        List<UploadTicketFile> files = ticket.getDocumentList();
        UploadTicketFile targetFile = files.stream()
                .filter(f -> f.getUuid().equals(uuid))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("File object missing..."));

        // 4. 물리 파일 삭제 + DB 삭제
        fileUtil.deleteFile(targetFile.getLink(), targetFile.isImage());
        files.remove(targetFile); // 리스트에서 빼면 DB에서도 사라짐 (orphanRemoval 등 영향)
    }
    // [NEW] 내 파일 전체 목록 조회 구현

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<FileItemDTO> listUserFiles(String email, PageRequestDTO pageRequestDTO) {

        // 최신순 정렬 (Repository에서 ORDER BY t.tno DESC를 했으므로 여기선 Unsorted로 보내도 됨)
        Pageable pageable = PageRequest.of(pageRequestDTO.getPage() - 1, pageRequestDTO.getSize());

        Page<UploadTicketFile> result = ticketRepository.findAllFilesByUser(email, pageable);

        List<FileItemDTO> dtoList = result.getContent().stream().map(f -> {
            // 저장된 파일명 조합 (UUID_원본명)
            String savedName = f.getUuid() + "_" + f.getOriginalName();

            return FileItemDTO.builder()
                    .ord(f.getOrd())
                    .uuid(f.getUuid())
                    .originalName(f.getOriginalName())
                    .ext(f.getExt())
                    .size(f.getSize())
                    .image(f.isImage())
                    .savedName(savedName)
                    .viewUrl(fileUtil.makeViewUrl(savedName))     // 이미지 보기 URL
                    .previewUrl(fileUtil.makePreviewUrl(f))       // 썸네일 or 아이콘 URL
                    .downloadUrl(fileUtil.makeDownloadUrl(savedName)) // 다운로드 URL
                    .build();
        }).collect(Collectors.toList());

        return PageResponseDTO.<FileItemDTO>withAll()
                .dtoList(dtoList)
                .pageRequestDTO(pageRequestDTO)
                .totalCount(result.getTotalElements())
                .build();
    }


}
