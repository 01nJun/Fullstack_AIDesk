package com.desk.repository;

import com.desk.domain.TicketPin;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TicketPinRepository extends JpaRepository<TicketPin, Long> {
    // 특정 유저의 찜 목록 전체 가져오기
    List<TicketPin> findByMemberEmail(String email);

    // 중복 체크 및 삭제를 위한 조회
    Optional<TicketPin> findByMemberEmailAndTicketTno(String email, Long tno);
}