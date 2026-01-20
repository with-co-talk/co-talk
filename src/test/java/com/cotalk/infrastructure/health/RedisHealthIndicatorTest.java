package com.cotalk.infrastructure.health;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * RedisHealthIndicator 단위 테스트.
 *
 * @author seunggu.lee
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RedisHealthIndicator 단위 테스트")
class RedisHealthIndicatorTest {

    @Mock
    private RedisConnectionFactory redisConnectionFactory;

    @Mock
    private RedisConnection redisConnection;

    private RedisHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        healthIndicator = new RedisHealthIndicator(redisConnectionFactory);
    }

    @Test
    @DisplayName("Redis 연결 정상 - UP 상태 반환")
    void should_returnUp_when_redisIsHealthy() {
        // given
        given(redisConnectionFactory.getConnection()).willReturn(redisConnection);
        given(redisConnection.ping()).willReturn("PONG");

        // when
        Health health = healthIndicator.health();

        // then
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsKey("redis");
        assertThat(health.getDetails()).containsKey("responseTime");
        assertThat(health.getDetails().get("redis")).isEqualTo("Connected");
    }

    @Test
    @DisplayName("Redis 응답이 PONG이 아닌 경우 - DOWN 상태 반환")
    void should_returnDown_when_unexpectedResponse() {
        // given
        given(redisConnectionFactory.getConnection()).willReturn(redisConnection);
        given(redisConnection.ping()).willReturn("UNEXPECTED");

        // when
        Health health = healthIndicator.health();

        // then
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails().get("redis")).isEqualTo("Unexpected response");
        assertThat(health.getDetails().get("response")).isEqualTo("UNEXPECTED");
    }

    @Test
    @DisplayName("Redis 연결 실패 - DOWN 상태 반환")
    void should_returnDown_when_redisConnectionFails() {
        // given
        given(redisConnectionFactory.getConnection())
                .willThrow(new RuntimeException("Connection refused"));

        // when
        Health health = healthIndicator.health();

        // then
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails().get("redis")).isEqualTo("Connection failed");
        assertThat(health.getDetails()).containsKey("error");
    }
}
