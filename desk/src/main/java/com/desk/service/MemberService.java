package com.desk.service;

import com.desk.domain.Member;
import com.desk.dto.MemberDTO;
import com.desk.dto.MemberJoinDTO;
import com.desk.dto.MemberModifyDTO;
import com.desk.dto.PageRequestDTO;
import com.desk.dto.PageResponseDTO;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Transactional // 이 인터페이스를 구현하는 클래스의 모든 메서드는 트랜잭션 안에서 실행됩니다. 즉, DB 수정 시 오류가 나면 롤백(Rollback)됩니다.
public interface MemberService {
    
    // 카카오 로그인 시 액세스 토큰을 받아 회원 정보를 가져오는 기능
    MemberDTO getKakaoMember(String accessToken);

     void modifyMember(MemberModifyDTO memberModifyDTO);

     void join(MemberJoinDTO memberJoinDTO);

     // 채팅용 멤버 검색 (관리자 아닌 사람도 가능, 승인된 멤버만)
     PageResponseDTO<MemberDTO> searchActiveMembers(PageRequestDTO pageRequestDTO, String keyword, String department);

     default MemberDTO entityToDTO(Member member){
        
        MemberDTO dto = new MemberDTO(
            member.getEmail(),
             member.getPw(), 
             member.getNickname(), 
             member.isSocial(), 
             member.getDepartment() != null ? member.getDepartment().name() : null,
             member.isApproved(),
             member.getRoleList().stream().map(memberRole -> memberRole.name()).collect(Collectors.toList()),
             member.isFaceEnabled());
        return dto;
    }
}