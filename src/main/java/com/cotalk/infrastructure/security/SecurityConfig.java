package com.cotalk.infrastructure.security;

import com.cotalk.infrastructure.config.properties.AppProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security 설정 클래스.
 * JWT 기반 인증을 구성한다.
 *
 * <p>보안 설정:
 * <ul>
 *   <li>CSRF 보호 비활성화 (JWT 사용으로 불필요)</li>
 *   <li>세션 관리 STATELESS 설정</li>
 *   <li>인증 예외 처리 (401 응답)</li>
 *   <li>JWT 인증 필터 적용</li>
 * </ul>
 *
 * <p>허용된 공개 엔드포인트:
 * <ul>
 *   <li>/api/v1/auth/** - 인증 관련 API</li>
 *   <li>/ws/** - WebSocket 연결</li>
 *   <li>/swagger-ui/**, /v3/api-docs/** - API 문서</li>
 * </ul>
 *
 * @author seunggu.lee
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final String[] allowedOrigins;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            AppProperties appProperties) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.allowedOrigins = appProperties.cors().allowedOrigins().split(",");
    }

    /**
     * CORS 설정의 fail-open 위험을 시작 시점에 차단한다.
     *
     * <p>{@code allowCredentials=true} 상태에서 허용 오리진에 와일드카드("*")나
     * "null"이 포함되면 브라우저 보안 모델상 자격증명 포함 요청이 임의 오리진으로
     * 허용될 수 있다. 배포 설정 오타로 CORS가 조용히 약화되는 것을 막기 위해
     * 위반 시 즉시 예외를 던져 애플리케이션 기동을 실패시킨다.</p>
     *
     * @throws IllegalStateException 허용 오리진에 "*" 또는 "null"이 포함된 경우
     */
    @PostConstruct
    void validateCorsConfiguration() {
        for (String origin : allowedOrigins) {
            String normalized = origin == null ? null : origin.trim();
            if (normalized == null || normalized.equals("*") || normalized.equalsIgnoreCase("null")) {
                throw new IllegalStateException(
                        "CORS allowCredentials=true 상태에서 허용 오리진에 와일드카드(\"*\") 또는 \"null\"을 "
                                + "사용할 수 없습니다. app.cors.allowed-origins에 명시적 오리진을 지정하세요. "
                                + "현재 값: " + Arrays.toString(allowedOrigins));
            }
        }
    }

    /**
     * Spring Security 필터 체인을 구성한다.
     *
     * @param http HttpSecurity 설정 객체
     * @return 구성된 SecurityFilterChain
     * @throws Exception 보안 설정 중 오류 발생 시
     */
    @Bean
    // 테스트 프로파일(ratelimit-test 등)에서 IntegrationTestSecurityConfig만 사용하도록 비활성화 가능
    @ConditionalOnProperty(name = "app.security.default-chain.enabled", matchIfMissing = true)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 보안 헤더 설정
                .headers(headers -> headers
                        // X-Frame-Options: DENY - 클릭재킹 방지
                        .frameOptions(frame -> frame.deny())
                        // X-Content-Type-Options: nosniff - MIME 스니핑 방지
                        .contentTypeOptions(content -> {})
                        // X-XSS-Protection 비활성화 (CSP로 대체, 최신 브라우저에서 권장하지 않음)
                        .xssProtection(xss -> xss.disable())
                        // Content-Security-Policy - XSS 및 데이터 주입 공격 방지
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'; frame-ancestors 'none'; " +
                                        "script-src 'self'; style-src 'self' 'unsafe-inline'; " +
                                        "img-src 'self' data: https:; font-src 'self';"))
                        // HTTP Strict Transport Security (HSTS) - HTTPS 강제
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)  // 1년
                                .preload(true))
                        // Referrer-Policy - 리퍼러 정보 제한
                        .referrerPolicy(referrer -> referrer
                                .policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        // Permissions-Policy - 브라우저 기능 제한
                        .permissionsPolicyHeader(permissions -> permissions
                                .policy("geolocation=(), microphone=(), camera=()")))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"error\": \"인증이 필요합니다.\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(403);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"error\": \"접근 권한이 없습니다.\"}");
                        }))
                .authorizeHttpRequests(auth -> auth
                        // 공개 엔드포인트 (인증 불필요)
                        .requestMatchers(
                                "/api/v1/auth/signup",
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/oauth/**",
                                "/api/v1/auth/verify-email",
                                "/api/v1/auth/resend-verification",
                                "/api/v1/auth/find-email"
                        ).permitAll()
                        .requestMatchers(
                                "/api/v1/password/reset-request",
                                "/api/v1/password/reset-validate",
                                "/api/v1/password/reset",
                                "/api/v1/password/reset-request-code",
                                "/api/v1/password/verify-code",
                                "/api/v1/password/reset-with-code"
                        ).permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                        // 관리자 전용 엔드포인트
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        // /actuator/prometheus 포함 그 외 모든 actuator 엔드포인트는 ADMIN 전용 (메트릭 노출 차단)
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        // 그 외 모든 요청은 인증 필요
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        // 명시적으로 허용할 헤더 지정 (보안 강화)
        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With",
                "Cache-Control"
        ));
        // 클라이언트에 노출할 응답 헤더
        configuration.setExposedHeaders(List.of(
                "X-RateLimit-Limit",
                "X-RateLimit-Remaining",
                "Retry-After"
        ));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * 비밀번호 인코더를 생성한다.
     * BCrypt 해싱 알고리즘을 사용한다.
     *
     * @return BCryptPasswordEncoder 인스턴스
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
