package com.desk.service;

import com.desk.dto.AIFileRequestDTO;
import com.desk.dto.AIFileResponseDTO;

public interface AIFileService {
    AIFileResponseDTO chat(String receiverEmail, AIFileRequestDTO request);
}



