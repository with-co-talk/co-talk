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
            given(request.getRequestURI()).willReturn("/api/v1/auth/login");
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
    }
}
