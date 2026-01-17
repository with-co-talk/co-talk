package com.cotalk.infrastructure.health;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

/**
 * Redis 헬스 체크 인디케이터.
 * Redis 서버의 연결 상태와 응답 시간을 확인한다.
 *
 * <p>PING 명령을 실행하여 PONG 응답을 확인한다.</p>
 *
 * @author seunggu.lee
 */
@Component
@RequiredArgsConstructor
public class RedisHealthIndicator implements HealthIndicator {

    private final RedisConnectionFactory redisConnectionFactory;

    /**
     * Redis 헬스 상태를 확인한다.
     * PING 명령을 실행하여 연결 상태와 응답 시간을 측정한다.
     *
     * @return Redis 헬스 상태
     */
    @Override
    public Health health() {
        try {
            long startTime = System.currentTimeMillis();
            String pong = redisConnectionFactory.getConnection().ping();
            long responseTime = System.currentTimeMillis() - startTime;

            if (!"PONG".equals(pong)) {
                return Health.down()
                        .withDetail("redis", "Unexpected response")
                        .withDetail("response", pong)
                        .build();
            }

            return Health.up()
                    .withDetail("redis", "Connected")
                    .withDetail("responseTime", responseTime + "ms")
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("redis", "Connection failed")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
