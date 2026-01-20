package com.cotalk.infrastructure.health;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * DatabaseHealthIndicator 단위 테스트.
 *
 * @author seunggu.lee
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DatabaseHealthIndicator 단위 테스트")
class DatabaseHealthIndicatorTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private DatabaseHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        healthIndicator = new DatabaseHealthIndicator(jdbcTemplate);
    }

    @Test
    @DisplayName("데이터베이스 연결 정상 - UP 상태 반환")
    void should_returnUp_when_databaseIsHealthy() {
        // given
        given(jdbcTemplate.queryForObject("SELECT 1", Integer.class))
                .willReturn(1);

        // when
        Health health = healthIndicator.health();

        // then
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsKey("database");
        assertThat(health.getDetails()).containsKey("responseTime");
        assertThat(health.getDetails().get("database")).isEqualTo("PostgreSQL");
    }

    @Test
    @DisplayName("데이터베이스 연결 실패 - DOWN 상태 반환")
    void should_returnDown_when_databaseConnectionFails() {
        // given
        given(jdbcTemplate.queryForObject("SELECT 1", Integer.class))
                .willThrow(new RuntimeException("Connection refused"));

        // when
        Health health = healthIndicator.health();

        // then
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKey("error");
        assertThat(health.getDetails().get("database")).isEqualTo("PostgreSQL");
    }
}
