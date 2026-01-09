package com.desk.security.token;

import com.desk.domain.RefreshToken;
import com.desk.repository.RefreshTokenRepository;
import com.desk.security.token.redis.RefreshTokenRedis;
import com.desk.util.JWTUtil;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.annotation.Commit;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Refresh Token 성능 비교 테스트
 * - DB vs Redis 성능 비교
 * - 1000개 데이터로 부하 테스트
 */
@SpringBootTest
@DisplayName("Refresh Token 성능 비교 테스트")
public class RefreshTokenPerformanceTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final int TEST_DATA_COUNT = 1000;
    private static final String TOKEN_KEY_PREFIX = "refresh:token:";
    private static final String FAMILY_KEY_PREFIX = "refresh:family:";
    private static final int REFRESH_TTL_MIN = 60 * 24; // 24시간

    private List<String> testJtis = new ArrayList<>();
    private List<String> testFamilyIds = new ArrayList<>();
    private List<String> testEmails = new ArrayList<>();

    @BeforeEach
    void setUp() {
        // 테스트 데이터 초기화
        testJtis.clear();
        testFamilyIds.clear();
        testEmails.clear();

        // 테스트용 JTI, FamilyId, Email 생성
        for (int i = 0; i < TEST_DATA_COUNT; i++) {
            testJtis.add(UUID.randomUUID().toString().replace("-", ""));
            testFamilyIds.add(UUID.randomUUID().toString());
            testEmails.add("test" + i + "@desk.com");
        }
    }

    @AfterEach
    void tearDown() {
        // 주의: 데이터를 확인하려면 아래 cleanup 메서드들을 주석 처리하세요
        // cleanupDB();
        // cleanupRedis();
    }

    @Test
    @DisplayName("DB에 1000개 Refresh Token 생성 성능 테스트")
    @Transactional
    @Commit // 실제 DB에 저장되도록 설정
    void testDBCreatePerformance() {
        System.out.println("\n========== DB 데이터 생성 테스트 ==========");
        
        long startTime = System.nanoTime();
        
        List<RefreshToken> tokens = new ArrayList<>();
        for (int i = 0; i < TEST_DATA_COUNT; i++) {
            RefreshToken token = createDBToken(i);
            tokens.add(token);
        }
        
        // 배치 저장
        refreshTokenRepository.saveAll(tokens);
        
        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;
        
        System.out.println("DB 생성 시간: " + durationMs + " ms");
        System.out.println("평균 시간: " + (durationMs / (double) TEST_DATA_COUNT) + " ms/개");
        System.out.println("초당 처리량: " + (TEST_DATA_COUNT * 1000.0 / durationMs) + " 건/초");
    }

    @Test
    @DisplayName("Redis에 1000개 Refresh Token 생성 성능 테스트")
    void testRedisCreatePerformance() {
        System.out.println("\n========== Redis 데이터 생성 테스트 ==========");
        
        long startTime = System.nanoTime();
        
        Duration refreshTtl = Duration.ofMinutes(REFRESH_TTL_MIN);
        
        for (int i = 0; i < TEST_DATA_COUNT; i++) {
            RefreshTokenRedis tokenData = createRedisToken(i);
            String tokenKey = TOKEN_KEY_PREFIX + testJtis.get(i);
            
            // 토큰 저장
            redisTemplate.opsForValue().set(tokenKey, tokenData, refreshTtl);
            
            // Family Set에 추가
            String familyKey = FAMILY_KEY_PREFIX + testFamilyIds.get(i);
            redisTemplate.opsForSet().add(familyKey, testJtis.get(i));
            redisTemplate.expire(familyKey, refreshTtl);
        }
        
        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;
        
        System.out.println("Redis 생성 시간: " + durationMs + " ms");
        System.out.println("평균 시간: " + (durationMs / (double) TEST_DATA_COUNT) + " ms/개");
        System.out.println("초당 처리량: " + (TEST_DATA_COUNT * 1000.0 / durationMs) + " 건/초");
    }

    @Test
    @DisplayName("DB 조회 성능 테스트 (1000개 중 랜덤 조회)")
    @Transactional
    void testDBReadPerformance() {
        System.out.println("\n========== DB 조회 성능 테스트 ==========");
        
        // 테스트 데이터 생성
        createDBTestData();
        
        // 랜덤 조회 테스트 (100번)
        int readCount = 100;
        Random random = new Random();
        
        long startTime = System.nanoTime();
        
        for (int i = 0; i < readCount; i++) {
            int randomIndex = random.nextInt(TEST_DATA_COUNT);
            String jti = testJtis.get(randomIndex);
            
            refreshTokenRepository.findByRefreshTokenId(jti);
        }
        
        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;
        
        System.out.println("DB 조회 시간 (" + readCount + "회): " + durationMs + " ms");
        System.out.println("평균 시간: " + (durationMs / (double) readCount) + " ms/회");
        System.out.println("초당 처리량: " + (readCount * 1000.0 / durationMs) + " 건/초");
    }

    @Test
    @DisplayName("Redis 조회 성능 테스트 (1000개 중 랜덤 조회)")
    void testRedisReadPerformance() {
        System.out.println("\n========== Redis 조회 성능 테스트 ==========");
        
        // 테스트 데이터 생성
        createRedisTestData();
        
        // 랜덤 조회 테스트 (100번)
        int readCount = 100;
        Random random = new Random();
        
        long startTime = System.nanoTime();
        
        for (int i = 0; i < readCount; i++) {
            int randomIndex = random.nextInt(TEST_DATA_COUNT);
            String jti = testJtis.get(randomIndex);
            String tokenKey = TOKEN_KEY_PREFIX + jti;
            
            redisTemplate.opsForValue().get(tokenKey);
        }
        
        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;
        
        System.out.println("Redis 조회 시간 (" + readCount + "회): " + durationMs + " ms");
        System.out.println("평균 시간: " + (durationMs / (double) readCount) + " ms/회");
        System.out.println("초당 처리량: " + (readCount * 1000.0 / durationMs) + " 건/초");
    }

    @Test
    @DisplayName("종합 성능 비교: 생성 + 조회 + 업데이트")
    @Transactional
    void testOverallPerformance() {
        System.out.println("\n========== 종합 성능 비교 테스트 ==========");
        
        // ========== DB 테스트 ==========
        System.out.println("\n--- DB 성능 테스트 ---");
        long dbStartTime = System.nanoTime();
        
        // 1. 생성
        List<RefreshToken> dbTokens = new ArrayList<>();
        for (int i = 0; i < TEST_DATA_COUNT; i++) {
            dbTokens.add(createDBToken(i));
        }
        refreshTokenRepository.saveAll(dbTokens);
        
        // 2. 조회 (100번)
        Random random = new Random();
        for (int i = 0; i < 100; i++) {
            int randomIndex = random.nextInt(TEST_DATA_COUNT);
            refreshTokenRepository.findByRefreshTokenId(testJtis.get(randomIndex));
        }
        
        // 3. 업데이트 (50번)
        for (int i = 0; i < 50; i++) {
            int randomIndex = random.nextInt(TEST_DATA_COUNT);
            Optional<RefreshToken> tokenOpt = refreshTokenRepository.findByRefreshTokenId(testJtis.get(randomIndex));
            if (tokenOpt.isPresent()) {
                RefreshToken token = tokenOpt.get();
                token.markAsUsed();
                refreshTokenRepository.save(token);
            }
        }
        
        long dbEndTime = System.nanoTime();
        long dbDurationMs = (dbEndTime - dbStartTime) / 1_000_000;
        System.out.println("DB 총 소요 시간: " + dbDurationMs + " ms");
        
        // ========== Redis 테스트 ==========
        System.out.println("\n--- Redis 성능 테스트 ---");
        long redisStartTime = System.nanoTime();
        
        // 1. 생성
        Duration refreshTtl = Duration.ofMinutes(REFRESH_TTL_MIN);
        for (int i = 0; i < TEST_DATA_COUNT; i++) {
            RefreshTokenRedis tokenData = createRedisToken(i);
            String tokenKey = TOKEN_KEY_PREFIX + testJtis.get(i);
            redisTemplate.opsForValue().set(tokenKey, tokenData, refreshTtl);
            
            String familyKey = FAMILY_KEY_PREFIX + testFamilyIds.get(i);
            redisTemplate.opsForSet().add(familyKey, testJtis.get(i));
            redisTemplate.expire(familyKey, refreshTtl);
        }
        
        // 2. 조회 (100번)
        for (int i = 0; i < 100; i++) {
            int randomIndex = random.nextInt(TEST_DATA_COUNT);
            String tokenKey = TOKEN_KEY_PREFIX + testJtis.get(randomIndex);
            redisTemplate.opsForValue().get(tokenKey);
        }
        
        // 3. 업데이트 (50번)
        for (int i = 0; i < 50; i++) {
            int randomIndex = random.nextInt(TEST_DATA_COUNT);
            String tokenKey = TOKEN_KEY_PREFIX + testJtis.get(randomIndex);
            RefreshTokenRedis tokenData = (RefreshTokenRedis) redisTemplate.opsForValue().get(tokenKey);
            if (tokenData != null) {
                tokenData.setUsed(true);
                redisTemplate.opsForValue().set(tokenKey, tokenData, refreshTtl);
            }
        }
        
        long redisEndTime = System.nanoTime();
        long redisDurationMs = (redisEndTime - redisStartTime) / 1_000_000;
        System.out.println("Redis 총 소요 시간: " + redisDurationMs + " ms");
        
        // ========== 결과 비교 ==========
        System.out.println("\n========== 성능 비교 결과 ==========");
        System.out.println("DB: " + dbDurationMs + " ms");
        System.out.println("Redis: " + redisDurationMs + " ms");
        
        if (dbDurationMs < redisDurationMs) {
            double improvement = ((double) (redisDurationMs - dbDurationMs) / dbDurationMs) * 100;
            System.out.println("DB가 " + String.format("%.2f", improvement) + "% 더 빠름");
        } else {
            double improvement = ((double) (dbDurationMs - redisDurationMs) / dbDurationMs) * 100;
            System.out.println("Redis가 " + String.format("%.2f", improvement) + "% 더 빠름");
        }
    }

    // ========== 헬퍼 메서드 ==========

    private RefreshToken createDBToken(int index) {
        String jti = testJtis.get(index);
        String familyId = testFamilyIds.get(index);
        String email = testEmails.get(index);
        
        // JWT 생성
        Map<String, Object> claims = Map.of(
                "email", email,
                "tokenType", "REFRESH",
                "jti", jti,
                "familyId", familyId,
                "auth_time", Instant.now().getEpochSecond(),
                "amr", "pwd"
        );
        String refreshJwt = JWTUtil.generateToken(claims, REFRESH_TTL_MIN);
        
        return RefreshToken.builder()
                .refreshTokenId(jti)
                .refreshToken(refreshJwt)
                .userEmail(email)
                .familyId(familyId)
                .used(false)
                .userAgentHash("test_user_agent_hash_" + index)
                .ipAddress("192.168.1." + (index % 255))
                .expiresAt(Instant.now().plusSeconds(REFRESH_TTL_MIN * 60))
                .build();
    }

    private RefreshTokenRedis createRedisToken(int index) {
        String jti = testJtis.get(index);
        String familyId = testFamilyIds.get(index);
        String email = testEmails.get(index);
        
        // JWT 생성
        Map<String, Object> claims = Map.of(
                "email", email,
                "tokenType", "REFRESH",
                "jti", jti,
                "familyId", familyId,
                "auth_time", Instant.now().getEpochSecond(),
                "amr", "pwd"
        );
        String refreshJwt = JWTUtil.generateToken(claims, REFRESH_TTL_MIN);
        
        return RefreshTokenRedis.builder()
                .refreshTokenId(jti)
                .refreshToken(refreshJwt)
                .userId(email)
                .userEmail(email)
                .familyId(familyId)
                .used(false)
                .userAgentHash("test_user_agent_hash_" + index)
                .ipAddress("192.168.1." + (index % 255))
                .build();
    }

    private void createDBTestData() {
        List<RefreshToken> tokens = new ArrayList<>();
        for (int i = 0; i < TEST_DATA_COUNT; i++) {
            tokens.add(createDBToken(i));
        }
        refreshTokenRepository.saveAll(tokens);
    }

    private void createRedisTestData() {
        Duration refreshTtl = Duration.ofMinutes(REFRESH_TTL_MIN);
        for (int i = 0; i < TEST_DATA_COUNT; i++) {
            RefreshTokenRedis tokenData = createRedisToken(i);
            String tokenKey = TOKEN_KEY_PREFIX + testJtis.get(i);
            redisTemplate.opsForValue().set(tokenKey, tokenData, refreshTtl);
            
            String familyKey = FAMILY_KEY_PREFIX + testFamilyIds.get(i);
            redisTemplate.opsForSet().add(familyKey, testJtis.get(i));
            redisTemplate.expire(familyKey, refreshTtl);
        }
    }

    private void cleanupDB() {
        refreshTokenRepository.deleteAll();
    }

    private void cleanupRedis() {
        // Redis의 모든 테스트 키 삭제
        for (int i = 0; i < TEST_DATA_COUNT; i++) {
            String tokenKey = TOKEN_KEY_PREFIX + testJtis.get(i);
            redisTemplate.delete(tokenKey);
            
            String familyKey = FAMILY_KEY_PREFIX + testFamilyIds.get(i);
            redisTemplate.delete(familyKey);
        }
    }
    
    // ========== 데이터 확인용 테스트 메서드 ==========
    
    @Test
    @DisplayName("데이터 확인용: DB Refresh Token 개수 확인")
    @Transactional
    void testCheckDBDataCount() {
        long count = refreshTokenRepository.count();
        System.out.println("\n========== DB Refresh Token 개수 ==========");
        System.out.println("현재 DB에 저장된 Refresh Token 개수: " + count);
        
        if (count > 0) {
            // 최근 5개 조회
            List<RefreshToken> allTokens = refreshTokenRepository.findAll();
            List<RefreshToken> recentTokens = allTokens.stream()
                    .limit(5)
                    .toList();
            System.out.println("\n최근 5개 샘플:");
            for (RefreshToken token : recentTokens) {
                System.out.println("  - JTI: " + token.getRefreshTokenId() + 
                                 ", Email: " + token.getUserEmail() + 
                                 ", FamilyId: " + token.getFamilyId());
            }
        }
    }
    
    @Test
    @DisplayName("데이터 확인용: Redis Refresh Token 개수 확인")
    void testCheckRedisDataCount() {
        System.out.println("\n========== Redis 데이터 확인 ==========");
        
        // Redis에 저장된 모든 refresh token 키 개수 확인
        Set<String> keys = redisTemplate.keys(TOKEN_KEY_PREFIX + "*");
        int tokenCount = keys != null ? keys.size() : 0;
        
        System.out.println("Redis에 저장된 Refresh Token 개수: " + tokenCount);
        
        if (tokenCount > 0 && keys != null) {
            // 샘플 5개 조회
            System.out.println("\n샘플 5개:");
            int sampleCount = Math.min(5, keys.size());
            int index = 0;
            for (String key : keys) {
                if (index >= sampleCount) break;
                
                RefreshTokenRedis tokenData = (RefreshTokenRedis) redisTemplate.opsForValue().get(key);
                if (tokenData != null) {
                    System.out.println("  - Key: " + key);
                    System.out.println("    JTI: " + tokenData.getRefreshTokenId() + 
                                     ", Email: " + tokenData.getUserEmail() + 
                                     ", FamilyId: " + tokenData.getFamilyId() + 
                                     ", Used: " + tokenData.isUsed());
                }
                index++;
            }
            
            // Family Set 개수도 확인
            Set<String> familyKeys = redisTemplate.keys(FAMILY_KEY_PREFIX + "*");
            int familyCount = familyKeys != null ? familyKeys.size() : 0;
            System.out.println("\nRedis에 저장된 Family Set 개수: " + familyCount);
        } else {
            System.out.println("Redis에 데이터가 없습니다.");
        }
    }
    
    @Test
    @DisplayName("수동 실행: DB에 1000개 데이터 생성 (확인용)")
    @Transactional
    @Commit
    void testManualDBCreate() {
        System.out.println("\n========== 수동 DB 데이터 생성 ==========");
        createDBTestData();
        long count = refreshTokenRepository.count();
        System.out.println("생성 완료! DB에 저장된 Refresh Token 개수: " + count);
        System.out.println("데이터 확인: SELECT * FROM refresh_token LIMIT 10;");
    }
    
    @Test
    @DisplayName("수동 실행: Redis에 1000개 데이터 생성 (확인용)")
    void testManualRedisCreate() {
        System.out.println("\n========== 수동 Redis 데이터 생성 ==========");
        createRedisTestData();
        
        Set<String> keys = redisTemplate.keys(TOKEN_KEY_PREFIX + "*");
        int count = keys != null ? keys.size() : 0;
        System.out.println("생성 완료! Redis에 저장된 Refresh Token 개수: " + count);
        System.out.println("Redis CLI에서 확인: KEYS refresh:token:*");
        System.out.println("또는 Redis CLI에서: DBSIZE");
    }
}

