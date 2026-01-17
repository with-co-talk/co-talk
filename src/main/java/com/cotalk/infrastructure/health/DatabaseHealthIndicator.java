package com.cotalk.infrastructure.health;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 데이터베이스 헬스 체크 인디케이터.
 * PostgreSQL 데이터베이스의 연결 상태와 응답 시간을 확인한다.
 *
 * <p>응답 시간이 1초를 초과하면 SLOW 상태로 DOWN을 반환한다.</p>
 *
 * @author seunggu.lee
 */
@Component
@RequiredArgsConstructor
public class DatabaseHealthIndicator implements HealthIndicator {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 데이터베이스 헬스 상태를 확인한다.
     * SELECT 1 쿼리를 실행하여 연결 상태와 응답 시간을 측정한다.
     *
     * @return 데이터베이스 헬스 상태
     */
    @Override
    public Health health() {
        try {
            long startTime = System.currentTimeMillis();
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            long responseTime = System.currentTimeMillis() - startTime;

            if (responseTime > 1000) {
                return Health.down()
                        .withDetail("database", "PostgreSQL")
                        .withDetail("responseTime", responseTime + "ms")
                        .withDetail("status", "SLOW")
                        .build();
            }

            return Health.up()
                    .withDetail("database", "PostgreSQL")
                    .withDetail("responseTime", responseTime + "ms")
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("database", "PostgreSQL")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
