package com.desk.service;

import com.desk.dto.MemberDTO;
import com.desk.dto.PageRequestDTO;
import com.desk.dto.PageResponseDTO;

public interface AdminService {
    PageResponseDTO<MemberDTO> getPendingMembers(PageRequestDTO pageRequestDTO, String keyword, String department);
    PageResponseDTO<MemberDTO> getActiveMembers(PageRequestDTO pageRequestDTO, String keyword, String department);
    void approveMember(String email);
    void deleteMember(String email);
}