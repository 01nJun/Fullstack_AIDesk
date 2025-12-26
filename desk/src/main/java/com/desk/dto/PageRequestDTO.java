package com.desk.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class PageRequestDTO {

    @Builder.Default
    private int page = 1;

    @Builder.Default
    private int size = 10;

    // 검색 키워드
    private String type;    // 검색 조건 (예: t=제목, c=내용, w=작성자)
    private String keyword; // 사용자가 입력한 검색어
    // 카테고리
    private String category; // 카테고리
}