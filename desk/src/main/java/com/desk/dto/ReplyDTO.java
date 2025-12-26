 package com.desk.dto;

 import com.fasterxml.jackson.annotation.JsonFormat;
 import lombok.*;
 import java.time.LocalDateTime;

 @Data
 @Builder
 @AllArgsConstructor
 @NoArgsConstructor
 public class ReplyDTO {
     private Long rno;
     private Long bno; // 어떤 게시글의 댓글인지 번호가 필요해요!
     private String replyText;
     private String replyer;

     @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
     private LocalDateTime regDate;
 }