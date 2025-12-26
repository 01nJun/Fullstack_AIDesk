package com.desk.repository.search;


import com.desk.dto.PageRequestDTO;
import org.springframework.data.domain.Page;

public interface BoardSearch {
     // 리턴 타입을 Page<Object[]> 로 변경
    Page<Object[]> search1(PageRequestDTO pageRequestDTO);
}