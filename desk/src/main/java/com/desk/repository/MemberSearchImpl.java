package com.desk.repository;

import com.desk.domain.Department;
import com.desk.domain.Member;
import com.desk.domain.QMember;
import com.desk.dto.PageRequestDTO;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.JPQLQuery;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;

import java.util.List;

@Log4j2
public class MemberSearchImpl extends QuerydslRepositorySupport implements MemberSearch {

    public MemberSearchImpl() {
        super(Member.class);
    }

    @Override
    public Page<Member> searchMembers(boolean isApproved, PageRequestDTO pageRequestDTO, String keyword, String department) {

        QMember member = new QMember("m"); // 별칭을 "m"으로 명시
        JPQLQuery<Member> query = from(member);

        BooleanBuilder booleanBuilder = new BooleanBuilder();
        booleanBuilder.and(member.isDeleted.eq(false));
        booleanBuilder.and(member.isApproved.eq(isApproved));

        if (keyword != null && !keyword.trim().isEmpty()) {
            booleanBuilder.and(member.email.contains(keyword).or(member.nickname.contains(keyword)));
        }

        if (department != null && !department.trim().isEmpty()) {
            try {
                Department deptEnum = Department.valueOf(department.toUpperCase());
                booleanBuilder.and(member.department.eq(deptEnum));
            } catch (IllegalArgumentException e) {
                log.error("Invalid department value: " + department);
            }
        }

        query.where(booleanBuilder);

        // [수정 포인트 1] Pageable에서 Sort를 제거합니다.
        Pageable pageable = PageRequest.of(
                pageRequestDTO.getPage() - 1,
                pageRequestDTO.getSize()
        );

        // [수정 포인트 2] Querydsl 객체를 사용하여 직접 정렬을 추가합니다.
        query.orderBy(member.email.desc());

        // [수정 포인트 3] 페이징만 적용합니다.
        this.getQuerydsl().applyPagination(pageable, query);

        List<Member> list = query.fetch();
        long total = query.fetchCount();

        return new PageImpl<>(list, pageable, total);
    }
}