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

        // 1. 기본 조건: 수신자 본인 티켓
        builder.and(tp.receiver.email.eq(receiver));

        // 2. 동적 필터 적용
        if (filter != null) {
            // 상단 검색어 (제목, 내용, 작성자)
            if (filter.getKeyword() != null && !filter.getKeyword().isBlank()) {
                String kw = "%" + filter.getKeyword() + "%";
                builder.and(ticket.title.like(kw).or(ticket.content.like(kw)).or(ticket.writer.email.like(kw)));
            }
            // 상단/컬럼 중요도 필터
            if (filter.getGrade() != null) {
                builder.and(ticket.grade.eq(filter.getGrade()));
            }
            // 상단 읽음 상태 필터
            if (filter.getRead() != null) {
                builder.and(tp.isread.eq(filter.getRead()));
            }
            // 컬럼 진행 상태 필터
            if (filter.getState() != null) {
                builder.and(tp.state.eq(filter.getState()));
            }
        }

        // 3. 동적 정렬 적용 (Tno 또는 Deadline)
        OrderSpecifier<?> orderSpecifier = tp.pno.desc(); // 기본값
        if (!pageable.getSort().isEmpty()) {
            for (Sort.Order order : pageable.getSort()) {
                if (order.getProperty().equals("deadline")) {
                    orderSpecifier = order.isAscending() ? ticket.deadline.asc() : ticket.deadline.desc();
                } else if (order.getProperty().equals("tno")) {
                    orderSpecifier = order.isAscending() ? tp.pno.asc() : tp.pno.desc();
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