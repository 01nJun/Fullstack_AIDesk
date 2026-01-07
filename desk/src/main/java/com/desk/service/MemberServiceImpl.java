package com.desk.service;

import com.desk.domain.Department;
import com.desk.domain.Member;
import com.desk.domain.MemberRole;
import com.desk.dto.MemberDTO;
import com.desk.dto.MemberJoinDTO;
import com.desk.dto.MemberModifyDTO;
import com.desk.dto.PageRequestDTO;
import com.desk.dto.PageResponseDTO;
import com.desk.repository.MemberRepository;
import com.desk.util.MemberExistException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
public class MemberServiceImpl implements MemberService {

    // DB에서 회원 데이터를 조회, 저장
    private final MemberRepository memberRepository;
    // 비밀번호 암호화 처리
    private final PasswordEncoder passwordEncoder;

    @Override // 카카오 회원 정보 조회
    public MemberDTO getKakaoMember(String accessToken) {

        // 카카오에서 이메일 가져오기
        String email = getEmailFromKakaoAccessToken(accessToken);

        log.info("email: " + email );

        // DB에 이미 회원이 있다면 → DTO로 변환 후 반환
        Optional<Member> result = memberRepository.findById(email);

        // 기존의 회원
        if(result.isPresent()){
            MemberDTO memberDTO = entityToDTO(result.get());

            return memberDTO;
        }

        // DB에 없는 회원이면 닉네임은 '소셜회원'으로 패스워드는 임의로 생성 후 반환
        Member socialMember = makeSocialMember(email);
        log.info("socialMember: {}", socialMember);
        memberRepository.save(socialMember);
        log.info("저장 완료");

        MemberDTO memberDTO = entityToDTO(socialMember);

        return memberDTO;
    }

    private String getEmailFromKakaoAccessToken(String accessToken){

        // 카카오에서 사용자 정보를 가져올 수 있는 REST API URL, 이 URL에 GET 요청을 보내면 사용자 프로필 정보가 JSON 형태로 돌아옵니다.
        String kakaoGetUserURL = "https://kapi.kakao.com/v2/user/me";

        // accessToken이 없으면 실행할 수 없으므로 예외 처리
        if(accessToken == null){
            throw new RuntimeException("Access Token is null");
        }
        // RestTemplate : Spring에서 REST API 호출을 쉽게 할 수 있게 해주는 클래스, HTTP 요청 보내고, 응답 받는 데 사용
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken); // 카카오 서버가 요청을 인증하도록 Bearer 토큰 방식으로 전달
        headers.add("Content-Type","application/x-www-form-urlencoded"); // 요청 방식 지정 (GET에서도 헤더를 명시할 수 있음)
        // HttpEntity : HTTP 요청 헤더와 바디를 담는 객체, 여기서는 GET 요청이라 바디는 필요 없고, 헤더만 담음
        HttpEntity<String> entity = new HttpEntity<>(headers);

        // URL을 안전하게 조합하고 URI 객체로 만드는 역할
        UriComponents uriBuilder = UriComponentsBuilder.fromHttpUrl(kakaoGetUserURL).build();

        ResponseEntity<LinkedHashMap> response =  // 결과는 JSON을 LinkedHashMap 형태로 변환
                restTemplate.exchange( // exchange → REST API 호출
                        uriBuilder.toString(),
                        HttpMethod.GET,
                        entity,
                        LinkedHashMap.class);

        log.info(response);

        LinkedHashMap<String, LinkedHashMap> bodyMap = response.getBody(); // 카카오 API의 JSON 구조를 Map 구조로 변환

        log.info("------------------------------");
        log.info(bodyMap);

        // kakao_account 키의 value는 Map 형태 - 여기서 이메일, 프로필 등 다양한 정보 추출 가능
        LinkedHashMap<String, String> kakaoAccount = bodyMap.get("kakao_account");

        log.info("kakaoAccount: " + kakaoAccount);

        // 최종적으로 사용자 이메일을 반환, 이메일이 존재하지 않으면 null 반환 가능
        return kakaoAccount.get("email");

    }

    private String makeTempPassword() {

        // 멀티스레드 환경에서 여러 스레드가 공유된 자원(객체, 함수, 변수 등)
        // 동시에 접근하는 것을 조절하고 조율하는 방법
        StringBuffer buffer = new StringBuffer();

        // 길이 10, A~z 범위 임의 문자 생성
        for(int i = 0;  i < 10; i++){
            buffer.append((char)((int)(Math.random()*55) + 65));
        }
        return buffer.toString();
    }

    // 소셜 회원 객체 생성 로직 수정
    private Member makeSocialMember(String email) {

        String tempPassword = makeTempPassword();

        //  log.info("tempPassword: " + tempPassword);

        String nickname = "소셜회원"; // 임시 닉네임

        Member member = Member.builder()
                .email(email)
                .pw(passwordEncoder.encode(tempPassword))
                .nickname(nickname)
                .social(true)
                .department(null) // 부서 미정 (나중에 수정 페이지에서 입력받음)
                .isApproved(false) // 승인 대기
                .isDeleted(false)
                .build();

        member.addRole(MemberRole.USER);

        return member;
    }


    @Override
    public void modifyMember(MemberModifyDTO memberModifyDTO) {
        log.info("modify DTO: " + memberModifyDTO);
        Optional<Member> result = memberRepository.findById(memberModifyDTO.getEmail());
        Member member = result.orElseThrow();

        member.changePw(passwordEncoder.encode(memberModifyDTO.getPw()));
        member.changeSocial(false); // 소셜 연동 해제 여부는 정책에 따라 결정 (여기서는 일반회원 전환)
        member.changeNickname(memberModifyDTO.getNickname());

        // 부서 업데이트
        if(memberModifyDTO.getDepartment() != null){
            member.changeDepartment(Department.valueOf(memberModifyDTO.getDepartment()));
        }

        memberRepository.save(member);
    }
    @Override // 회원 가입
    public void join(MemberJoinDTO memberJoinDTO) {

        String email = memberJoinDTO.getEmail();

        // 1. 이메일 중복 검사
        if(memberRepository.existsById(email)){
            throw new MemberExistException();
        }

        // 2. 회원 엔티티 생성
        Department dept = Department.valueOf(memberJoinDTO.getDepartment());

        Member member = Member.builder()
                .email(email)
                .pw(passwordEncoder.encode(memberJoinDTO.getPw()))
                .nickname(memberJoinDTO.getNickname())
                .social(false)
                .department(dept) // 부서 설정
                .isApproved(false) // 승인 대기
                .isDeleted(false)
                .build();

        // 3. 기본 권한 설정 (USER)
        member.addRole(MemberRole.USER);

        // 4. 저장
        memberRepository.save(member);
    }

    @Override
    public PageResponseDTO<MemberDTO> searchActiveMembers(PageRequestDTO pageRequestDTO, String keyword, String department) {
        // 승인된 멤버만 검색 (isApproved = true)
        Page<Member> result = memberRepository.searchMembers(true, pageRequestDTO, keyword, department);
        return makePageResponse(result, pageRequestDTO);
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

}