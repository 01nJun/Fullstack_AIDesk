package com.desk.service;

import com.desk.dto.PageRequestDTO;
import com.desk.dto.PageResponseDTO;
import com.desk.dto.TicketFileDTO;
import com.desk.dto.TicketFilterDTO;

public interface FileService {
    PageResponseDTO<TicketFileDTO> getFileBoxList(String email, String type, TicketFilterDTO filter, PageRequestDTO pageRequestDTO);
    void deleteFile(String uuid, String requesterEmail);
}