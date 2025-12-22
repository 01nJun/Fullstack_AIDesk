package com.desk.repository;

import com.desk.domain.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MemberRepository extends JpaRepository<Member, String> {
    
    @EntityGraph(attributePaths = {"roleList"})
    @Query("select m from Member m where m.email = :email")
    Member getWithRoles(@Param("email") String email);

    // 승인 대기 인원 조회 (삭제 안 된 사람 중)
    @EntityGraph(attributePaths = {"roleList"})
    List<Member> findByIsApprovedFalseAndIsDeletedFalse();

    // 전체 직원 조회 (승인 완료 + 삭제 안 된 사람), 페이징
    @EntityGraph(attributePaths = {"roleList"})
    Page<Member> findByIsApprovedTrueAndIsDeletedFalse(Pageable pageable);
}