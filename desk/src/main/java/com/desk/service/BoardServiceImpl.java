package com.desk.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.desk.domain.Board;
import com.desk.dto.PageRequestDTO;
import com.desk.dto.PageResponseDTO;
import com.desk.dto.BoardDTO;
import com.desk.repository.BoardRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Transactional
@Log4j2
@RequiredArgsConstructor
public class BoardServiceImpl implements BoardService {

    private final ModelMapper modelMapper;
    private final BoardRepository boardRepository;

    @Override
    public Long register(BoardDTO boardDTO) {
        log.info("--- 게시글 등록 ---");
        Board board = dtoToEntity(boardDTO);
        Board savedBoard = boardRepository.save(board);
        return savedBoard.getBno();
    }

    @Override
    public BoardDTO get(Long bno) {
        log.info("--- 게시글 상세 조회: " + bno);
        Optional<Board> result = boardRepository.findById(bno);
        Board board = result.orElseThrow();
        return entityToDto(board);
    }

    @Override
    public void modify(BoardDTO boardDTO) {
        log.info("--- 게시글 수정 시작 ---");
        Optional<Board> result = boardRepository.findById(boardDTO.getBno());
        Board board = result.orElseThrow();

        board.changeTitle(boardDTO.getTitle());
        board.changeCategory(boardDTO.getCategory());
        board.changeContent(boardDTO.getContent());
        board.changeModDate(LocalDateTime.now());

        boardRepository.save(board);
    }

    @Override
    public void remove(Long bno) {
        log.info("--- 게시글 삭제: " + bno);
        boardRepository.deleteById(bno);
    }

    @Override
    public PageResponseDTO<BoardDTO> list(PageRequestDTO pageRequestDTO) {
        
        log.info("--- 게시글 목록 조회 (검색 및 댓글수 포함) ---");

        // [수정 포인트 1] 리턴 타입을 Page<Board>에서 Page<Object[]>로 변경!
        // 리포지토리의 search1이 이제 [Board엔티티, 댓글수] 배열을 돌려주기 때문이에요.
        Page<Object[]> result = boardRepository.search1(pageRequestDTO);

        // [수정 포인트 2] 데이터 뭉치(Object[])를 하나씩 꺼내서 DTO로 변환합니다.
        List<BoardDTO> dtoList = result.getContent().stream().map(arr -> {
            
            Board board = (Board) arr[0];      // 0번 칸: Board 엔티티
            long replyCount = (long) arr[1];   // 1번 칸: 댓글 개수 (long 타입)
            
            // 엔티티를 DTO로 변환
            BoardDTO boardDTO = entityToDto(board);
            
            // [핵심] DTO에 댓글 개수를 세팅해줍니다! (BoardDTO에 replyCount 필드가 있어야 함)
            boardDTO.setReplyCount(replyCount); 
            
            return boardDTO;
            
        }).collect(Collectors.toList());

        long totalCount = result.getTotalElements();

        return PageResponseDTO.<BoardDTO>withAll()
                .dtoList(dtoList)
                .pageRequestDTO(pageRequestDTO)
                .totalCount(totalCount)
                .build();
    }
}