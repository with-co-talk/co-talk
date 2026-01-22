package com.cotalk.infrastructure.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CustomUserPrincipal 단위 테스트.
 *
 * @author seunggu.lee
 */
@DisplayName("CustomUserPrincipal")
class CustomUserPrincipalTest {

    @Nested
    @DisplayName("생성자 및 기본 메서드")
    class ConstructorAndBasicMethods {

        @Test
        @DisplayName("생성자로 객체를 생성할 수 있다")
        void should_createInstance_when_validArguments() {
            // given
            Long userId = 1L;
            String role = "USER";
            List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));

            // when
            CustomUserPrincipal principal = new CustomUserPrincipal(userId, role, authorities);

            // then
            assertThat(principal).isNotNull();
            assertThat(principal.getUserId()).isEqualTo(userId);
            assertThat(principal.getRole()).isEqualTo(role);
        }

        @Test
        @DisplayName("getUserId는 올바른 사용자 ID를 반환한다")
        void should_returnUserId_when_getUserIdCalled() {
            // given
            Long userId = 123L;
            CustomUserPrincipal principal = new CustomUserPrincipal(userId, "USER", List.of());

            // when
            Long result = principal.getUserId();

            // then
            assertThat(result).isEqualTo(userId);
        }

        @Test
        @DisplayName("getRole은 올바른 역할을 반환한다")
        void should_returnRole_when_getRoleCalled() {
            // given
            String role = "ADMIN";
            CustomUserPrincipal principal = new CustomUserPrincipal(1L, role, List.of());

            // when
            String result = principal.getRole();

            // then
            assertThat(result).isEqualTo(role);
        }
    }

    @Nested
    @DisplayName("UserDetails 구현")
    class UserDetailsImplementation {

        @Test
        @DisplayName("getAuthorities는 권한 목록을 반환한다")
        void should_returnAuthorities_when_getAuthoritiesCalled() {
            // given
            List<SimpleGrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_USER"),
                    new SimpleGrantedAuthority("ROLE_ADMIN")
            );
            CustomUserPrincipal principal = new CustomUserPrincipal(1L, "USER", authorities);

            // when
            Collection<? extends GrantedAuthority> result = principal.getAuthorities();

            // then
            assertThat(result).hasSize(2);
            assertThat(result)
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
        }

        @Test
        @DisplayName("getPassword는 null을 반환한다 (JWT 인증)")
        void should_returnNull_when_getPasswordCalled() {
            // given
            CustomUserPrincipal principal = new CustomUserPrincipal(1L, "USER", List.of());

            // when
            String result = principal.getPassword();

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("getUsername은 userId를 문자열로 반환한다")
        void should_returnUserIdAsString_when_getUsernameCalled() {
            // given
            Long userId = 42L;
            CustomUserPrincipal principal = new CustomUserPrincipal(userId, "USER", List.of());

            // when
            String result = principal.getUsername();

            // then
            assertThat(result).isEqualTo("42");
        }
    }

    @Nested
    @DisplayName("다양한 역할 테스트")
    class DifferentRolesTest {

        @Test
        @DisplayName("USER 역할로 생성할 수 있다")
        void should_createWithUserRole() {
            // given
            List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));

            // when
            CustomUserPrincipal principal = new CustomUserPrincipal(1L, "USER", authorities);

            // then
            assertThat(principal.getRole()).isEqualTo("USER");
            assertThat(principal.getAuthorities())
                    .extracting(GrantedAuthority::getAuthority)
                    .contains("ROLE_USER");
        }

        @Test
        @DisplayName("ADMIN 역할로 생성할 수 있다")
        void should_createWithAdminRole() {
            // given
            List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));

            // when
            CustomUserPrincipal principal = new CustomUserPrincipal(1L, "ADMIN", authorities);

            // then
            assertThat(principal.getRole()).isEqualTo("ADMIN");
            assertThat(principal.getAuthorities())
                    .extracting(GrantedAuthority::getAuthority)
                    .contains("ROLE_ADMIN");
        }

        @Test
        @DisplayName("MODERATOR 역할로 생성할 수 있다")
        void should_createWithModeratorRole() {
            // given
            List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_MODERATOR"));

            // when
            CustomUserPrincipal principal = new CustomUserPrincipal(1L, "MODERATOR", authorities);

            // then
            assertThat(principal.getRole()).isEqualTo("MODERATOR");
        }
    }

    @Nested
    @DisplayName("엣지 케이스")
    class EdgeCases {

        @Test
        @DisplayName("빈 권한 목록으로 생성할 수 있다")
        void should_createWithEmptyAuthorities() {
            // when
            CustomUserPrincipal principal = new CustomUserPrincipal(1L, "USER", List.of());

            // then
            assertThat(principal.getAuthorities()).isEmpty();
        }

        @Test
        @DisplayName("매우 큰 userId로 생성할 수 있다")
        void should_createWithLargeUserId() {
            // given
            Long largeUserId = Long.MAX_VALUE;

            // when
            CustomUserPrincipal principal = new CustomUserPrincipal(largeUserId, "USER", List.of());

            // then
            assertThat(principal.getUserId()).isEqualTo(largeUserId);
            assertThat(principal.getUsername()).isEqualTo(String.valueOf(Long.MAX_VALUE));
        }

        @Test
        @DisplayName("여러 권한을 가진 principal을 생성할 수 있다")
        void should_createWithMultipleAuthorities() {
            // given
            List<SimpleGrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_USER"),
                    new SimpleGrantedAuthority("ROLE_ADMIN"),
                    new SimpleGrantedAuthority("PERMISSION_READ"),
                    new SimpleGrantedAuthority("PERMISSION_WRITE")
            );

            // when
            CustomUserPrincipal principal = new CustomUserPrincipal(1L, "ADMIN", authorities);

            // then
            assertThat(principal.getAuthorities()).hasSize(4);
        }
    }
}
