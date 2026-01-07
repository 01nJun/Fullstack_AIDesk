package com.desk.service;

import com.desk.domain.TicketFile;
import com.desk.dto.PageRequestDTO;
import com.desk.dto.PageResponseDTO;
import com.desk.dto.TicketFileDTO;
import com.desk.dto.TicketFilterDTO;
import com.desk.repository.TicketFileRepository;
import com.desk.util.CustomFileUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FileServiceImpl implements FileService {

    private final TicketFileRepository ticketFileRepository;
    private final CustomFileUtil fileUtil;

    @Override
    public PageResponseDTO<TicketFileDTO> getFileBoxList(String email, String type, TicketFilterDTO filter, PageRequestDTO pageRequestDTO) {

        // 1. 정렬 및 페이징 설정
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt"); // 기본 최신순
        if ("createdAt,asc".equals(pageRequestDTO.getSort())) {
            sort = Sort.by(Sort.Direction.ASC, "createdAt");
        }

        Pageable pageable = PageRequest.of(pageRequestDTO.getPage() - 1, 10, sort);

        // 2. 검색 조건 및 탭 조건에 따른 조회 (QueryDSL 추천이나, 여기선 JPA Method/Spec 예시)
        String kw = (filter.getKeyword() == null) ? "" : filter.getKeyword();

        Page<TicketFile> result;

        // 탭 조건: ALL(내가 작성자 OR 수신자), SENT(내가 작성자), RECEIVED(내가 수신자)
        if ("SENT".equals(type)) {
            result = ticketFileRepository.findByWriterAndSearch(email, kw, pageable);
        } else if ("RECEIVED".equals(type)) {
            result = ticketFileRepository.findByReceiverAndSearch(email, kw, pageable);
        } else {
            result = ticketFileRepository.findAllByEmailAndSearch(email, kw, pageable);
        }

        // 3. Entity -> DTO 변환
        List<TicketFileDTO> dtoList = result.getContent().stream()
                .map(this::entityToDTO)
                .collect(Collectors.toList());

        return PageResponseDTO.<TicketFileDTO>withAll()
                .dtoList(dtoList)
                .pageRequestDTO(pageRequestDTO)
                .totalCount(result.getTotalElements())
                .build();
    }
    @Override
    @Transactional
    public void deleteFile(String uuid) {
        TicketFile ticketFile = ticketFileRepository.findById(uuid)
                .orElseThrow(() -> new IllegalArgumentException("파일을 찾을 수 없습니다."));

        // 1. 물리 파일 삭제
        fileUtil.deleteFile(ticketFile.getUuid());

        // 2. DB 기록 삭제
        ticketFileRepository.delete(ticketFile);
    }

    private TicketFileDTO entityToDTO(TicketFile ticketFile) {
        return TicketFileDTO.builder()
                .uuid(ticketFile.getUuid())
                .fileName(ticketFile.getFileName())
                .fileSize(ticketFile.getFileSize())
                .ord(ticketFile.getOrd())
                .createdAt(ticketFile.getCreatedAt())
                .writer(ticketFile.getWriter())
                .receiver(ticketFile.getReceiver())
                .build();
    }
}