package com.desk.service;

import com.desk.dto.AITicketRequestDTO;
import com.desk.dto.AITicketResponseDTO;

public interface AITicketService {

    /**
     * [AI 티켓 처리 메인 메서드]
     * 1. 부서 라우팅 (Step 1)
     * 2. 담당자 확인 (Step 2)
     * 3. 인터뷰 및 티켓 생성 (Step 3)
     */
    AITicketResponseDTO processRequest(AITicketRequestDTO request);
}