package com.desk.domain;

import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable // JPA 내장타입으로 사용하기 위한 어노테이션
@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UploadTicketFile {

    private String fileName;
    private int ord;
    public void setOrd(int ord) {
        this.ord = ord;
    }


}
