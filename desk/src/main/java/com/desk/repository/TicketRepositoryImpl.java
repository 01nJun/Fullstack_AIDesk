package com.desk.repository;

import com.desk.domain.QTicket;
import com.desk.domain.Ticket;
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
public class TicketRepositoryImpl {

    // 쿼리팩토리 사용 (QueryDSL, 동적쿼리)
    private final JPAQueryFactory queryFactory;
    
    // 리스트 조회
    public Page<Ticket> findAllWithPersonalList(String writer, TicketFilterDTO filter, Pageable pageable) {
        // 쿼리DSL 객체
        QTicket ticket = QTicket.ticket;

        // 동적쿼리 조건 붙이는 빌더
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(ticket.writer.eq(writer));

        // TicketFilterDTO 있으면 필터링
        if (filter != null) {
            if (filter.getGrade() != null) {
                builder.and(ticket.grade.eq(filter.getGrade()));
            }
            if (filter.getKeyword() != null && !filter.getKeyword().isBlank()) {
                String keyword = "%" + filter.getKeyword() + "%";
                builder.and(ticket.title.like(keyword).or(ticket.content.like(keyword)));
            }
        }

        // fetchJoin 으로 수신자 목록 조인 (N+1 방지)
        // distinct 으로 같은 티켓 여러개 한 번에 합쳐서 반환
        List<Ticket> content = queryFactory
                .selectFrom(ticket)
                .distinct()
                .leftJoin(ticket.personalList).fetchJoin()
                .where(builder)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(ticket.tno.desc())
                .fetch();

        // 총 개수 세기
        JPAQuery<Long> countQuery = queryFactory
                .select(ticket.count())
                .from(ticket)
                .where(builder);

        // 이게 더 좋은거라는데 (pageable하지 않으면 count 생략) 너무 끔찍하게 생겨서 원래 하던걸로 해뒀어요...
//        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
        return new PageImpl<>(content, pageable, countQuery.fetchOne());

    }

    // 단건 조회 (상세)
    // tno (티켓O 퍼스널X)로 티켓 가져와서 수신자 목록 보여줌
    // 여기 writer 조건 추가하면 권한 체크
    public Optional<Ticket> findWithPersonalListById(Long tno) {
        QTicket ticket = QTicket.ticket;
        
        Ticket result = queryFactory
                .selectFrom(ticket)
                .leftJoin(ticket.personalList).fetchJoin()
                .where(ticket.tno.eq(tno))
                .fetchOne();
        
        return Optional.ofNullable(result);
    }
}

