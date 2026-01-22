package com.cotalk.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * JwtAuthenticationFilter 단위 테스트.
 *
 * @author seunggu.lee
 */
@DisplayName("JwtAuthenticationFilter")
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("유효한 토큰 처리")
    class ValidTokenHandling {

        @Test
        @DisplayName("유효한 JWT 토큰으로 인증을 설정한다")
        void should_setAuthentication_when_validToken() throws ServletException, IOException {
            // given
            String token = "valid-jwt-token";
            Long userId = 1L;
            String role = "USER";

            request.addHeader("Authorization", "Bearer " + token);

            given(jwtTokenProvider.validateToken(token)).willReturn(true);
            given(jwtTokenProvider.getUserIdFromToken(token)).willReturn(userId);
            given(jwtTokenProvider.getRoleFromToken(token)).willReturn(role);

            // when
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // then
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
            assertThat(SecurityContextHolder.getContext().getAuthentication().isAuthenticated()).isTrue();

            CustomUserPrincipal principal = (CustomUserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            assertThat(principal.getUserId()).isEqualTo(userId);
            assertThat(principal.getRole()).isEqualTo(role);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("ADMIN 역할로 인증을 설정한다")
        void should_setAuthentication_when_adminRole() throws ServletException, IOException {
            // given
            String token = "admin-jwt-token";
            Long userId = 2L;
            String role = "ADMIN";

            request.addHeader("Authorization", "Bearer " + token);

            given(jwtTokenProvider.validateToken(token)).willReturn(true);
            given(jwtTokenProvider.getUserIdFromToken(token)).willReturn(userId);
            given(jwtTokenProvider.getRoleFromToken(token)).willReturn(role);

            // when
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // then
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
            assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));

            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("토큰 없음 처리")
    class NoTokenHandling {

        @Test
        @DisplayName("Authorization 헤더가 없으면 인증을 설정하지 않는다")
        void should_notSetAuthentication_when_noAuthorizationHeader() throws ServletException, IOException {
            // when
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // then
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("빈 Authorization 헤더면 인증을 설정하지 않는다")
        void should_notSetAuthentication_when_emptyAuthorizationHeader() throws ServletException, IOException {
            // given
            request.addHeader("Authorization", "");

            // when
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // then
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Bearer 접두사가 없으면 인증을 설정하지 않는다")
        void should_notSetAuthentication_when_noBearerPrefix() throws ServletException, IOException {
            // given
            request.addHeader("Authorization", "Basic some-token");

            // when
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // then
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("유효하지 않은 토큰 처리")
    class InvalidTokenHandling {

        @Test
        @DisplayName("유효하지 않은 토큰이면 인증을 설정하지 않는다")
        void should_notSetAuthentication_when_invalidToken() throws ServletException, IOException {
            // given
            String invalidToken = "invalid-token";
            request.addHeader("Authorization", "Bearer " + invalidToken);

            given(jwtTokenProvider.validateToken(invalidToken)).willReturn(false);

            // when
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // then
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("만료된 토큰이면 인증을 설정하지 않는다")
        void should_notSetAuthentication_when_expiredToken() throws ServletException, IOException {
            // given
            String expiredToken = "expired-token";
            request.addHeader("Authorization", "Bearer " + expiredToken);

            given(jwtTokenProvider.validateToken(expiredToken)).willReturn(false);

            // when
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // then
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("필터 체인 동작")
    class FilterChainBehavior {

        @Test
        @DisplayName("인증 성공 후에도 필터 체인을 계속 진행한다")
        void should_continueFilterChain_when_authenticationSuccess() throws ServletException, IOException {
            // given
            String token = "valid-token";
            request.addHeader("Authorization", "Bearer " + token);

            given(jwtTokenProvider.validateToken(token)).willReturn(true);
            given(jwtTokenProvider.getUserIdFromToken(token)).willReturn(1L);
            given(jwtTokenProvider.getRoleFromToken(token)).willReturn("USER");

            // when
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // then
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("인증 실패 후에도 필터 체인을 계속 진행한다")
        void should_continueFilterChain_when_authenticationFailed() throws ServletException, IOException {
            // given
            String invalidToken = "invalid-token";
            request.addHeader("Authorization", "Bearer " + invalidToken);

            given(jwtTokenProvider.validateToken(invalidToken)).willReturn(false);

            // when
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // then
            verify(filterChain).doFilter(request, response);
        }
    }
}
