package com.desk.dto;

import com.desk.domain.TicketGrade;
import com.desk.domain.TicketState;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketFilterDTO {
    /*
    * filter={}는 TicketFilterDTO의 toString()에 의존합니다.
    * filter가 길게 찍히면, DTO에 @ToString을 커스터마이징해서
    * “필요한 필드만” 나오게 만들면 더 정돈됩니다.
    * 라고 하는데 이거 추후에 확인해볼 필요 있을 것 같습니다.
    * */

    // 공통
    private TicketGrade grade;

    // 받은함에서 주로 사용
    private Boolean read;
    private TicketState state;

    // 제목/본문 검색까지 확장
    private String keyword;
}