package com.cotalk.infrastructure.ratelimit;

import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.mockito.Mockito.mock;

/**
 * 테스트 환경에서 Rate Limit 관련 빈을 모킹하는 설정
 * Rate Limit이 비활성화된 테스트 환경에서도 RateLimitInterceptor가 정상 작동하도록 함
 */
@TestConfiguration
public class RateLimitTestConfiguration {

    @Bean
    @Primary
    public ProxyManager<byte[]> bucket4jProxyManager() {
        return mock(ProxyManager.class);
    }

    @Bean
    @Primary
    public RateLimitProperties rateLimitProperties() {
        return mock(RateLimitProperties.class);
    }
}
