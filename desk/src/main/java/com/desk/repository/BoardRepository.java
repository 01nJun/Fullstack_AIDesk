package com.desk.repository;

import com.desk.repository.search.BoardSearch;
import org.springframework.data.jpa.repository.JpaRepository;
import com.desk.domain.Board;

// JpaRepository의 기본 기능 + 우리가 만든 검색(BoardSearch) 기능을 하나로 
public interface BoardRepository extends JpaRepository<Board, Long>, BoardSearch {

}