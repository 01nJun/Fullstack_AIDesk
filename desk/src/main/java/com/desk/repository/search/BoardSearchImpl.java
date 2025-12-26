package com.desk.repository.search;

import com.desk.domain.Board;
import com.desk.domain.QBoard;
import com.desk.domain.QReply; // 1. 댓글 Q파일 임포트!
import com.desk.dto.PageRequestDTO;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.JPQLQuery;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;

import java.util.List;
import java.util.stream.Collectors;

public class BoardSearchImpl extends QuerydslRepositorySupport implements BoardSearch {

    public BoardSearchImpl() {
        super(Board.class);
    }

    // [선생님 체크] 리턴 타입을 Page<Board>에서 Page<Object[]>로 꼭 바꿔야 합니다!
    @Override
    public Page<Object[]> search1(PageRequestDTO pageRequestDTO) {
        
        QBoard board = QBoard.board;
        QReply reply = QReply.reply; // 댓글 Q파일 선언

        JPQLQuery<Board> query = from(board);

        // 2. [핵심] 댓글 테이블과 왼쪽 합체(Left Join)를 합니다.
        // 게시글에 댓글이 0개여도 게시글은 보여야 하니까 leftJoin을 씁니다.
        query.leftJoin(reply).on(reply.board.eq(board));

        // 키워드 검색 로직 (기존 유지)
        if (pageRequestDTO.getType() != null && 
            pageRequestDTO.getKeyword() != null && 
            !pageRequestDTO.getKeyword().trim().isEmpty()) {

            String[] typeArray = pageRequestDTO.getType().split("");
            String keyword = pageRequestDTO.getKeyword();
            BooleanBuilder booleanBuilder = new BooleanBuilder();

            for (String t : typeArray) {
                switch (t) {
                    case "t": booleanBuilder.or(board.title.contains(keyword)); break;
                    case "c": booleanBuilder.or(board.content.contains(keyword)); break;
                    case "w": booleanBuilder.or(board.writer.contains(keyword)); break;
                }
            }
            query.where(booleanBuilder);
        }

        // 카테고리 필터링 (기존 유지)
        if (pageRequestDTO.getCategory() != null && 
            !pageRequestDTO.getCategory().trim().isEmpty() &&
            !pageRequestDTO.getCategory().equals("전체")) {
            
            query.where(board.category.eq(pageRequestDTO.getCategory()));
        }

        // 3. [핵심] 게시글별로 그룹을 묶어줍니다. (그래야 게시글당 댓글 수를 셀 수 있어요)
        query.groupBy(board);

        // 페이징 처리
        Pageable pageable = PageRequest.of(
                pageRequestDTO.getPage() - 1,
                pageRequestDTO.getSize(),
                Sort.by("bno").descending()
        );
        this.getQuerydsl().applyPagination(pageable, query);

        // 4. [핵심] select 부분에서 '게시글'과 '댓글 개수(count)'를 함께 뽑아냅니다.
        JPQLQuery<Tuple> tupleQuery = query.select(board, reply.count());
        
        List<Tuple> tupleList = tupleQuery.fetch();
        long count = tupleQuery.fetchCount();

        // 5. Tuple 형태를 서비스에서 쓰기 편하게 Object 배열 리스트로 변환합니다.
        List<Object[]> resultList = tupleList.stream()
                .map(t -> t.toArray())
                .collect(Collectors.toList());

        return new PageImpl<>(resultList, pageable, count);
    }
}