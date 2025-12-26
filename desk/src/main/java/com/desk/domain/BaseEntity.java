 package com.desk.domain; // 패키지 경로 확인!

 import jakarta.persistence.Column;
 import jakarta.persistence.EntityListeners;
 import jakarta.persistence.MappedSuperclass;
 import lombok.Getter;
 import org.springframework.data.annotation.CreatedDate;
 import org.springframework.data.annotation.LastModifiedDate;
 import org.springframework.data.jpa.domain.support.AuditingEntityListener;
 import java.time.LocalDateTime;

 @MappedSuperclass // ← 이게 있어야 다른 엔티티들이 상속받을 수 있어요!
 @EntityListeners(value = { AuditingEntityListener.class })
 @Getter
 abstract class BaseEntity {

     @CreatedDate
     @Column(name = "regdate", updatable = false)
     private LocalDateTime regDate;

     @LastModifiedDate
     @Column(name = "moddate")
     private LocalDateTime modDate;
 }