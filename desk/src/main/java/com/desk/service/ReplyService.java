 package com.desk.service;

 import com.desk.dto.PageRequestDTO;
 import com.desk.dto.PageResponseDTO;
 import com.desk.dto.ReplyDTO;

 public interface ReplyService {
     Long register(ReplyDTO replyDTO); // 댓글 등록
     ReplyDTO read(Long rno);          // 댓글 조회
     void modify(ReplyDTO replyDTO);   // 댓글 수정
     void remove(Long rno);            // 댓글 삭제
    
     // 특정 게시물의 댓글 목록 가져오기
     PageResponseDTO<ReplyDTO> getListOfBoard(Long bno, PageRequestDTO pageRequestDTO);
 }