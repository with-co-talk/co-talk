package com.cotalk.infrastructure.ratelimit;

import com.cotalk.infrastructure.security.JwtTokenProvider;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

    private RateLimitInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new RateLimitInterceptor(rateLimitProperties, proxyManager, jwtTokenProvider);
    }

    @Nested
    @DisplayName("preHandle 메서드")
    class PreHandleMethod {

        @Test
        @DisplayName("Rate Limit이 비활성화되어 있으면 통과")
        void should_pass_when_rateLimitDisabled() {
            // given
            given(request.getRequestURI()).willReturn("/api/v1/test");
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
    }
}
