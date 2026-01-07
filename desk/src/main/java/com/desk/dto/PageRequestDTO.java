package com.desk.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class PageRequestDTO {

    @Builder.Default
    private int page = 1;

    @Builder.Default
    private int size = 10;

    private String type;
    private String keyword;  // 검색어
    private String category; // 카테고리

    //  프론트에서 "deadline,asc" 형태로 넘어오는 정렬 문자열
    private String sort;

    // PageRequestDTO.java
    public Pageable getPageable(String defaultProperty) { // 매개변수로 tno 또는 pno를 받음
        Sort finalSort; // finalSort: 프론트 정렬 문자열("deadline,asc")을 파싱해 엔티티 필드 기준의 ASC/DESC 정렬 규칙을 담은 Sort 객체

        if (this.sort != null && !this.sort.isEmpty()) {
            // 프론트에서 "deadline,asc" 같이 보낸 경우
            String[] split = this.sort.split(",");
            String prop = split[0];
            String dir = split.length > 1 ? split[1] : "asc";
            finalSort = dir.equalsIgnoreCase("desc") ? Sort.by(prop).descending() : Sort.by(prop).ascending();
        } else {
            // 정렬 조건이 없으면 무조건 최신순(내림차순)
            finalSort = Sort.by(defaultProperty).descending();
        }

        return PageRequest.of(this.page - 1, this.size, finalSort);
    }
}