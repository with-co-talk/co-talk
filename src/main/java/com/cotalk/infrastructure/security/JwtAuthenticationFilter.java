package com.cotalk.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 인증 필터.
 * 모든 HTTP 요청에서 JWT 토큰을 추출하고 검증하여 인증을 수행한다.
 *
 * <p>동작 방식:
 * <ol>
 *   <li>Authorization 헤더에서 Bearer 토큰 추출</li>
 *   <li>JWT 토큰 유효성 검증</li>
 *   <li>유효한 경우 SecurityContext에 인증 정보 설정</li>
 * </ol>
 *
 * @author seunggu.lee
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * HTTP 요청에서 JWT 토큰을 검증하고 인증을 처리한다.
     *
     * @param request HTTP 요청 객체
     * @param response HTTP 응답 객체
     * @param filterChain 필터 체인
     * @throws ServletException 서블릿 예외 발생 시
     * @throws IOException 입출력 예외 발생 시
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            Long userId = jwtTokenProvider.getUserIdFromToken(token);
            String role = jwtTokenProvider.getRoleFromToken(token);

            List<SimpleGrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_" + role)
            );

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * HTTP 요청에서 JWT 토큰을 추출한다.
     * Authorization 헤더에서 "Bearer " 접두사를 제거하고 토큰 값을 반환한다.
     *
     * @param request HTTP 요청 객체
     * @return 추출된 JWT 토큰, 토큰이 없으면 null
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
