package com.desk.service;

import com.desk.domain.Board;
import com.desk.domain.Reply;
import com.desk.dto.MemberDTO; // MemberDTO 패키지 경로 확인!
import com.desk.dto.PageRequestDTO;
import com.desk.dto.PageResponseDTO;
import com.desk.dto.ReplyDTO;
import com.desk.repository.ReplyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
@Transactional
public class ReplyServiceImpl implements ReplyService {

    private final ReplyRepository replyRepository;
    private final ModelMapper modelMapper;

    @Override
    public Long register(ReplyDTO replyDTO) {
        // [수정] ModelMapper 대신 빌더를 사용하여 매핑 에러 방지
        Reply reply = Reply.builder()
                .replyText(replyDTO.getReplyText())
                .replyer(replyDTO.getReplyer())
                .board(Board.builder().bno(replyDTO.getBno()).build())
                .build();

        // 대댓글 로직: 부모 번호가 있다면 직접 찾아서 연결
        if (replyDTO.getParentRno() != null && replyDTO.getParentRno() > 0) {
            Reply parent = replyRepository.findById(replyDTO.getParentRno())
                    .orElseThrow(() -> new RuntimeException("부모 댓글이 존재하지 않습니다."));
            reply.setParent(parent); // 엔티티의 필드명이 parent라면 setParent 사용
        }

        log.info("댓글/대댓글 등록 시도: " + reply);
        return replyRepository.save(reply).getRno();
    }

    @Override
    public ReplyDTO read(Long rno) {
        Reply reply = replyRepository.findById(rno).orElseThrow();
        ReplyDTO dto = modelMapper.map(reply, ReplyDTO.class);

        if(reply.getParent() != null) {
            dto.setParentRno(reply.getParent().getRno());
        }
        return dto;
    }

    @Override
    public void modify(ReplyDTO replyDTO) {
        Reply reply = replyRepository.findById(replyDTO.getRno()).orElseThrow();
        String currentUserNickname = getCurrentUserNickname();

        log.info("수정 시도 - 유저: {}, 작성자: {}", currentUserNickname, reply.getReplyer());

        // [보안] 본인 확인
        if (reply.getReplyer() == null || !reply.getReplyer().equals(currentUserNickname)) {
            throw new RuntimeException("수정 권한이 없습니다.");
        }

        reply.changeText(replyDTO.getReplyText());
        replyRepository.save(reply);
    }

    @Override
    public void remove(Long rno) {
        Reply reply = replyRepository.findById(rno).orElseThrow();
        String currentUserNickname = getCurrentUserNickname();
        boolean isAdmin = checkAdminRole();

        log.info("삭제 시도 - 유저: {}, 작성자: {}, 관리자: {}", currentUserNickname, reply.getReplyer(), isAdmin);

        // [보안] 본인 또는 관리자 확인
        if (isAdmin || (reply.getReplyer() != null && reply.getReplyer().equals(currentUserNickname))) {
            replyRepository.deleteById(rno);
        } else {
            throw new RuntimeException("삭제 권한이 없습니다.");
        }
    }

    @Override
    public PageResponseDTO<ReplyDTO> getListOfBoard(Long bno, PageRequestDTO pageRequestDTO) {
        // [수정] 정렬은 Repository의 @Query에서 coalesce로 처리하므로 Sort는 비웁니다.
        Pageable pageable = PageRequest.of(
                pageRequestDTO.getPage() <= 0 ? 0 : pageRequestDTO.getPage() - 1,
                pageRequestDTO.getSize()
        );

        Page<Reply> result = replyRepository.listOfBoard(bno, pageable);

        List<ReplyDTO> dtoList = result.getContent().stream()
                .map(reply -> {
                    ReplyDTO dto = modelMapper.map(reply, ReplyDTO.class);
                    if (reply.getParent() != null) {
                        dto.setParentRno(reply.getParent().getRno());
                    }
                    return dto;
                })
                .collect(Collectors.toList());

        return PageResponseDTO.<ReplyDTO>withAll()
                .pageRequestDTO(pageRequestDTO)
                .dtoList(dtoList)
                .totalCount((int) result.getTotalElements())
                .build();
    }

    // --- 보안 헬퍼 메서드 ---
    private String getCurrentUserNickname() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        Object principal = auth.getPrincipal();
        if (principal instanceof MemberDTO) {
            return ((MemberDTO) principal).getNickname();
        }
        return auth.getName();
    }

    private boolean checkAdminRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}