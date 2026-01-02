package com.desk.repository;

import com.desk.domain.Member;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface MemberRepository extends JpaRepository<Member, String>, MemberSearch {
    
    @EntityGraph(attributePaths = {"roleList"})
    @Query("select m from Member m where m.email = :email")
    Member getWithRoles(@Param("email") String email);


@Query("select m from Member m where m.nickname = :nickname and m.isDeleted = false and m.isApproved = true")
    Optional<Member> findByNickname(@Param("nickname") String nickname);

    
}