package com.cotalk.infrastructure.ratelimit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * RateLimitWebConfig 테스트.
 *
 * @author seunggu.lee
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitWebConfig")
class RateLimitWebConfigTest {

    @Mock
    private RateLimitInterceptor rateLimitInterceptor;

    @Mock
    private InterceptorRegistry registry;

    @Mock
    private InterceptorRegistration registration;

    @InjectMocks
    private RateLimitWebConfig webConfig;

    @Test
    @DisplayName("인터셉터를 /api/** 경로에 등록한다")
    void should_registerInterceptor_forApiPath() {
        // given
        given(registry.addInterceptor(any())).willReturn(registration);
        given(registration.addPathPatterns(any(String.class))).willReturn(registration);
        given(registration.excludePathPatterns(any(String[].class))).willReturn(registration);

        // when
        webConfig.addInterceptors(registry);

        // then
        verify(registry).addInterceptor(rateLimitInterceptor);
        verify(registration).addPathPatterns("/api/**");
    }

    @Test
    @DisplayName("Swagger는 제외하고 인증 API는 Rate Limit 적용한다")
    void should_excludeSwaggerPaths_andIncludeAuthPaths() {
        // given
        given(registry.addInterceptor(any())).willReturn(registration);
        given(registration.addPathPatterns(any(String.class))).willReturn(registration);
        given(registration.excludePathPatterns(any(String[].class))).willReturn(registration);

        // when
        webConfig.addInterceptors(registry);

        // then
        ArgumentCaptor<String[]> excludePatternsCaptor = ArgumentCaptor.forClass(String[].class);
        verify(registration).excludePathPatterns(excludePatternsCaptor.capture());

        String[] excludePatterns = excludePatternsCaptor.getValue();
        assertThat(excludePatterns).contains(
                "/swagger-ui/**",
                "/v3/api-docs/**"
        );
        assertThat(excludePatterns).doesNotContain("/api/v1/auth/**");
    }
}
