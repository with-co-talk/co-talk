package com.cotalk.infrastructure.security;

import com.cotalk.domain.exception.UnauthorizedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SecurityContextHelper 단위 테스트.
 *
 * @author seunggu.lee
 */
@DisplayName("SecurityContextHelper")
class SecurityContextHelperTest {

    private SecurityContextHelper securityContextHelper;

    @BeforeEach
    void setUp() {
        securityContextHelper = new SecurityContextHelper();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("getCurrentUserId 메서드")
    class GetCurrentUserId {

        @Test
        @DisplayName("Long 타입 principal에서 사용자 ID를 반환한다")
        void should_returnUserId_when_principalIsLong() {
            // given
            Long userId = 1L;
            setAuthenticationWithLongPrincipal(userId);

            // when
            Long result = securityContextHelper.getCurrentUserId();

            // then
            assertThat(result).isEqualTo(userId);
        }

        @Test
        @DisplayName("String 타입 principal에서 사용자 ID를 반환한다")
        void should_returnUserId_when_principalIsString() {
            // given
            String userId = "123";
            setAuthenticationWithStringPrincipal(userId);

            // when
            Long result = securityContextHelper.getCurrentUserId();

            // then
            assertThat(result).isEqualTo(123L);
        }

        @Test
        @DisplayName("인증되지 않은 경우 UnauthorizedException을 던진다")
        void should_throwUnauthorizedException_when_notAuthenticated() {
            // when & then
            assertThatThrownBy(() -> securityContextHelper.getCurrentUserId())
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("인증이 필요합니다");
        }

        @Test
        @DisplayName("principal이 파싱 불가능한 문자열이면 UnauthorizedException을 던진다")
        void should_throwUnauthorizedException_when_principalIsInvalidString() {
            // given
            setAuthenticationWithStringPrincipal("not-a-number");

            // when & then
            assertThatThrownBy(() -> securityContextHelper.getCurrentUserId())
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("유효하지 않은 인증 정보");
        }

        @Test
        @DisplayName("principal이 지원하지 않는 타입이면 UnauthorizedException을 던진다")
        void should_throwUnauthorizedException_when_principalIsUnsupportedType() {
            // given
            setAuthenticationWithObjectPrincipal(new Object());

            // when & then
            assertThatThrownBy(() -> securityContextHelper.getCurrentUserId())
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("유효하지 않은 인증 정보");
        }
    }

    @Nested
    @DisplayName("isAuthenticated 메서드")
    class IsAuthenticated {

        @Test
        @DisplayName("인증된 사용자면 true를 반환한다")
        void should_returnTrue_when_authenticated() {
            // given
            setAuthenticationWithLongPrincipal(1L);

            // when
            boolean result = securityContextHelper.isAuthenticated();

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("인증되지 않은 경우 false를 반환한다")
        void should_returnFalse_when_notAuthenticated() {
            // when
            boolean result = securityContextHelper.isAuthenticated();

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("익명 사용자면 false를 반환한다")
        void should_returnFalse_when_anonymousUser() {
            // given
            setAuthenticationWithStringPrincipal("anonymousUser");

            // when
            boolean result = securityContextHelper.isAuthenticated();

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("다양한 인증 상황")
    class VariousAuthenticationScenarios {

        @Test
        @DisplayName("CustomUserPrincipal로 인증된 경우 ID를 반환한다")
        void should_returnUserId_when_customUserPrincipal() {
            // given
            Long userId = 42L;
            List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
            CustomUserPrincipal principal = new CustomUserPrincipal(userId, "USER", authorities);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // when
            Long result = securityContextHelper.getCurrentUserId();

            // then
            assertThat(result).isEqualTo(userId);
        }

        @Test
        @DisplayName("매우 큰 userId 문자열도 변환할 수 있다")
        void should_handleLargeUserId_when_stringPrincipal() {
            // given
            String largeId = String.valueOf(Long.MAX_VALUE);
            setAuthenticationWithStringPrincipal(largeId);

            // when
            Long result = securityContextHelper.getCurrentUserId();

            // then
            assertThat(result).isEqualTo(Long.MAX_VALUE);
        }
    }

    private void setAuthenticationWithLongPrincipal(Long userId) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userId, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void setAuthenticationWithStringPrincipal(String userId) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userId, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void setAuthenticationWithObjectPrincipal(Object principal) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
