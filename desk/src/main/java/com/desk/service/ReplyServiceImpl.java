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
        Reply reply = modelMapper.map(replyDTO, Reply.class);
        Board board = Board.builder().bno(replyDTO.getBno()).build();
        reply.setBoard(board);

        log.info("댓글 등록: " + reply);
        return replyRepository.save(reply).getRno();
    }

    @Override
    public ReplyDTO read(Long rno) {
        return replyRepository.findById(rno)
                .map(reply -> modelMapper.map(reply, ReplyDTO.class))
                .orElseThrow();
    }

    /**
     * 댓글 수정: [표의 규칙] 오직 본인(닉네임 일치)만 가능
     */
    @Override
    public void modify(ReplyDTO replyDTO) {
        Reply reply = replyRepository.findById(replyDTO.getRno()).orElseThrow();

        String currentUserNickname = getCurrentUserNickname();

        log.info("수정 시도 - 현재유저: " + currentUserNickname + " / 작성자: " + reply.getReplyer());

        // [핵심 로직] 작성자와 현재 로그인한 유저의 닉네임이 다르면 거부
        if (!reply.getReplyer().equals(currentUserNickname)) {
            throw new RuntimeException("수정 권한이 없습니다. 본인만 수정할 수 있습니다.");
        }

        reply.changeText(replyDTO.getReplyText());
        replyRepository.save(reply);
    }

    /**
     * 댓글 삭제: [표의 규칙] 본인 또는 관리자(ADMIN) 가능
     */
    @Override
    public void remove(Long rno) {
        Reply reply = replyRepository.findById(rno).orElseThrow();

        String currentUserNickname = getCurrentUserNickname();
        boolean isAdmin = checkAdminRole();

        log.info("삭제 시도 - 현재유저: " + currentUserNickname + " / 작성자: " + reply.getReplyer() + " / 관리자여부: " + isAdmin);

        // [핵심 로직] 작성자 본인이거나 관리자(ROLE_ADMIN)인 경우만 삭제 허용
        if (reply.getReplyer().equals(currentUserNickname) || isAdmin) {
            replyRepository.deleteById(rno);
        } else {
            throw new RuntimeException("삭제 권한이 없습니다.");
        }
    }

    @Override
    public PageResponseDTO<ReplyDTO> getListOfBoard(Long bno, PageRequestDTO pageRequestDTO) {
        Pageable pageable = PageRequest.of(
                pageRequestDTO.getPage() <= 0 ? 0 : pageRequestDTO.getPage() - 1,
                pageRequestDTO.getSize(),
                Sort.by("rno").ascending());

        Page<Reply> result = replyRepository.listOfBoard(bno, pageable);

        List<ReplyDTO> dtoList = result.getContent().stream()
                .map(reply -> modelMapper.map(reply, ReplyDTO.class))
                .collect(Collectors.toList());

        return PageResponseDTO.<ReplyDTO>withAll()
                .pageRequestDTO(pageRequestDTO)
                .dtoList(dtoList)
                .totalCount((int)result.getTotalElements())
                .build();
    }

    // --- [중요 수정] 현재 로그인한 사용자의 '닉네임'을 가져오는 로직 ---
    private String getCurrentUserNickname() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) return null;

        Object principal = auth.getPrincipal();

        // 1. JWT 등으로 로그인한 사용자가 MemberDTO(UserDetails) 타입인 경우
        if (principal instanceof MemberDTO) {
            return ((MemberDTO) principal).getNickname(); // DTO에 있는 닉네임을 반환
        }

        // 2. 만약 MemberDTO가 아니라 단순 문자열(이메일 등)이라면 getName 반환
        return auth.getName();
    }

    private boolean checkAdminRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
