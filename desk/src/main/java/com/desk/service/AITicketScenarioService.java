package com.desk.service;

import com.desk.dto.AITicketRequestDTO;
import com.desk.dto.AITicketResponseDTO;

public interface AITicketScenarioService {

    /**
     * @return 매칭되면 시나리오 응답, 아니면 null
     */
    AITicketResponseDTO tryHandleScenario(AITicketRequestDTO request);
}



