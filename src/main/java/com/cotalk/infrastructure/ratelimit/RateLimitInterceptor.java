package com.cotalk.infrastructure.ratelimit;

import com.cotalk.domain.exception.RateLimitExceededException;
import com.cotalk.infrastructure.security.JwtTokenProvider;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Rate Limit 인터셉터.
 * HTTP 요청을 가로채서 Rate Limit을 검사하고, 초과 시 예외를 발생시킨다.
 *
 * <p>Bucket4j를 사용하여 토큰 버킷 알고리즘으로 Rate Limiting을 수행한다.
 * 사용자별 또는 IP별로 요청 제한을 적용할 수 있다.</p>
 *
 * <p>Rate Limit 초과 시 429 Too Many Requests 응답과 함께
 * X-RateLimit-Limit, X-RateLimit-Remaining, Retry-After 헤더를 포함한다.</p>
 *
 * <p>이 컴포넌트는 {@code app.rate-limit.enabled=true}일 때만 활성화된다.</p>
 *
 * @author seunggu.lee
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.rate-limit.enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitProperties rateLimitProperties;
    private final ProxyManager<byte[]> proxyManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final Environment environment;
    private final Counter allowedCounter;
    private final Counter blockedCounter;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * RateLimitInterceptor 생성자.
     *
     * @param rateLimitProperties Rate Limit 설정
     * @param proxyManager Bucket4j Redis ProxyManager
     * @param jwtTokenProvider JWT 토큰 프로바이더
     * @param environment 활성 프로파일 확인용 스프링 환경
     * @param meterRegistry Micrometer 메트릭 레지스트리
     */
    public RateLimitInterceptor(RateLimitProperties rateLimitProperties,
                                 ProxyManager<byte[]> proxyManager,
                                 JwtTokenProvider jwtTokenProvider,
                                 Environment environment,
                                 MeterRegistry meterRegistry) {
        this.rateLimitProperties = rateLimitProperties;
        this.proxyManager = proxyManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.environment = environment;
        this.allowedCounter = Counter.builder("cotalk.ratelimit.requests")
                .tag("result", "allowed")
                .description("Rate limit 허용된 요청 수")
                .register(meterRegistry);
        this.blockedCounter = Counter.builder("cotalk.ratelimit.requests")
                .tag("result", "blocked")
                .description("Rate limit 차단된 요청 수")
                .register(meterRegistry);
    }

    /**
     * 요청 처리 전에 Rate Limit을 검사한다.
     *
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @param handler 요청 핸들러
     * @return Rate Limit 이내이면 true, 초과 시 RateLimitExceededException 발생
     * @throws RateLimitExceededException Rate Limit 초과 시
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!rateLimitProperties.isEnabled()) {
            return true;
        }

        // k6 부하 테스트 우회: X-K6-Token 헤더가 설정된 토큰과 일치하면 rate limit 미적용.
        // prod 프로파일에서는 설정과 무관하게 헤더를 무시하여 fail-closed 보장 (운영 우회 불가).
        if (!isProdProfile()) {
            String bypassToken = rateLimitProperties.getK6BypassToken();
            if (bypassToken != null && !bypassToken.isBlank()) {
                String requestToken = request.getHeader("X-K6-Token");
                if (bypassToken.equals(requestToken)) {
                    return true;
                }
            }
        }

        String path = resolveMatchPath(request);
        RateLimitProperties.EndpointRateLimit limit = findRateLimit(path);

        if (limit == null) {
            return true;
        }

        String keyString = generateKey(request, limit, path);
        byte[] key = keyString.getBytes(StandardCharsets.UTF_8);
        Bucket bucket = resolveBucket(key, limit);
        boolean consumed = bucket.tryConsume(1);

        long limitValue = resolveLimitValue(limit);

        if (!consumed) {
            blockedCounter.increment();
            long retryAfter = calculateRetryAfter(limit);

            response.setHeader("X-RateLimit-Limit", String.valueOf(limitValue));
            response.setHeader("X-RateLimit-Remaining", "0");
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            response.setStatus(429);

            log.warn("Rate limit exceeded: path={}, key={}, retryAfter={}s", path, keyString, retryAfter);
            throw RateLimitExceededException.tooManyRequests(retryAfter);
        }

        allowedCounter.increment();
        response.setHeader("X-RateLimit-Limit", String.valueOf(limitValue));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(bucket.getAvailableTokens()));

        return true;
    }

    /**
     * Rate Limit 설정에서 표시용 제한 값을 반환한다.
     *
     * @param limit Rate Limit 설정
     * @return 제한 값
     */
    private long resolveLimitValue(RateLimitProperties.EndpointRateLimit limit) {
        if (limit.getRequestsPerSecond() > 0) {
            return limit.getRequestsPerSecond();
        } else if (limit.getRequestsPerMinute() > 0) {
            return limit.getRequestsPerMinute();
        }
        return limit.getRequestsPerHour();
    }

    /**
     * 현재 prod 프로파일이 활성화되어 있는지 확인한다.
     *
     * @return prod 프로파일이 활성화되어 있으면 true
     */
    private boolean isProdProfile() {
        return environment != null && environment.acceptsProfiles(Profiles.of("prod"));
    }

    /**
     * Rate Limit 매칭에 사용할 정규화된 경로를 결정한다.
     *
     * <p>Spring MVC가 핸들러 매핑 시 결정한 best-matching 패턴
     * ({@link HandlerMapping#BEST_MATCHING_PATTERN_ATTRIBUTE})이 있으면 이를 사용한다.
     * 이 패턴은 트레일링 슬래시/매트릭스 변수 등 우회 변형을 흡수한 정규 패턴이므로
     * prefix 매칭으로 인한 우회를 차단한다. 속성이 없으면(필터/테스트 등)
     * 요청 URI를 정규화({@link #normalizeUri(String)})하여 대체한다.</p>
     *
     * @param request HTTP 요청
     * @return Rate Limit 매칭에 사용할 경로
     */
    private String resolveMatchPath(HttpServletRequest request) {
        Object bestMatch = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (bestMatch instanceof String pattern && !pattern.isBlank()) {
            return pattern;
        }
        return normalizeUri(request.getRequestURI());
    }

    /**
     * 요청 URI를 매칭 가능한 형태로 정규화한다.
     *
     * <p>매트릭스 변수(";"), 트레일링 슬래시를 제거하여
     * {@code /api/v1/auth/login/}, {@code /api/v1/auth/login;jsessionid=...} 같은
     * 변형이 limiter를 우회하지 못하도록 한다.</p>
     *
     * @param uri 원본 요청 URI
     * @return 정규화된 경로
     */
    private String normalizeUri(String uri) {
        if (uri == null) {
            return null;
        }
        String normalized = uri;
        int semicolon = normalized.indexOf(';');
        if (semicolon >= 0) {
            normalized = normalized.substring(0, semicolon);
        }
        while (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    /**
     * 요청 경로에 해당하는 Rate Limit 설정을 찾는다.
     *
     * <p>설정된 엔드포인트 경로를 Ant 패턴으로 보고 {@link AntPathMatcher}로 매칭한다.
     * 경로가 정확히 일치하거나, 엔드포인트 하위 경로({@code endpoint + "/**"})에
     * 해당하면 매칭으로 간주한다. 트레일링 슬래시 등으로 정규화된 경로를 매칭하므로
     * 기존 prefix(startsWith) 방식의 브루트포스 우회를 차단하면서도
     * 하위 경로(예: {@code /api/v1/chat/messages/123}) 커버리지는 유지한다.</p>
     *
     * @param path 매칭 대상 경로(정규화된 URI 또는 resolved 패턴)
     * @return Rate Limit 설정, 없으면 null
     */
    private RateLimitProperties.EndpointRateLimit findRateLimit(String path) {
        if (path == null) {
            return null;
        }
        return rateLimitProperties.getEndpoints().stream()
                .filter(endpoint -> endpoint.getPath() != null && matchesEndpoint(endpoint.getPath(), path))
                .findFirst()
                .orElse(null);
    }

    /**
     * 엔드포인트 경로와 요청 경로가 매칭되는지 판별한다.
     *
     * @param endpointPath 설정된 엔드포인트 경로(Ant 패턴)
     * @param path 매칭 대상 경로
     * @return 매칭되면 true
     */
    private boolean matchesEndpoint(String endpointPath, String path) {
        if (pathMatcher.match(endpointPath, path)) {
            return true;
        }
        String normalized = endpointPath.endsWith("/")
                ? endpointPath.substring(0, endpointPath.length() - 1)
                : endpointPath;
        return pathMatcher.match(normalized + "/**", path);
    }

    /**
     * Rate Limit 키를 생성한다.
     * 사용자별 제한이면 사용자 ID, 아니면 클라이언트 IP를 기준으로 키를 생성한다.
     *
     * <p>버킷 키의 엔드포인트 식별자는 raw 요청 URI가 아니라 매칭 단계에서 사용한
     * 정규화/해석된 엔드포인트 식별자({@code endpointId})를 사용한다. 이로써
     * {@code /api/v1/auth/login}, {@code /api/v1/auth/login/},
     * {@code /api/v1/auth/login;jsessionid=...} 같은 URI 변형이 동일 클라이언트에 대해
     * 서로 다른 버킷을 만들어 throttle을 우회(브루트포스 증폭)하는 것을 차단한다.
     * 매칭된 엔드포인트 설정 경로({@link RateLimitProperties.EndpointRateLimit#getPath()})를
     * 우선 사용하고, 없으면 해석된 매칭 경로로 대체한다.</p>
     *
     * @param request HTTP 요청
     * @param limit Rate Limit 설정
     * @param matchedPath 매칭 단계에서 해석된 경로(폴백용)
     * @return Rate Limit 키 문자열
     */
    private String generateKey(HttpServletRequest request, RateLimitProperties.EndpointRateLimit limit, String matchedPath) {
        String endpointId = (limit.getPath() != null && !limit.getPath().isBlank())
                ? limit.getPath()
                : matchedPath;
        if (limit.isPerUser()) {
            // 사용자별 제한
            Optional<Long> userId = extractToken(request)
                    .filter(jwtTokenProvider::validateToken)
                    .map(jwtTokenProvider::getUserIdFromToken);

            if (userId.isPresent()) {
                return "rate-limit:user:" + userId.get() + ":" + endpointId;
            }
        }
        // IP별 제한
        String ip = getClientIpAddress(request);
        return "rate-limit:ip:" + ip + ":" + endpointId;
    }

    /**
     * Rate Limit 버킷을 조회하거나 생성한다.
     *
     * @param key Rate Limit 키
     * @param limit Rate Limit 설정
     * @return Bucket4j 버킷
     */
    private Bucket resolveBucket(byte[] key, RateLimitProperties.EndpointRateLimit limit) {
        Supplier<BucketConfiguration> configSupplier = () -> {
            // 분당 제한을 기본으로 사용 (가장 일반적)
            long capacity;
            if (limit.getRequestsPerMinute() > 0) {
                capacity = limit.getRequestsPerMinute();
            } else if (limit.getRequestsPerSecond() > 0) {
                capacity = limit.getRequestsPerSecond();
            } else {
                capacity = limit.getRequestsPerHour();
            }
            
            Duration refillDuration;
            if (limit.getRequestsPerMinute() > 0) {
                refillDuration = Duration.ofMinutes(1);
            } else if (limit.getRequestsPerSecond() > 0) {
                refillDuration = Duration.ofSeconds(1);
            } else {
                refillDuration = Duration.ofHours(1);
            }
            
            return BucketConfiguration.builder()
                    .addLimit(bandwidth -> bandwidth
                            .capacity(capacity)
                            .refillIntervally(capacity, refillDuration))
                    .build();
        };

        return proxyManager.builder()
                .build(key, configSupplier);
    }

    /**
     * Rate Limit 초과 시 재시도 대기 시간을 계산한다.
     *
     * @param limit Rate Limit 설정
     * @return 재시도 대기 시간(초)
     */
    private long calculateRetryAfter(RateLimitProperties.EndpointRateLimit limit) {
        // 가장 제한적인 제한 기준으로 재시도 시간 계산
        if (limit.getRequestsPerSecond() > 0) {
            return 1; // 초당 제한이면 1초 후 재시도
        } else if (limit.getRequestsPerMinute() > 0) {
            return 60 / limit.getRequestsPerMinute();
        } else if (limit.getRequestsPerHour() > 0) {
            return 3600 / limit.getRequestsPerHour();
        }
        return 60; // 기본값
    }

    /**
     * HTTP 요청에서 JWT 토큰을 추출한다.
     *
     * @param request HTTP 요청
     * @return JWT 토큰을 담은 Optional, 없으면 빈 Optional
     */
    private Optional<String> extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return Optional.of(bearerToken.substring(7));
        }
        return Optional.empty();
    }

    /**
     * 클라이언트 IP 주소를 조회한다.
     * Nginx가 설정하는 X-Real-IP 헤더를 우선 사용한다 (스푸핑 불가).
     * X-Real-IP가 없는 경우 remoteAddr을 반환한다.
     * X-Forwarded-For는 클라이언트가 임의로 조작할 수 있으므로 사용하지 않는다.
     *
     * @param request HTTP 요청
     * @return 클라이언트 IP 주소
     */
    private String getClientIpAddress(HttpServletRequest request) {
        // Nginx가 설정하는 X-Real-IP를 우선 사용 (스푸핑 불가)
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }

        // X-Real-IP 없는 경우 remoteAddr 사용 (X-Forwarded-For는 클라이언트가 조작 가능)
        return request.getRemoteAddr();
    }
}
