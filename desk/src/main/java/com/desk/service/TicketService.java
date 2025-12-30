package com.desk.service;

import com.desk.dto.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface TicketService {

    // Ticket + TicketPersonal N개 생성
    TicketSentListDTO create(TicketCreateDTO req, String writer,List<MultipartFile> files);

    // 보낸 티켓 목록(페이징 + 필터)
    PageResponseDTO<TicketSentListDTO> listSent(String writer, TicketFilterDTO filter, PageRequestDTO pageRequestDTO);

    // 전체 티켓 목록(페이징 + 필터)
    PageResponseDTO<TicketSentListDTO> listAll(String email, TicketFilterDTO filter, PageRequestDTO pageRequestDTO);

    // 보낸 티켓 단일 상세
    TicketSentListDTO readSent(Long tno, String writer);

    // 삭제
    void deleteSent(Long tno, String writer);

    TicketCreateDTO get(Long tno);

    List<FileItemDTO> getTicketFiles(Long tno);

    // [NEW] 티켓 내 특정 파일 1개 삭제
    void removeFile(String uuid, String writerEmail);
    // [NEW] 내 파일 전체 목록 조회
    PageResponseDTO<FileItemDTO> listUserFiles(String writer, PageRequestDTO pageRequestDTO);
}