package com.desk.service;

import com.desk.dto.TicketPinResponseDTO;
import java.util.List;

public interface TicketPinService {
    // 찜 토글 (추가/삭제 후 최신 목록 반환)
    List<TicketPinResponseDTO> togglePin(String email, Long tno);

    // 찜 목록 조회
    List<TicketPinResponseDTO> getPinList(String email);
}