package com.desk.repository;

import com.desk.domain.Member;
import com.desk.dto.PageRequestDTO;
import org.springframework.data.domain.Page;

public interface MemberSearch {
    Page<Member> searchMembers(boolean isApproved, PageRequestDTO pageRequestDTO, String keyword, String department);
}