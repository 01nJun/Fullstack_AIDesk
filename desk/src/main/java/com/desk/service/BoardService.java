package com.desk.service;

import com.desk.domain.Board;
import com.desk.dto.PageRequestDTO;
import com.desk.dto.PageResponseDTO;
import com.desk.dto.BoardDTO;

public interface BoardService {
    
    Long register(BoardDTO boardDTO);

    BoardDTO get(Long bno);
    
    void modify(BoardDTO boardDTO);

    void remove(Long bno);
    
    // 목록 및 검색 처리를 위한 메서드 
    PageResponseDTO<BoardDTO> list(PageRequestDTO pageRequestDTO);

  
    // DTO를 엔티티로 변환 (글 등록/수정 시 사용)
    default Board dtoToEntity(BoardDTO boardDTO) {
        return Board.builder()
                .bno(boardDTO.getBno())
                .title(boardDTO.getTitle())
                .content(boardDTO.getContent())
                .writer(boardDTO.getWriter())
                .category(boardDTO.getCategory())
                .build();
    }

    // 엔티티를 DTO로 변환 (상세보기/목록 출력 시 사용)
    default BoardDTO entityToDto(Board board) {
        return BoardDTO.builder()
                .bno(board.getBno())
                .title(board.getTitle())
                .content(board.getContent())
                .writer(board.getWriter())
                .category(board.getCategory())
                .regDate(board.getRegDate())
                .modDate(board.getModDate())
                .build();
    }
}