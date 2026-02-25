package com.cotalk.integration;

import com.cotalk.infrastructure.security.CustomUserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
// @Primary 제거 - 동일한 빈 이름("securityFilterChain")으로 프로덕션 빈을 오버라이드
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * 통합 테스트를 위한 Security 설정.
 * 모든 API 요청에 대해 인증을 비활성화하고 기본 사용자 Principal을 제공합니다.
 *
 * @author seunggu.lee
 */
@TestConfiguration
public class IntegrationTestSecurityConfig {

    /**
     * 테스트용 Security 필터 체인을 생성합니다.
     * 모든 요청을 허용하고, 요청 파라미터에서 userId를 읽어 인증 객체를 설정합니다.
     *
     * @param http HttpSecurity 설정 객체
     * @return SecurityFilterChain 인스턴스
     * @throws Exception 설정 오류 시
     */
    @Bean("securityFilterChain")
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .addFilterBefore(new TestAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * 테스트용 인증 필터.
     * 요청 파라미터 또는 Authorization 헤더에서 userId를 읽어 인증 객체를 설정합니다.
     */
    private static class TestAuthenticationFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                        FilterChain filterChain) throws ServletException, IOException {
            // 이미 인증된 경우 스킵
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                filterChain.doFilter(request, response);
                return;
            }

            // 요청 파라미터에서 userId 추출
            String userIdParam = request.getParameter("userId");
            Long userId = 1L; // 기본값

            if (userIdParam != null) {
                try {
                    userId = Long.parseLong(userIdParam);
                } catch (NumberFormatException ignored) {
                    // 기본값 사용
                }
            }

            // 테스트용 인증 객체 생성
            CustomUserPrincipal principal = new CustomUserPrincipal(userId, "USER", Collections.emptyList());
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(authentication);

            try {
                filterChain.doFilter(request, response);
            } finally {
                // 요청 처리 후 SecurityContext 정리
                SecurityContextHolder.clearContext();
            }
        }
    }
}
