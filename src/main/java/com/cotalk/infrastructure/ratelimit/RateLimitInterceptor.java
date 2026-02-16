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
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

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
    private final Counter allowedCounter;
    private final Counter blockedCounter;

    /**
     * RateLimitInterceptor 생성자.
     *
     * @param rateLimitProperties Rate Limit 설정
     * @param proxyManager Bucket4j Redis ProxyManager
     * @param jwtTokenProvider JWT 토큰 프로바이더
     * @param meterRegistry Micrometer 메트릭 레지스트리
     */
    public RateLimitInterceptor(RateLimitProperties rateLimitProperties,
                                 ProxyManager<byte[]> proxyManager,
                                 JwtTokenProvider jwtTokenProvider,
                                 MeterRegistry meterRegistry) {
        this.rateLimitProperties = rateLimitProperties;
        this.proxyManager = proxyManager;
        this.jwtTokenProvider = jwtTokenProvider;
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

        String path = request.getRequestURI();
        RateLimitProperties.EndpointRateLimit limit = findRateLimit(path);

        if (limit == null) {
            return true;
        }

        String keyString = generateKey(request, limit);
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
     * 요청 경로에 해당하는 Rate Limit 설정을 찾는다.
     *
     * @param path 요청 경로
     * @return Rate Limit 설정, 없으면 null
     */
    private RateLimitProperties.EndpointRateLimit findRateLimit(String path) {
        return rateLimitProperties.getEndpoints().stream()
                .filter(endpoint -> endpoint.getPath() != null && path.startsWith(endpoint.getPath()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Rate Limit 키를 생성한다.
     * 사용자별 제한이면 사용자 ID, 아니면 클라이언트 IP를 기준으로 키를 생성한다.
     *
     * @param request HTTP 요청
     * @param limit Rate Limit 설정
     * @return Rate Limit 키 문자열
     */
    private String generateKey(HttpServletRequest request, RateLimitProperties.EndpointRateLimit limit) {
        if (limit.isPerUser()) {
            // 사용자별 제한
            Optional<Long> userId = extractToken(request)
                    .filter(jwtTokenProvider::validateToken)
                    .map(jwtTokenProvider::getUserIdFromToken);

            if (userId.isPresent()) {
                return "rate-limit:user:" + userId.get() + ":" + request.getRequestURI();
            }
        }
        // IP별 제한
        String ip = getClientIpAddress(request);
        return "rate-limit:ip:" + ip + ":" + request.getRequestURI();
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
     * X-Forwarded-For 헤더가 있으면 첫 번째 IP를 반환한다.
     *
     * @param request HTTP 요청
     * @return 클라이언트 IP 주소
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
