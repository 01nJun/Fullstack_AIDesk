package com.desk.repository.chat;

import com.desk.domain.ChatFile;
import com.desk.domain.ChatMessage;
import com.desk.domain.QChatFile;
import com.desk.domain.QChatMessage;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * ChatMessage QueryDSL 구현체
 * N+1 문제 해결을 위한 최적화된 쿼리 제공
 */
@RequiredArgsConstructor
public class ChatMessageSearchImpl implements ChatMessageSearch {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<ChatMessage> findByChatRoomIdOrderByMessageSeqDescWithFiles(Long chatRoomId, Pageable pageable) {
        QChatMessage chatMessage = QChatMessage.chatMessage;
        QChatFile chatFile = QChatFile.chatFile;

        // 1. 메시지 목록 조회 (페이징)
        List<ChatMessage> messages = queryFactory
                .selectFrom(chatMessage)
                .where(chatMessage.chatRoom.id.eq(chatRoomId))
                .orderBy(chatMessage.messageSeq.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        if (messages.isEmpty()) {
            return new PageImpl<>(messages, pageable, 0);
        }

        // 2. 해당 메시지들의 파일을 한 번에 조회 (N+1 방지)
        List<Long> messageIds = messages.stream()
                .map(ChatMessage::getId)
                .toList();

        List<ChatFile> files = queryFactory
                .selectFrom(chatFile)
                .where(chatFile.chatMessage.id.in(messageIds))
                .fetch();

        // 3. 파일을 메시지에 매핑
        // 메시지 ID를 키로 하는 맵 생성
        java.util.Map<Long, List<ChatFile>> filesByMessageId = files.stream()
                .filter(file -> file.getChatMessage() != null && file.getChatMessage().getId() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        file -> file.getChatMessage().getId()
                ));

        // 각 메시지에 파일 할당
        messages.forEach(message -> {
            List<ChatFile> messageFiles = filesByMessageId.getOrDefault(message.getId(), java.util.Collections.emptyList());
            // chatFiles 컬렉션 초기화 및 설정
            message.getChatFiles().clear();
            message.getChatFiles().addAll(messageFiles);
        });

        // 4. 전체 개수 조회
        Long total = queryFactory
                .select(chatMessage.count())
                .from(chatMessage)
                .where(chatMessage.chatRoom.id.eq(chatRoomId))
                .fetchOne();

        return new PageImpl<>(messages, pageable, total != null ? total : 0);
    }
}

