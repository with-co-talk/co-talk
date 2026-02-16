package com.cotalk.infrastructure.ratelimit;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

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

    /** k6 부하 테스트 우회 토큰. X-K6-Token 헤더와 일치 시 rate limit 미적용. */
    private String k6BypassToken = "";

    /**
     * 엔드포인트별 Rate Limit 설정 리스트.
     */
    private List<EndpointRateLimit> endpoints = new ArrayList<>();

    /**
     * 엔드포인트별 Rate Limit 설정을 정의하는 내부 클래스.
     *
     * @author seunggu.lee
     */
    @Data
    public static class EndpointRateLimit {
        /**
         * 엔드포인트 경로 패턴 (예: "/api/v1/auth/login")
         */
        private String path;

        /**
         * 시간당 허용 요청 수 (0이면 미설정)
         */
        private long requestsPerHour = 0;

        /**
         * 분당 허용 요청 수 (0이면 미설정)
         */
        private long requestsPerMinute = 0;

        /**
         * 초당 허용 요청 수 (0이면 미설정)
         */
        private long requestsPerSecond = 0;

        /**
         * 사용자별 제한 여부 (true: 사용자별, false: IP별)
         */
        private boolean perUser = false;
    }
}
