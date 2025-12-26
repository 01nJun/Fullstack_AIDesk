 package com.desk.repository;

 import com.desk.domain.Reply;
 import org.springframework.data.domain.Page;
 import org.springframework.data.domain.Pageable;
 import org.springframework.data.jpa.repository.JpaRepository;
 import org.springframework.data.jpa.repository.Query;
 import org.springframework.data.repository.query.Param;

 public interface ReplyRepository extends JpaRepository<Reply, Long> {
     // 특정 게시물(bno)의 댓글들만 페이징 처리해서 가져오는 마법의 쿼리
     @Query("select r from Reply r where r.board.bno = :bno")
     Page<Reply> listOfBoard(@Param("bno") Long bno, Pageable pageable);
 }