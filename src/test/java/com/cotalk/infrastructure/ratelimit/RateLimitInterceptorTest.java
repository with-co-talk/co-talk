package com.cotalk.infrastructure.ratelimit;

import com.cotalk.infrastructure.security.JwtTokenProvider;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.mock.env.MockEnvironment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * RateLimitInterceptor 테스트.
 * ProxyManager의 복잡한 빌더 패턴으로 인해 Rate Limit 통과/초과 시나리오는
 * 통합 테스트(RateLimitIntegrationTest)에서 검증합니다.
 *
 * @author seunggu.lee
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitInterceptor")
class RateLimitInterceptorTest {

    @Mock
    private RateLimitProperties rateLimitProperties;

    @Mock
    private ProxyManager<byte[]> proxyManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private MeterRegistry meterRegistry;

    private MockEnvironment environment;

    private RateLimitInterceptor interceptor;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        environment = new MockEnvironment();
        interceptor = new RateLimitInterceptor(rateLimitProperties, proxyManager, jwtTokenProvider, environment, meterRegistry);
    }

    @Nested
    @DisplayName("preHandle 메서드")
    class PreHandleMethod {

        @Test
        @DisplayName("Rate Limit이 비활성화되어 있으면 통과")
        void should_pass_when_rateLimitDisabled() {
            // given
            given(rateLimitProperties.isEnabled()).willReturn(false);

            // when
            boolean result = interceptor.preHandle(request, response, null);

            // then
            assertThat(result).isTrue();
            verify(proxyManager, never()).builder();
        }

        @Test
        @DisplayName("Rate Limit이 설정되지 않은 엔드포인트는 통과")
        void should_pass_when_noRateLimitConfigured() {
            // given
            given(rateLimitProperties.isEnabled()).willReturn(true);
            given(request.getRequestURI()).willReturn("/api/v1/unknown");
            given(rateLimitProperties.getEndpoints()).willReturn(new ArrayList<>());

            // when
            boolean result = interceptor.preHandle(request, response, null);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("path가 null인 엔드포인트 설정은 무시한다")
        void should_ignoreEndpoint_when_pathIsNull() {
            // given
            RateLimitProperties.EndpointRateLimit endpointWithNullPath = new RateLimitProperties.EndpointRateLimit();
            endpointWithNullPath.setPath(null);
            endpointWithNullPath.setRequestsPerMinute(100);

            given(rateLimitProperties.isEnabled()).willReturn(true);
            given(request.getRequestURI()).willReturn("/api/v1/test");
            given(rateLimitProperties.getEndpoints()).willReturn(List.of(endpointWithNullPath));

            // when
            boolean result = interceptor.preHandle(request, response, null);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("매칭되는 엔드포인트가 없으면 통과")
        void should_pass_when_noMatchingEndpoint() {
            // given
            RateLimitProperties.EndpointRateLimit endpoint = new RateLimitProperties.EndpointRateLimit();
            endpoint.setPath("/api/v2/other");
            endpoint.setRequestsPerMinute(100);

            given(rateLimitProperties.isEnabled()).willReturn(true);
            given(request.getRequestURI()).willReturn("/api/v1/test");
            given(rateLimitProperties.getEndpoints()).willReturn(List.of(endpoint));

            // when
            boolean result = interceptor.preHandle(request, response, null);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should_rate_limit_적용_when_트레일링_슬래시_경로로_우회_시도")
        void should_applyRateLimit_when_trailingSlashVariant() {
            // given: 설정은 "/api/v1/auth/login", 요청은 트레일링 슬래시 변형
            RateLimitProperties.EndpointRateLimit endpoint = new RateLimitProperties.EndpointRateLimit();
            endpoint.setPath("/api/v1/auth/login");
            endpoint.setRequestsPerMinute(5);
            endpoint.setPerUser(false);

            given(rateLimitProperties.isEnabled()).willReturn(true);
            given(request.getRequestURI()).willReturn("/api/v1/auth/login/");
            given(rateLimitProperties.getEndpoints()).willReturn(List.of(endpoint));
            stubBucketAllow();

            // when
            boolean result = interceptor.preHandle(request, response, null);

            // then: limiter가 경로를 해석하여 rate limit을 적용(버킷 소비)했는지 검증
            assertThat(result).isTrue();
            verify(proxyManager).builder();
        }

        @Test
        @DisplayName("should_rate_limit_적용_when_매트릭스변수_경로로_우회_시도")
        void should_applyRateLimit_when_matrixVariantPath() {
            // given
            RateLimitProperties.EndpointRateLimit endpoint = new RateLimitProperties.EndpointRateLimit();
            endpoint.setPath("/api/v1/password/verify-code");
            endpoint.setRequestsPerMinute(5);
            endpoint.setPerUser(false);

            given(rateLimitProperties.isEnabled()).willReturn(true);
            given(request.getRequestURI()).willReturn("/api/v1/password/verify-code;jsessionid=abc");
            given(rateLimitProperties.getEndpoints()).willReturn(List.of(endpoint));
            stubBucketAllow();

            // when
            boolean result = interceptor.preHandle(request, response, null);

            // then
            assertThat(result).isTrue();
            verify(proxyManager).builder();
        }

        @Test
        @DisplayName("should_rate_limit_적용_when_BEST_MATCHING_PATTERN_속성_존재")
        void should_applyRateLimit_when_bestMatchingPatternAttributePresent() {
            // given: MVC가 해석한 best-matching 패턴을 사용
            RateLimitProperties.EndpointRateLimit endpoint = new RateLimitProperties.EndpointRateLimit();
            endpoint.setPath("/api/v1/auth/login");
            endpoint.setRequestsPerMinute(5);
            endpoint.setPerUser(false);

            given(rateLimitProperties.isEnabled()).willReturn(true);
            given(request.getAttribute(
                    org.springframework.web.servlet.HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE))
                    .willReturn("/api/v1/auth/login");
            given(rateLimitProperties.getEndpoints()).willReturn(List.of(endpoint));
            stubBucketAllow();

            // when
            boolean result = interceptor.preHandle(request, response, null);

            // then
            assertThat(result).isTrue();
            verify(proxyManager).builder();
        }

        @Test
        @DisplayName("should_k6_우회헤더_무시_when_prod_프로파일")
        void should_ignoreK6BypassHeader_when_prodProfile() {
            // given: prod 프로파일 + 유효한 k6 토큰 설정 + 일치하는 헤더
            environment.setActiveProfiles("prod");
            RateLimitProperties.EndpointRateLimit endpoint = new RateLimitProperties.EndpointRateLimit();
            endpoint.setPath("/api/v1/auth/login");
            endpoint.setRequestsPerMinute(5);
            endpoint.setPerUser(false);

            given(rateLimitProperties.isEnabled()).willReturn(true);
            given(request.getRequestURI()).willReturn("/api/v1/auth/login");
            given(rateLimitProperties.getEndpoints()).willReturn(List.of(endpoint));
            stubBucketAllow();

            // when
            boolean result = interceptor.preHandle(request, response, null);

            // then: prod에서는 헤더를 읽지 않고 rate limit을 그대로 적용
            assertThat(result).isTrue();
            verify(proxyManager).builder();
            verify(request, never()).getHeader("X-K6-Token");
            verify(rateLimitProperties, never()).getK6BypassToken();
        }

        @Test
        @DisplayName("should_동일버킷_사용_when_URI_변형으로_우회_시도")
        void should_useSameBucket_when_uriVariantsOfSameEndpoint() {
            // given: 동일 엔드포인트의 두 URI 변형(트레일링 슬래시 유무)을 같은 클라이언트(IP)가 호출
            RateLimitProperties.EndpointRateLimit endpoint = new RateLimitProperties.EndpointRateLimit();
            endpoint.setPath("/api/v1/auth/login");
            endpoint.setRequestsPerMinute(5);
            endpoint.setPerUser(false);

            given(rateLimitProperties.isEnabled()).willReturn(true);
            given(rateLimitProperties.getEndpoints()).willReturn(List.of(endpoint));
            given(request.getRemoteAddr()).willReturn("203.0.113.7");

            java.util.List<byte[]> capturedKeys = new ArrayList<>();
            stubBucketCapturingKey(capturedKeys);

            // when: 첫 요청은 raw URI, 두 번째 요청은 트레일링 슬래시 변형
            given(request.getRequestURI()).willReturn("/api/v1/auth/login");
            interceptor.preHandle(request, response, null);
            given(request.getRequestURI()).willReturn("/api/v1/auth/login/");
            interceptor.preHandle(request, response, null);

            // then: 두 변형 모두 동일한 버킷 키(=동일 버킷)를 사용해야 우회가 차단됨
            assertThat(capturedKeys).hasSize(2);
            assertThat(capturedKeys.get(0)).isEqualTo(capturedKeys.get(1));
        }

        @Test
        @DisplayName("should_6번째_시도_차단_when_URI_변형으로_시도수_누적")
        void should_throttleSixthAttempt_when_uriVariantsShareBucket() {
            // given: 분당 5회 제한. 동일 클라이언트가 URI 변형을 섞어 6회 시도
            RateLimitProperties.EndpointRateLimit endpoint = new RateLimitProperties.EndpointRateLimit();
            endpoint.setPath("/api/v1/auth/login");
            endpoint.setRequestsPerMinute(5);
            endpoint.setPerUser(false);

            given(rateLimitProperties.isEnabled()).willReturn(true);
            given(rateLimitProperties.getEndpoints()).willReturn(List.of(endpoint));
            given(request.getRemoteAddr()).willReturn("203.0.113.8");

            // 키별로 단일 버킷을 공유하여 실제 누적 소비를 모사
            stubBucketSharedByKey(5);

            String[] variants = {
                    "/api/v1/auth/login",
                    "/api/v1/auth/login/",
                    "/api/v1/auth/login;jsessionid=a",
                    "/api/v1/auth/login",
                    "/api/v1/auth/login/"
            };
            for (String uri : variants) {
                given(request.getRequestURI()).willReturn(uri);
                boolean result = interceptor.preHandle(request, response, null);
                assertThat(result).isTrue();
            }

            // when & then: 6번째(또 다른 변형) 시도는 동일 버킷이 소진되어 차단되어야 함
            given(request.getRequestURI()).willReturn("/api/v1/auth/login;jsessionid=b");
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> interceptor.preHandle(request, response, null))
                    .isInstanceOf(com.cotalk.domain.exception.RateLimitExceededException.class);
        }

        @Test
        @DisplayName("should_k6_우회_허용_when_비prod_프로파일_토큰일치")
        void should_allowK6Bypass_when_nonProdProfileTokenMatches() {
            // given: 비-prod + 일치하는 k6 토큰 헤더
            given(rateLimitProperties.isEnabled()).willReturn(true);
            given(rateLimitProperties.getK6BypassToken()).willReturn("secret-k6");
            given(request.getHeader("X-K6-Token")).willReturn("secret-k6");

            // when
            boolean result = interceptor.preHandle(request, response, null);

            // then: rate limit 미적용으로 통과, 버킷 미조회
            assertThat(result).isTrue();
            verify(proxyManager, never()).builder();
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private void stubBucketAllow() {
            io.github.bucket4j.distributed.proxy.RemoteBucketBuilder builder =
                    org.mockito.Mockito.mock(io.github.bucket4j.distributed.proxy.RemoteBucketBuilder.class);
            // build()의 선언 반환 타입은 BucketProxy이므로 Bucket이 아닌 BucketProxy를 모킹해야
            // Mockito가 WrongTypeOfReturnValue를 던지지 않는다.
            io.github.bucket4j.distributed.BucketProxy bucket =
                    org.mockito.Mockito.mock(io.github.bucket4j.distributed.BucketProxy.class);
            given(proxyManager.builder()).willReturn(builder);
            org.mockito.Mockito.doReturn(bucket).when(builder).build(
                    org.mockito.ArgumentMatchers.any(byte[].class),
                    org.mockito.ArgumentMatchers.<java.util.function.Supplier>any());
            given(bucket.tryConsume(1)).willReturn(true);
            given(bucket.getAvailableTokens()).willReturn(4L);
        }

        /**
         * 항상 통과하는 버킷을 stub하되, build()에 전달된 key(byte[])를 capture한다.
         */
        @SuppressWarnings({"unchecked", "rawtypes"})
        private void stubBucketCapturingKey(java.util.List<byte[]> capturedKeys) {
            io.github.bucket4j.distributed.proxy.RemoteBucketBuilder builder =
                    org.mockito.Mockito.mock(io.github.bucket4j.distributed.proxy.RemoteBucketBuilder.class);
            io.github.bucket4j.distributed.BucketProxy bucket =
                    org.mockito.Mockito.mock(io.github.bucket4j.distributed.BucketProxy.class);
            given(proxyManager.builder()).willReturn(builder);
            org.mockito.Mockito.doAnswer(invocation -> {
                capturedKeys.add(invocation.getArgument(0, byte[].class));
                return bucket;
            }).when(builder).build(
                    org.mockito.ArgumentMatchers.any(byte[].class),
                    org.mockito.ArgumentMatchers.<java.util.function.Supplier>any());
            given(bucket.tryConsume(1)).willReturn(true);
            given(bucket.getAvailableTokens()).willReturn(4L);
        }

        /**
         * key(String 표현) 별로 단일 버킷을 공유하여, capacity회까지만 tryConsume이 성공하도록 모사한다.
         * 동일 버킷 키를 사용하는 URI 변형들이 시도수를 누적 소비하는지 검증하기 위함이다.
         */
        @SuppressWarnings({"unchecked", "rawtypes"})
        private void stubBucketSharedByKey(int capacity) {
            io.github.bucket4j.distributed.proxy.RemoteBucketBuilder builder =
                    org.mockito.Mockito.mock(io.github.bucket4j.distributed.proxy.RemoteBucketBuilder.class);
            java.util.Map<String, io.github.bucket4j.distributed.BucketProxy> bucketsByKey = new java.util.HashMap<>();
            java.util.Map<String, int[]> remainingByKey = new java.util.HashMap<>();
            given(proxyManager.builder()).willReturn(builder);
            org.mockito.Mockito.doAnswer(invocation -> {
                byte[] keyBytes = invocation.getArgument(0, byte[].class);
                String keyStr = new String(keyBytes, java.nio.charset.StandardCharsets.UTF_8);
                return bucketsByKey.computeIfAbsent(keyStr, k -> {
                    int[] remaining = {capacity};
                    remainingByKey.put(k, remaining);
                    io.github.bucket4j.distributed.BucketProxy bucket =
                            org.mockito.Mockito.mock(io.github.bucket4j.distributed.BucketProxy.class);
                    given(bucket.tryConsume(1)).willAnswer(inv -> {
                        if (remaining[0] > 0) {
                            remaining[0]--;
                            return true;
                        }
                        return false;
                    });
                    given(bucket.getAvailableTokens()).willAnswer(inv -> (long) remaining[0]);
                    return bucket;
                });
            }).when(builder).build(
                    org.mockito.ArgumentMatchers.any(byte[].class),
                    org.mockito.ArgumentMatchers.<java.util.function.Supplier>any());
        }
    }
}
