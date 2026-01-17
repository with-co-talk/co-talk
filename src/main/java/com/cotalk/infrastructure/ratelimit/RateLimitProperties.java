package com.cotalk.infrastructure.ratelimit;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Rate Limit 설정 프로퍼티 클래스.
 * application.yml의 app.rate-limit 설정을 바인딩한다.
 *
 * <p>엔드포인트별로 다른 Rate Limit 설정을 적용할 수 있으며,
 * 사용자별 또는 IP별 제한을 설정할 수 있다.</p>
 *
 * @author seunggu.lee
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    /** Rate Limiting 활성화 여부 */
    private boolean enabled = true;

    /**
     * 엔드포인트별 Rate Limit 설정 맵.
     * 키: 엔드포인트 패턴 (예: "/api/v1/auth/login")
     * 값: 해당 엔드포인트의 Rate Limit 설정
     */
    private Map<String, EndpointRateLimit> endpoints = new HashMap<>();

    /**
     * 엔드포인트별 Rate Limit 설정을 정의하는 내부 클래스.
     *
     * @author seunggu.lee
     */
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
