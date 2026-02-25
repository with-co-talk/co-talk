package com.cotalk.infrastructure.ratelimit;

import com.cotalk.integration.IntegrationTestSecurityConfig;
import com.cotalk.infrastructure.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledIf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Rate Limit 통합 테스트.
 * Testcontainers Redis를 사용하여 Rate Limit 동작을 검증합니다.
 *
 * <p>IP 식별은 {@link RateLimitInterceptor}와 동일하게 X-Real-IP 헤더를 사용한다
 * (X-Forwarded-For는 클라이언트 조작 가능으로 미사용).</p>
 *
 * <p><b>주의:</b> Docker가 실행 중이어야 테스트가 실행됩니다.</p>
 *
 * @author seunggu.lee
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "app.rate-limit.enabled=true"
    }
)
@AutoConfigureMockMvc
@Import({IntegrationTestSecurityConfig.class, RateLimitWebConfig.class})
@ActiveProfiles("ratelimit-test")
@Testcontainers(disabledWithoutDocker = true)
@EnabledIf("isDockerAvailable")
@DisplayName("Rate Limit 통합 테스트")
class RateLimitIntegrationTest {

    /**
     * Docker 사용 가능 여부를 확인한다.
     *
     * @return Docker가 사용 가능하면 true
     */
    static boolean isDockerAvailable() {
        try {
            DockerClientFactory.instance().client();
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    private static final Logger log = LoggerFactory.getLogger(RateLimitIntegrationTest.class);

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Rate Limit 활성화
        registry.add("app.rate-limit.enabled", () -> "true");

        // Testcontainers Redis 설정 (Spring Data Redis - Lettuce)
        // Redisson은 application-ratelimit-test.yml에서 제외됨
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired(required = false)
    private RateLimitInterceptor rateLimitInterceptor;

    @Autowired(required = false)
    private RateLimitProperties rateLimitProperties;

    @Autowired(required = false)
    private RedisTemplate<String, ?> redisTemplate;

    @BeforeEach
    void setUp() {
        // Rate Limit 인터셉터와 설정이 로드되었는지 확인
        log.info("=== Rate Limit 설정 확인 ===");
        log.info("RateLimitInterceptor: {}", rateLimitInterceptor != null ? "등록됨" : "등록 안 됨");
        log.info("RateLimitProperties: {}", rateLimitProperties != null ? "등록됨" : "등록 안 됨");

        if (rateLimitInterceptor == null) {
            throw new IllegalStateException("RateLimitInterceptor가 등록되지 않았습니다. app.rate-limit.enabled=true인지 확인하세요.");
        }
        if (rateLimitProperties == null) {
            throw new IllegalStateException("RateLimitProperties가 등록되지 않았습니다.");
        }

        boolean enabled = rateLimitProperties.isEnabled();
        log.info("Rate Limit enabled: {}", enabled);
        log.info("Rate Limit endpoints: {}", rateLimitProperties.getEndpoints());
        log.info("===========================");

        if (!enabled) {
            throw new IllegalStateException("RateLimitProperties가 활성화되지 않았습니다. enabled=" + enabled);
        }

        // 이전 테스트에서 남은 Rate Limit 키 정리
        cleanupRateLimitKeys();
    }

    private void cleanupRateLimitKeys() {
        if (redisTemplate == null) {
            log.debug("RedisTemplate not available, skipping cleanup");
            return;
        }
        try {
            Set<String> keys = redisTemplate.keys("rate-limit:*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("Cleaned up {} rate limit keys", keys.size());
            }
        } catch (Exception e) {
            log.warn("Failed to cleanup rate limit keys: {}", e.getMessage());
        }
    }

    @Test
    @DisplayName("Rate Limit 이내 요청은 정상 처리")
    void should_allowRequest_when_withinRateLimit() throws Exception {
        // given - Rate Limit: 분당 5회, 고유 IP로 테스트 격리
        String endpoint = "/api/v1/users/search?nickname=test";
        String uniqueIp = "10.0.1." + UUID.randomUUID().toString().substring(0, 3); // 고유 IP

        // when & then - 5번 요청 (허용 범위)
        for (int i = 0; i < 5; i++) {
            var result = mockMvc.perform(get(endpoint)
                            .header("X-Real-IP", uniqueIp))
                    .andExpect(status().isOk())
                    .andReturn();

            // Rate Limit 헤더 확인 (인터셉터가 실행된 경우에만 존재)
            String rateLimitHeader = result.getResponse().getHeader("X-RateLimit-Limit");
            if (rateLimitHeader != null) {
                // Rate Limit이 활성화되어 있으면 헤더 확인
                mockMvc.perform(get(endpoint)
                                .header("X-Real-IP", uniqueIp))
                        .andExpect(header().exists("X-RateLimit-Limit"))
                        .andExpect(header().exists("X-RateLimit-Remaining"));
                break; // 한 번만 확인
            }
        }
    }

    @Test
    @DisplayName("Rate Limit 초과 시 429 에러 반환")
    void should_return429_when_rateLimitExceeded() throws Exception {
        // given - Rate Limit: 분당 5회, 고유 IP로 테스트 격리
        String endpoint = "/api/v1/users/search?nickname=test";
        String uniqueIp = "10.0.2." + UUID.randomUUID().toString().substring(0, 3); // 고유 IP

        // when - 5번 요청 (허용 범위)
        for (int i = 0; i < 5; i++) {
            var result = mockMvc.perform(get(endpoint)
                            .header("X-Real-IP", uniqueIp))
                    .andExpect(status().isOk())
                    .andReturn();

            // Rate Limit 헤더 확인
            String rateLimitHeader = result.getResponse().getHeader("X-RateLimit-Limit");
            log.debug("Request {}: Rate Limit Header = {}", i + 1, rateLimitHeader);

            if (rateLimitHeader == null) {
                log.warn("Rate Limit 헤더가 없습니다. 인터셉터가 실행되지 않았을 수 있습니다.");
            }
        }

        // then - 6번째 요청은 Rate Limit 초과
        mockMvc.perform(get(endpoint)
                        .header("X-Real-IP", uniqueIp))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("X-RateLimit-Limit"))
                .andExpect(header().exists("X-RateLimit-Remaining"))
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    @DisplayName("Rate Limit 초과 시 헤더 값 확인")
    void should_setRateLimitHeaders_when_rateLimitExceeded() throws Exception {
        // given - Rate Limit: 분당 5회, 고유 IP로 테스트 격리
        String endpoint = "/api/v1/users/search?nickname=test";
        String uniqueIp = "10.0.3." + UUID.randomUUID().toString().substring(0, 3); // 고유 IP

        // when - 5번 요청으로 버킷 소진
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get(endpoint)
                    .header("X-Real-IP", uniqueIp));
        }

        // then - 6번째 요청에서 헤더 확인
        mockMvc.perform(get(endpoint)
                        .header("X-Real-IP", uniqueIp))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("X-RateLimit-Limit", "5"))
                .andExpect(header().string("X-RateLimit-Remaining", "0"))
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    @DisplayName("IP별 Rate Limit은 동일 IP에서만 제한 적용")
    void should_limitByIp_when_perUserIsFalse() throws Exception {
        // given - Rate Limit: 분당 5회, IP별 제한, 고유 IP로 테스트 격리
        String endpoint = "/api/v1/users/search?nickname=test";
        String uniqueIp = "10.0.4." + UUID.randomUUID().toString().substring(0, 3); // 고유 IP

        // when - 동일 IP에서 5번 요청
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get(endpoint)
                            .header("X-Real-IP", uniqueIp))
                    .andExpect(status().isOk());
        }

        // then - 6번째 요청은 Rate Limit 초과
        mockMvc.perform(get(endpoint)
                        .header("X-Real-IP", uniqueIp))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("사용자별 Rate Limit은 JWT 토큰 기반으로 제한 적용")
    void should_limitByUser_when_perUserIsTrue() throws Exception {
        // given - Rate Limit: 분당 3회, 사용자별 제한
        String endpoint = "/api/v1/reports/my";
        Long userId = (long) (Math.random() * 100000 + 10000); // 고유 사용자 ID로 테스트 격리
        String validToken = "Bearer " + jwtTokenProvider.generateToken(userId);

        // when - 동일 사용자로 3번 요청
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get(endpoint)
                            .header("Authorization", validToken)
                            .param("userId", String.valueOf(userId)))
                    .andExpect(status().isOk());
        }

        // then - 4번째 요청은 Rate Limit 초과
        mockMvc.perform(get(endpoint)
                        .header("Authorization", validToken)
                        .param("userId", String.valueOf(userId)))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("Rate Limit이 설정되지 않은 엔드포인트는 제한 없음")
    void should_allowUnlimited_when_noRateLimitConfigured() throws Exception {
        // given - Rate Limit이 설정되지 않은 엔드포인트 (/api/v1/friends는 설정되어 있지 않음)
        String endpoint = "/api/v1/friends";

        // when & then - 여러 번 요청해도 Rate Limit 헤더 없음
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get(endpoint)
                            .param("userId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(header().doesNotExist("X-RateLimit-Limit"));
        }
    }

    @Test
    @DisplayName("다른 IP는 독립적인 Rate Limit 버킷을 사용")
    void should_haveIndependentBucket_when_differentIp() throws Exception {
        // given - Rate Limit: 분당 5회, IP별 제한
        String endpoint = "/api/v1/users/search?nickname=test";
        String ip1 = "10.1.1." + UUID.randomUUID().toString().substring(0, 3);
        String ip2 = "10.1.2." + UUID.randomUUID().toString().substring(0, 3);

        // when - IP1에서 5번 요청하여 버킷 소진
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get(endpoint)
                            .header("X-Real-IP", ip1))
                    .andExpect(status().isOk());
        }

        // then - IP1은 제한되지만, IP2는 여전히 요청 가능
        mockMvc.perform(get(endpoint)
                        .header("X-Real-IP", ip1))
                .andExpect(status().isTooManyRequests());

        mockMvc.perform(get(endpoint)
                        .header("X-Real-IP", ip2))
                .andExpect(status().isOk())
                .andExpect(header().string("X-RateLimit-Remaining", "4")); // 첫 요청이므로 4개 남음
    }

    @Test
    @DisplayName("다른 사용자는 독립적인 Rate Limit 버킷을 사용")
    void should_haveIndependentBucket_when_differentUser() throws Exception {
        // given - Rate Limit: 분당 3회, 사용자별 제한
        String endpoint = "/api/v1/reports/my";
        Long userId1 = (long) (Math.random() * 100000 + 20000);
        Long userId2 = (long) (Math.random() * 100000 + 30000);
        String token1 = "Bearer " + jwtTokenProvider.generateToken(userId1);
        String token2 = "Bearer " + jwtTokenProvider.generateToken(userId2);

        // when - 사용자1에서 3번 요청하여 버킷 소진
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get(endpoint)
                            .header("Authorization", token1)
                            .param("userId", String.valueOf(userId1)))
                    .andExpect(status().isOk());
        }

        // then - 사용자1은 제한되지만, 사용자2는 여전히 요청 가능
        mockMvc.perform(get(endpoint)
                        .header("Authorization", token1)
                        .param("userId", String.valueOf(userId1)))
                .andExpect(status().isTooManyRequests());

        mockMvc.perform(get(endpoint)
                        .header("Authorization", token2)
                        .param("userId", String.valueOf(userId2)))
                .andExpect(status().isOk())
                .andExpect(header().string("X-RateLimit-Remaining", "2")); // 첫 요청이므로 2개 남음
    }

    @Test
    @DisplayName("X-RateLimit-Remaining 헤더가 요청마다 감소")
    void should_decreaseRemainingCount_when_eachRequest() throws Exception {
        // given
        String endpoint = "/api/v1/users/search?nickname=test";
        String uniqueIp = "10.2.1." + UUID.randomUUID().toString().substring(0, 3);

        // when & then - 각 요청마다 Remaining 값이 1씩 감소
        for (int i = 0; i < 5; i++) {
            int expectedRemaining = 4 - i; // 5개 제한이므로 첫 요청 후 4, 두 번째 후 3, ...
            mockMvc.perform(get(endpoint)
                            .header("X-Real-IP", uniqueIp))
                    .andExpect(status().isOk())
                    .andExpect(header().string("X-RateLimit-Limit", "5"))
                    .andExpect(header().string("X-RateLimit-Remaining", String.valueOf(expectedRemaining)));
        }
    }

    @Test
    @DisplayName("다른 엔드포인트는 독립적인 Rate Limit 버킷을 사용")
    void should_haveIndependentBucket_when_differentEndpoint() throws Exception {
        // given - /api/v1/users와 /api/v1/chat은 별도 설정
        String usersEndpoint = "/api/v1/users/search?nickname=test";
        String chatEndpoint = "/api/v1/chat/rooms";
        String uniqueIp = "10.3.1." + UUID.randomUUID().toString().substring(0, 3);

        // when - users 엔드포인트에서 5번 요청하여 버킷 소진
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get(usersEndpoint)
                            .header("X-Real-IP", uniqueIp))
                    .andExpect(status().isOk());
        }

        // then - users는 제한되지만, chat은 별도 버킷이므로 요청 가능
        mockMvc.perform(get(usersEndpoint)
                        .header("X-Real-IP", uniqueIp))
                .andExpect(status().isTooManyRequests());

        mockMvc.perform(get(chatEndpoint)
                        .header("X-Real-IP", uniqueIp)
                        .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-RateLimit-Limit")); // chat 엔드포인트도 Rate Limit 적용됨
    }

    @Test
    @DisplayName("JWT 토큰 없이 perUser 엔드포인트 접근 시 IP 기반으로 폴백")
    void should_fallbackToIpLimit_when_noJwtTokenOnPerUserEndpoint() throws Exception {
        // given - perUser=true인 엔드포인트에 토큰 없이 접근
        String endpoint = "/api/v1/reports/my";
        String uniqueIp = "10.4.1." + UUID.randomUUID().toString().substring(0, 3);

        // when - 토큰 없이 3번 요청 (perUser이지만 토큰 없으면 IP 기반)
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get(endpoint)
                            .header("X-Real-IP", uniqueIp)
                            .param("userId", "1"))
                    .andExpect(status().isOk());
        }

        // then - 4번째 요청은 Rate Limit 초과 (IP 기반으로 제한됨)
        mockMvc.perform(get(endpoint)
                        .header("X-Real-IP", uniqueIp)
                        .param("userId", "1"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("X-Real-IP 헤더가 있으면 해당 IP를 사용")
    void should_useXRealIpHeader_when_present() throws Exception {
        // given
        String endpoint = "/api/v1/users/search?nickname=test";
        String uniqueIp = "10.5.1." + UUID.randomUUID().toString().substring(0, 3);

        // when - X-Real-IP로 5번 요청 (허용 범위)
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get(endpoint)
                            .header("X-Real-IP", uniqueIp))
                    .andExpect(status().isOk());
        }

        // then - 6번째 요청은 Rate Limit 초과
        mockMvc.perform(get(endpoint)
                        .header("X-Real-IP", uniqueIp))
                .andExpect(status().isTooManyRequests());

        // 다른 X-Real-IP는 독립적인 버킷을 사용하므로 여전히 요청 가능
        String differentIp = "10.5.2." + UUID.randomUUID().toString().substring(0, 3);
        mockMvc.perform(get(endpoint)
                        .header("X-Real-IP", differentIp))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Rate Limit 초과 후 연속 요청도 모두 429 반환")
    void should_return429Continuously_when_rateLimitExceeded() throws Exception {
        // given
        String endpoint = "/api/v1/users/search?nickname=test";
        String uniqueIp = "10.6.1." + UUID.randomUUID().toString().substring(0, 3);

        // when - 5번 요청으로 버킷 소진
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get(endpoint)
                            .header("X-Real-IP", uniqueIp))
                    .andExpect(status().isOk());
        }

        // then - 이후 모든 요청은 429
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get(endpoint)
                            .header("X-Real-IP", uniqueIp))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(header().string("X-RateLimit-Remaining", "0"));
        }
    }

    @Test
    @DisplayName("같은 설정을 공유하는 하위 경로들도 경로별로 별도 버킷 사용")
    void should_useSeparateBucket_when_differentSubPaths() throws Exception {
        // given - /api/v1/users 설정이 하위 경로에도 적용되지만, 경로별로 별도 버킷
        // /api/v1/users/search?nickname=a 와 /api/v1/users/search?nickname=b 는 같은 URI이므로 같은 버킷
        // 하지만 쿼리 파라미터는 URI에 포함되지 않으므로 같은 버킷 공유
        String endpoint = "/api/v1/users/search";
        String uniqueIp = "10.7.1." + UUID.randomUUID().toString().substring(0, 3);

        // when - endpoint로 5번 요청하여 버킷 소진
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get(endpoint)
                            .header("X-Real-IP", uniqueIp)
                            .param("nickname", "test" + i))
                    .andExpect(status().isOk());
        }

        // then - 같은 URI이므로 6번째 요청은 제한 (쿼리 파라미터가 달라도)
        mockMvc.perform(get(endpoint)
                        .header("X-Real-IP", uniqueIp)
                        .param("nickname", "different"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("동일 경로 동일 IP로 정확히 제한 횟수만큼만 허용")
    void should_allowExactlyLimitCount_when_samePathAndIp() throws Exception {
        // given
        String endpoint = "/api/v1/users/search?nickname=test";
        String uniqueIp = "10.8.1." + UUID.randomUUID().toString().substring(0, 3);
        int limit = 5;

        // when & then - 정확히 5번만 허용
        for (int i = 1; i <= limit; i++) {
            mockMvc.perform(get(endpoint)
                            .header("X-Real-IP", uniqueIp))
                    .andExpect(status().isOk());
        }

        // limit+1번째 요청은 거부
        mockMvc.perform(get(endpoint)
                        .header("X-Real-IP", uniqueIp))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("Retry-After 헤더가 양수 값을 반환")
    void should_returnPositiveRetryAfter_when_rateLimitExceeded() throws Exception {
        // given
        String endpoint = "/api/v1/users/search?nickname=test";
        String uniqueIp = "10.9.1." + UUID.randomUUID().toString().substring(0, 3);

        // when - 버킷 소진
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get(endpoint)
                            .header("X-Real-IP", uniqueIp))
                    .andExpect(status().isOk());
        }

        // then - Retry-After가 양수
        var result = mockMvc.perform(get(endpoint)
                        .header("X-Real-IP", uniqueIp))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andReturn();

        String retryAfter = result.getResponse().getHeader("Retry-After");
        int retryAfterSeconds = Integer.parseInt(retryAfter);
        assert retryAfterSeconds > 0 : "Retry-After should be positive, but was: " + retryAfterSeconds;
    }
}
