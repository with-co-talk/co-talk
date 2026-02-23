package com.cotalk.infrastructure.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Rate Limit 인터셉터를 웹 MVC에 등록하는 설정 클래스.
 * API 요청에 대한 Rate Limiting을 적용한다.
 *
 * <p>인증 API, Swagger UI, API 문서는 Rate Limiting에서 제외된다.</p>
 *
 * <p>이 설정은 {@code app.rate-limit.enabled=true}이고
 * {@link RateLimitInterceptor} 빈이 존재할 때만 활성화된다.</p>
 *
 * @author seunggu.lee
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnBean(RateLimitInterceptor.class)
@ConditionalOnProperty(name = "app.rate-limit.enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitWebConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    /**
     * Rate Limit 인터셉터를 등록한다.
     * /api/** 경로에 적용되며, 인증 API와 문서 API는 제외된다.
     *
     * @param registry 인터셉터 레지스트리
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("Registering RateLimitInterceptor with path pattern: /api/**");
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/swagger-ui/**",
                        "/v3/api-docs/**"
                );
        log.info("RateLimitInterceptor registered successfully");
    }
}
