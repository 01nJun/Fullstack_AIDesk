package com.desk.repository;

import java.time.LocalDateTime;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;


import com.desk.domain.Board;

import lombok.extern.log4j.Log4j2;

@SpringBootTest
@Log4j2
public class BoardRepositoryTests {

    @Autowired
    private BoardRepository boardRepository;

    @Test
    public void testInsert() {
        // 1부터 10까지 루프를 돌며 10개의 데이터를 넣습니다.
        IntStream.rangeClosed(1, 10).forEach(i -> {
            Board board = Board.builder()
                    .title("테스트 제목..." + i)
                    .content("테스트 내용..." + i)
                    .writer("user" + (i % 10))
                    .category("공지사항")
                    .regDate(LocalDateTime.now())
                    .modDate(LocalDateTime.now())
                    .build();

            boardRepository.save(board);
        });
        System.out.println("데이터 10개 삽입 완료!");
    }
}