package com.desk.repository;

import com.desk.domain.Reply;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReplyRepository extends JpaRepository<Reply, Long> {

    // [수정된 쿼리]
    // 1. coalesce(r.parent.rno, r.rno): 부모가 있으면 부모 번호로, 없으면 내 번호로 그룹핑 (부모-자식 뭉치기)
    // 2. r.regDate asc: 그 그룹 안에서 등록 순서대로 정렬 (부모가 먼저 나오고 그다음 대댓글)
    @Query("select r from Reply r left join fetch r.parent " +
            "where r.board.bno = :bno " +
            "order by coalesce(r.parent.rno, r.rno) asc, r.regDate asc")
    Page<Reply> listOfBoard(@Param("bno") Long bno, Pageable pageable);
}