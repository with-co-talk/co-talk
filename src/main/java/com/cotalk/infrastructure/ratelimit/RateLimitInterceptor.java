package com.cotalk.infrastructure.ratelimit;

import com.cotalk.domain.exception.RateLimitExceededException;
import com.cotalk.infrastructure.security.JwtTokenProvider;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.Map;
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
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.rate-limit.enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitProperties rateLimitProperties;
    private final ProxyManager<byte[]> proxyManager;
    private final JwtTokenProvider jwtTokenProvider;

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
            // Rate Limit이 설정되지 않은 엔드포인트는 통과
            return true;
        }

        String keyString = generateKey(request, limit);
        byte[] key = keyString.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Bucket bucket = resolveBucket(key, limit);

        if (!bucket.tryConsume(1)) {
            // Rate Limit 초과
            long availableTokens = bucket.getAvailableTokens();
            long retryAfter = calculateRetryAfter(limit);
            
            long limitValue = limit.getRequestsPerSecond() > 0 
                    ? limit.getRequestsPerSecond() 
                    : (limit.getRequestsPerMinute() > 0 
                            ? limit.getRequestsPerMinute() 
                            : limit.getRequestsPerHour());
            
            response.setHeader("X-RateLimit-Limit", String.valueOf(limitValue));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(availableTokens));
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            response.setStatus(429);

            log.warn("Rate limit exceeded: path={}, key={}, retryAfter={}s", path, key, retryAfter);
            throw RateLimitExceededException.tooManyRequests(retryAfter);
        }

        // Rate Limit 헤더 추가
        long limitValue = limit.getRequestsPerSecond() > 0 
                ? limit.getRequestsPerSecond() 
                : (limit.getRequestsPerMinute() > 0 
                        ? limit.getRequestsPerMinute() 
                        : limit.getRequestsPerHour());
        
        response.setHeader("X-RateLimit-Limit", String.valueOf(limitValue));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(bucket.getAvailableTokens()));

        return true;
    }

    /**
     * 요청 경로에 해당하는 Rate Limit 설정을 찾는다.
     *
     * @param path 요청 경로
     * @return Rate Limit 설정, 없으면 null
     */
    private RateLimitProperties.EndpointRateLimit findRateLimit(String path) {
        return rateLimitProperties.getEndpoints().entrySet().stream()
                .filter(entry -> path.startsWith(entry.getKey()))
                .findFirst()
                .map(Map.Entry::getValue)
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
            String token = extractToken(request);
            if (token != null && jwtTokenProvider.validateToken(token)) {
                Long userId = jwtTokenProvider.getUserIdFromToken(token);
                return "rate-limit:user:" + userId + ":" + request.getRequestURI();
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
            long capacity = limit.getRequestsPerMinute() > 0 
                    ? limit.getRequestsPerMinute() 
                    : (limit.getRequestsPerSecond() > 0 
                            ? limit.getRequestsPerSecond() 
                            : limit.getRequestsPerHour());
            
            Duration refillDuration = limit.getRequestsPerMinute() > 0 
                    ? Duration.ofMinutes(1) 
                    : (limit.getRequestsPerSecond() > 0 
                            ? Duration.ofSeconds(1) 
                            : Duration.ofHours(1));
            
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
     * @return JWT 토큰, 없으면 null
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
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
