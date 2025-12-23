package com.desk.repository;

import com.desk.domain.QTicket;
import com.desk.domain.QTicketPersonal;
import com.desk.domain.TicketPersonal;
import com.desk.dto.TicketFilterDTO;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class TicketPersonalRepositoryImpl { // 구현체입니다

    // 쿼리팩토리 사용 (QueryDSL, 동적쿼리)
    private final JPAQueryFactory queryFactory;
    
    public Page<TicketPersonal> findAllWithTicket(String receiver, TicketFilterDTO filter, Pageable pageable) {
        // 쿼리DSL 객체
        QTicketPersonal tp = QTicketPersonal.ticketPersonal;
        QTicket ticket = QTicket.ticket;

        // 동적쿼리 조건 붙이는 빌더
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(tp.receiver.eq(receiver));

        // TicketFilterDTO 있다면 = 필터링 했다면
        if (filter != null) {
            if (filter.getState() != null) {
                builder.and(tp.state.eq(filter.getState()));
            }
            if (filter.getRead() != null) {
                builder.and(tp.isread.eq(filter.getRead()));
            }
            if (filter.getGrade() != null) {
                builder.and(ticket.grade.eq(filter.getGrade()));
            }
            if (filter.getKeyword() != null && !filter.getKeyword().isBlank()) {
                String keyword = "%" + filter.getKeyword() + "%";
                builder.and(ticket.title.like(keyword).or(ticket.content.like(keyword)));
            }
        } // 조건 붙이기

        // 실제 목록 조회 + fetchJoin
        List<TicketPersonal> content = queryFactory
                .selectFrom(tp)
                .join(tp.ticket, ticket).fetchJoin()
                .where(builder)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(tp.pno.desc())
                .fetch();

        // 전체 몇 개인지 세기
        JPAQuery<Long> countQuery = queryFactory
                .select(tp.count())
                .from(tp)
                .join(tp.ticket, ticket)
                .where(builder);
        
//        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
        return new PageImpl<>(content, pageable, countQuery.fetchOne());
    }



    // 단건 조회
    // pno(티켓 번호)로 퍼스널 티켓 가져와서 --> 해당 티켓이 가리키는 ticket도 붙여서 가져옴
    public Optional<TicketPersonal> findWithTicketByPno(Long pno) {
        QTicketPersonal tp = QTicketPersonal.ticketPersonal;
        QTicket ticket = QTicket.ticket;

        // fetchJoin 으로 n+1문제 막기
        TicketPersonal result = queryFactory
                .selectFrom(tp)
                .join(tp.ticket, ticket).fetchJoin()
                .where(tp.pno.eq(pno))
                .fetchOne();
        
        return Optional.ofNullable(result);
    }
}

