package com.desk.repository;

import com.desk.domain.QTicket;
import com.desk.domain.QTicketPersonal;
import com.desk.domain.TicketPersonal;
import com.desk.dto.TicketFilterDTO;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class TicketPersonalSearchImpl implements TicketPersonalSearch{ // 구현체입니다

    // 쿼리팩토리 사용 (QueryDSL, 동적쿼리)
    private final JPAQueryFactory queryFactory;

    @Override
    public Page<TicketPersonal> findAllWithTicket(String receiver, TicketFilterDTO filter, Pageable pageable) {
        QTicketPersonal tp = QTicketPersonal.ticketPersonal;
        QTicket ticket = QTicket.ticket;

        BooleanBuilder builder = new BooleanBuilder();
        builder.and(tp.receiver.email.eq(receiver));

        // 필터 적용
        if (filter != null) {
            if (filter.getGrade() != null) builder.and(ticket.grade.eq(filter.getGrade()));
            if (filter.getKeyword() != null && !filter.getKeyword().isBlank()) {
                String kw = "%" + filter.getKeyword() + "%";
                builder.and(ticket.title.like(kw).or(ticket.content.like(kw)).or(ticket.writer.email.like(kw)));
            }
        }

        // 정렬 적용
        // 프론트 정렬 옵션(tno DESC / deadline ASC·DESC)에 따라 기본 최신순 또는 마감일 기준 QueryDSL 정렬 적용
        OrderSpecifier<?> orderSpecifier = tp.pno.desc();
        if (!pageable.getSort().isEmpty()) {
            for (Sort.Order order : pageable.getSort()) {
                if (order.getProperty().equals("deadline")) {
                    orderSpecifier = order.isAscending() ? ticket.deadline.asc() : ticket.deadline.desc();
                }
            }
        }

        List<TicketPersonal> content = queryFactory.selectFrom(tp)
                .join(tp.ticket, ticket).fetchJoin()
                .where(builder)
                .offset(pageable.getOffset()).limit(pageable.getPageSize())
                .orderBy(orderSpecifier)
                .fetch();

        long total = queryFactory.select(tp.count()).from(tp).join(tp.ticket, ticket).where(builder).fetchOne();
        return new PageImpl<>(content, pageable, total);
    }



    // 단건 조회
    // pno(티켓 번호)로 퍼스널 티켓 가져와서 --> 해당 티켓이 가리키는 ticket도 붙여서 가져옴
    @Override
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

