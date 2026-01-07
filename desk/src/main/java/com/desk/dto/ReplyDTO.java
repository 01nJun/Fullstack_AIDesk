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
    private Long bno;
    private String replyText;
    private String replyer;

    // --- [대댓글을 위해 추가해야 할 필드] ---
    private Long parentRno; // 부모 댓글 번호 (답글일 때만 사용)

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime regDate;

    // 수정 시간도 화면에 필요할 수 있으니 참고하세요!
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime modDate;
}