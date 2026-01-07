package com.desk.service;

import com.desk.domain.Member;
import com.desk.dto.MemberDTO;
import com.desk.dto.PageRequestDTO;
import com.desk.dto.PageResponseDTO;
import com.desk.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
@Transactional // 트랜잭션 처리 (Lazy Loading 해결)
public class AdminServiceImpl implements AdminService {

    private final MemberRepository memberRepository;

    @Override
    public PageResponseDTO<MemberDTO> getPendingMembers(PageRequestDTO pageRequestDTO, String keyword, String department) {
        Page<Member> result = memberRepository.searchMembers(false, pageRequestDTO, keyword, department);
        return makePageResponse(result, pageRequestDTO);
    }

    @Override
    public PageResponseDTO<MemberDTO> getActiveMembers(PageRequestDTO pageRequestDTO, String keyword, String department) {
        Page<Member> result = memberRepository.searchMembers(true, pageRequestDTO, keyword, department);
        return makePageResponse(result, pageRequestDTO);
    }

    @Override
    public void approveMember(String email) {
        Member member = memberRepository.findById(email).orElseThrow();
        member.changeApproved(true);
    }

    @Override
    public void deleteMember(String email) {
        Member member = memberRepository.findById(email).orElseThrow();
        member.changeDeleted(true);
    }

    private PageResponseDTO<MemberDTO> makePageResponse(Page<Member> result, PageRequestDTO pageRequestDTO) {
        List<MemberDTO> dtoList = result.getContent().stream()
                .map(this::entityToDTO)
                .collect(Collectors.toList());

        return PageResponseDTO.<MemberDTO>withAll()
                .dtoList(dtoList)
                .pageRequestDTO(pageRequestDTO)
                .totalCount((int)result.getTotalElements())
                .build();
    }

    // 엔티티 -> DTO 변환 (안전하게 처리)
    private MemberDTO entityToDTO(Member member) {
        return new MemberDTO(
                member.getEmail(),
                member.getPw() != null ? member.getPw() : "",
                member.getNickname(),
                member.isSocial(),
                member.getDepartment() != null ? member.getDepartment().name() : null,
                member.isApproved(),
                member.getRoleList().stream().map(Enum::name).collect(Collectors.toList()),
                member.isFaceEnabled()
        );
    }
}