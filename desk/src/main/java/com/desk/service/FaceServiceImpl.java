package com.desk.service;

import com.desk.domain.Member;
import com.desk.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class FaceServiceImpl implements FaceService {

    // PostgreSQL 전용 JdbcTemplate (DataSourceConfig에서 설정한 빈 이름)
    @Qualifier("postgresJdbcTemplate")
    private final JdbcTemplate pgJdbcTemplate;

    // MariaDB 전용 Repository
    private final MemberRepository memberRepository;

    // AI 파이썬 서버 통신용 WebClient
    private final WebClient.Builder webClientBuilder;

    /**
     * AI 서버로부터 얼굴 임베딩 벡터를 추출하는 공통 메서드
     */
    private float[] getFaceVector(MultipartFile mf) throws IOException {
        WebClient webClient = webClientBuilder.build();

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", mf.getBytes())
                .filename(mf.getOriginalFilename())
                .contentType(MediaType.valueOf(mf.getContentType()));

        try {
            // 파이썬 AI 서버 호출 (포트 50001)
            Map<String, Object> response = webClient.post()
                    .uri("http://localhost:50001/get-face-vector")
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null || !response.containsKey("vector")) {
                throw new RuntimeException("AI 서버로부터 벡터를 가져오지 못했습니다.");
            }

            List<Double> doubleList = (List<Double>) response.get("vector");
            float[] vector = new float[doubleList.size()];
            for (int i = 0; i < doubleList.size(); i++) {
                vector[i] = doubleList.get(i).floatValue();
            }
            return vector;
        } catch (Exception e) {
            log.error("AI 서버 통신 에러: {}", e.getMessage());
            throw new RuntimeException("얼굴 분석 중 오류가 발생했습니다.");
        }
    }

    /**
     * 얼굴 로그인: 이미지로 사용자 찾기
     */
    @Override
    public String findFace(MultipartFile mf) throws IOException {
        float[] vector = getFaceVector(mf);
        String strVector = Arrays.toString(vector).replace(" ", "");

        // L2 거리가 가장 가까운 1명 찾기
        String sql = """
                SELECT content, (embedding <-> ?::vector) AS similarity
                FROM face_vector_store
                ORDER BY embedding <-> ?::vector
                LIMIT 1
                """;

        List<Map<String, Object>> list = pgJdbcTemplate.queryForList(sql, strVector, strVector);

        if (list == null || list.isEmpty()) {
            log.info("검색 결과 없음");
            return null;
        }

        double similarity = (Double) list.get(0).get("similarity");
        String email = (String) list.get(0).get("content");
        log.info("조회된 사용자: {}, 유사도(L2 거리): {}", email, similarity);

        // 임계값 0.7 이내인 경우만 해당 사용자 email 반환
        if (similarity > 0.7) {
            log.info("유사도 임계값 초과 (인증 실패)");
            return null;
        }

        return email;
    }

    /**
     * 얼굴 등록: PostgreSQL 저장 + MariaDB 상태 변경
     */
    @Override
    @Transactional // MariaDB 상태 변경을 위해 트랜잭션 적용
    public void registerFace(String email, MultipartFile mf) throws IOException {
        // 1. 벡터 추출
        float[] vector = getFaceVector(mf);
        String strVector = Arrays.toString(vector).replace(" ", "");

        // 2. PostgreSQL 저장 (기존 데이터 있으면 삭제 후 등록)
        pgJdbcTemplate.update("DELETE FROM face_vector_store WHERE content = ?", email);

        String sql = """
                INSERT INTO face_vector_store (content, embedding)
                VALUES (?, ?::vector)
                """;
        pgJdbcTemplate.update(sql, email, strVector);
        log.info("PostgreSQL 벡터 저장 완료: {}", email);

        // 3. MariaDB의 Member 엔티티 상태 변경 (faceEnabled = true)
        Member member = memberRepository.findById(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + email));

        member.changeFaceEnabled(true);
        memberRepository.save(member);
        log.info("MariaDB 회원 얼굴 로그인 활성화 완료: {}", email);
    }

    /**
     * 얼굴 로그인 사용 여부 토글 (MariaDB만 수정)
     */
    @Override
    @Transactional
    public void updateFaceStatus(String email, boolean status) {
        Member member = memberRepository.findById(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + email));

        member.changeFaceEnabled(status);
        memberRepository.save(member);
        log.info("회원 얼굴 로그인 상태 변경: {} -> {}", email, status);
    }
}