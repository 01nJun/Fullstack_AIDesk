package com.desk.repository;

import com.desk.domain.QTicket;
import com.desk.domain.QTicketPersonal;
import com.desk.domain.Ticket;
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
public class TicketSearchImpl implements TicketSearch{

    // 쿼리팩토리 사용 (QueryDSL, 동적쿼리)
    private final JPAQueryFactory queryFactory;

    // 단건 조회 (상세)
    // tno (티켓O 퍼스널X)로 티켓 가져와서 수신자 목록 보여줌
    // 여기 writer 조건 추가하면 권한 체크
    @Override
    public Optional<Ticket> findWithPersonalListById(Long tno) {
        QTicket ticket = QTicket.ticket;

        Ticket result = queryFactory
                .selectFrom(ticket)
                .leftJoin(ticket.personalList).fetchJoin()
                .where(ticket.tno.eq(tno))
                .fetchOne();

        return Optional.ofNullable(result);
    }
    @Override
    public Page<Ticket> findAllAll(String email, TicketFilterDTO filter, Pageable pageable) {
        QTicket ticket = QTicket.ticket;
        QTicketPersonal tp = QTicketPersonal.ticketPersonal;
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(ticket.writer.email.eq(email).or(tp.receiver.email.eq(email)));

        applyFilters(builder, filter, ticket);

        List<Ticket> content = queryFactory.selectFrom(ticket).distinct()
                .leftJoin(ticket.personalList, tp).fetchJoin()
                .where(builder)
                .offset(pageable.getOffset()).limit(pageable.getPageSize())
                .orderBy(getOrderSpecifier(pageable, ticket))
                .fetch();

        long total = queryFactory.select(ticket.countDistinct()).from(ticket).leftJoin(ticket.personalList, tp).where(builder).fetchOne();
        return new PageImpl<>(content, pageable, total);
    }

    // [보낸 티켓 탭]
    @Override
    public Page<Ticket> findAllWithPersonalList(String writer, TicketFilterDTO filter, Pageable pageable) {
        QTicket ticket = QTicket.ticket;
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(ticket.writer.email.eq(writer));

        applyFilters(builder, filter, ticket);

        List<Ticket> content = queryFactory.selectFrom(ticket).distinct()
                .leftJoin(ticket.personalList).fetchJoin()
                .where(builder)
                .offset(pageable.getOffset()).limit(pageable.getPageSize())
                .orderBy(getOrderSpecifier(pageable, ticket))
                .fetch();

        long total = queryFactory.select(ticket.countDistinct()).from(ticket).where(builder).fetchOne();
        return new PageImpl<>(content, pageable, total);
    }

    private void applyFilters(BooleanBuilder builder, TicketFilterDTO filter, QTicket ticket) {
        if (filter != null) {
            if (filter.getGrade() != null) builder.and(ticket.grade.eq(filter.getGrade()));
            if (filter.getKeyword() != null && !filter.getKeyword().isBlank()) {
                String kw = "%" + filter.getKeyword() + "%";
                builder.and(ticket.title.like(kw).or(ticket.content.like(kw)).or(ticket.writer.email.like(kw)));
            }
        }
    }
    private OrderSpecifier<?> getOrderSpecifier(Pageable pageable, QTicket ticket) {
        if (!pageable.getSort().isEmpty()) {
            for (Sort.Order order : pageable.getSort()) {
                if (order.getProperty().equals("deadline")) {
                    return order.isAscending() ? ticket.deadline.asc() : ticket.deadline.desc();
                }
                if (order.getProperty().equals("tno")) {
                    return order.isAscending() ? ticket.tno.asc() : ticket.tno.desc();
                }
            }
        }
        return ticket.tno.desc(); // 최종 기본값: 최신순
    }
}

