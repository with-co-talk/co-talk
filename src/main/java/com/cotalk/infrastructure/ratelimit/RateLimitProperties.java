package com.cotalk.infrastructure.ratelimit;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Rate Limit 설정 프로퍼티
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;

    /**
     * 엔드포인트별 Rate Limit 설정
     * 키: 엔드포인트 패턴 (예: "/api/v1/auth/login")
     * 값: Rate Limit 설정
     */
    private Map<String, EndpointRateLimit> endpoints = new HashMap<>();

    @Data
    public static class EndpointRateLimit {
        /**
         * 시간당 허용 요청 수
         */
        private long requestsPerHour = 100;

        /**
         * 분당 허용 요청 수
         */
        private long requestsPerMinute = 20;

        /**
         * 초당 허용 요청 수
         */
        private long requestsPerSecond = 5;

        /**
         * 사용자별 제한 여부 (true: 사용자별, false: IP별)
         */
        private boolean perUser = true;
    }
}
