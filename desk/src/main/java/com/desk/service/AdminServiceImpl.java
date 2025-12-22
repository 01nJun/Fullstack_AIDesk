package com.desk.service;

import com.desk.domain.Member;
import com.desk.dto.MemberDTO;
import com.desk.dto.PageRequestDTO;
import com.desk.dto.PageResponseDTO;
import com.desk.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    public List<MemberDTO> getPendingMembers() {
        List<Member> members = memberRepository.findByIsApprovedFalseAndIsDeletedFalse();
        
        return members.stream()
                .map(this::entityToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public PageResponseDTO<MemberDTO> getActiveMembers(PageRequestDTO pageRequestDTO) {
        Pageable pageable = PageRequest.of(
            pageRequestDTO.getPage() - 1,
            pageRequestDTO.getSize(),
            Sort.by("email").descending()
        );

        Page<Member> result = memberRepository.findByIsApprovedTrueAndIsDeletedFalse(pageable);
        
        List<MemberDTO> dtoList = result.getContent().stream()
                .map(this::entityToDTO)
                .collect(Collectors.toList());

        return PageResponseDTO.<MemberDTO>withAll()
                .dtoList(dtoList)
                .pageRequestDTO(pageRequestDTO)
                .totalCount((int)result.getTotalElements())
                .build();
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

    // 엔티티 -> DTO 변환 (안전하게 처리)
    private MemberDTO entityToDTO(Member member) {
        return new MemberDTO(
            member.getEmail(),
            member.getPw() != null ? member.getPw() : "", 
            member.getNickname(),
            member.isSocial(),
            member.getDepartment() != null ? member.getDepartment().name() : null,
            member.isApproved(),
            member.getRoleList().stream().map(Enum::name).collect(Collectors.toList())
        );
    }
}