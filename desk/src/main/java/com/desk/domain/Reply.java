 package com.desk.domain;

 import jakarta.persistence.*;
 import lombok.*;

 @Entity
 @Table(name = "tbl_reply", indexes = {
     @Index(name = "idx_reply_board_bno", columnList = "board_bno")
 })
 @Getter
 @Builder
 @AllArgsConstructor
 @NoArgsConstructor
 @ToString(exclude = "board")
 public class Reply extends BaseEntity {

     @Id
     @GeneratedValue(strategy = GenerationType.IDENTITY)
     private Long rno; // 댓글 번호

     @ManyToOne(fetch = FetchType.LAZY)
     private Board board;

     private String replyText; // 댓글 내용
     private String replyer;   // 댓글 작성자

     // --- [수정 포인트: 도메인 로직 추가] ---
    
     /**
      * 댓글 내용을 수정할 때 사용합니다.
      * ReplyServiceImpl의 modify 메서드에서 호출됩니다.
      */
     public void changeText(String replyText) {
         this.replyText = replyText;
     }


     public void changeReplyer(String replyer) {
         this.replyer = replyer;
     }
 }