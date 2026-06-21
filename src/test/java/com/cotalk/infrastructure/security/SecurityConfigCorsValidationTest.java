package com.cotalk.infrastructure.security;

import com.cotalk.infrastructure.config.properties.AppProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * SecurityConfig의 CORS fail-open 방어(M-2) 단위 테스트.
 *
 * <p>{@code allowCredentials=true} 상태에서 와일드카드("*")/"null" 오리진이
 * 설정되면 시작 시점에 예외로 기동을 실패시키는지 검증한다.</p>
 *
 * @author seunggu.lee
 */
@DisplayName("SecurityConfig CORS 검증")
class SecurityConfigCorsValidationTest {

    private SecurityConfig newConfig(String allowedOrigins) {
        JwtAuthenticationFilter filter = mock(JwtAuthenticationFilter.class);
        AppProperties appProperties = new AppProperties(
                allowedOrigins,
                new AppProperties.Cors(allowedOrigins),
                new AppProperties.Redis("chat:room:", "user:event:"),
                new AppProperties.PasswordReset(30),
                new AppProperties.Terms("1.0", "1.0"),
                new AppProperties.Encryption("dGhpc2lzYXRlc3RrZXlmb3JkZXZlbG9wbWVudG9ubHk=", false),
                new AppProperties.Swagger("http://localhost:8080", "API 서버"),
                AppProperties.Search.of("dGVzdC1ibGluZC1pbmRleC1zZWNyZXQtZm9yLXVuaXQtdGVzdHM="),
                new AppProperties.Lock(false)
        );
        return new SecurityConfig(filter, appProperties);
    }

    @Test
    @DisplayName("should_예외_발생_when_와일드카드_오리진_with_자격증명")
    void should_throw_when_wildcardOriginWithCredentials() {
        // given
        SecurityConfig config = newConfig("*");

        // when & then
        assertThatThrownBy(config::validateCorsConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("와일드카드");
    }

    @Test
    @DisplayName("should_예외_발생_when_null_오리진_with_자격증명")
    void should_throw_when_nullStringOriginWithCredentials() {
        // given: "null" 문자열 오리진 포함
        SecurityConfig config = newConfig("https://app.cotalk.com,null");

        // when & then
        assertThatThrownBy(config::validateCorsConfiguration)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("should_정상_when_명시적_오리진만_설정")
    void should_pass_when_onlyExplicitOrigins() {
        // given
        SecurityConfig config = newConfig("https://app.cotalk.com,https://admin.cotalk.com");

        // when & then
        assertThatCode(config::validateCorsConfiguration).doesNotThrowAnyException();
        assertThat(config).isNotNull();
    }
}
