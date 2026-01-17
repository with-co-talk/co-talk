package com.cotalk.infrastructure.ratelimit;

import com.cotalk.domain.exception.RateLimitExceededException;
import com.cotalk.infrastructure.security.JwtTokenProvider;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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

    @Mock
    private Bucket bucket;

    private RateLimitInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new RateLimitInterceptor(rateLimitProperties, proxyManager, jwtTokenProvider);
    }

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

    // Note: ProxyManager.builder()의 반환 타입이 복잡하여 단위 테스트가 어렵습니다.
    // 실제 Rate Limit 동작은 통합 테스트나 실제 Redis 환경에서 검증하는 것이 적절합니다.
}
