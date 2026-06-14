package com.cotalk.infrastructure.health;

import com.cotalk.infrastructure.config.properties.AppProperties;
import com.cotalk.infrastructure.lock.DistributedLockExecutor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RedissonClient;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.SimpleStatusAggregator;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * DistributedLockHealthIndicator 단위 테스트.
 *
 * @author seunggu.lee
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DistributedLockHealthIndicator 단위 테스트")
class DistributedLockHealthIndicatorTest {

    private static AppProperties appProperties(boolean failClosed) {
        return new AppProperties(null, null, null, null, null, null, null, null,
                new AppProperties.Lock(failClosed));
    }

    @Test
    @DisplayName("분산락 활성(RedissonClient 존재) - UP 상태 반환")
    void should_returnUp_when_lockEnabled() {
        // given
        RedissonClient redissonClient = mock(RedissonClient.class);
        DistributedLockExecutor executor =
                new DistributedLockExecutor(redissonClient, appProperties(false));
        DistributedLockHealthIndicator indicator = new DistributedLockHealthIndicator(executor);

        // when
        Health health = indicator.health();

        // then
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails().get("distributedLock")).isEqualTo("활성");
    }

    @Test
    @DisplayName("분산락 NoOp 강등 + fail-closed=false - DEGRADED 상태(top-level UP 유지) 반환")
    void should_returnDegraded_when_noOpAndNotFailClosed() {
        // given
        DistributedLockExecutor executor =
                new DistributedLockExecutor(null, appProperties(false));
        DistributedLockHealthIndicator indicator = new DistributedLockHealthIndicator(executor);

        // when
        Health health = indicator.health();

        // then: DOWN이 아니라 DEGRADED여야 top-level /actuator/health가 UP을 유지한다
        assertThat(health.getStatus()).isEqualTo(DistributedLockHealthIndicator.DEGRADED);
        assertThat(health.getStatus().getCode()).isEqualTo("DEGRADED");
        assertThat(health.getStatus()).isNotEqualTo(Status.DOWN);
        assertThat(health.getDetails().get("distributedLock")).isEqualTo("비활성 (NoOp)");
        assertThat(health.getDetails().get("failClosed")).isEqualTo(false);
    }

    @Test
    @DisplayName("분산락 비활성 + fail-closed=true - DOWN 상태와 failClosed=true 반환")
    void should_returnDown_when_noOpAndFailClosed() {
        // given
        DistributedLockExecutor executor =
                new DistributedLockExecutor(null, appProperties(true));
        DistributedLockHealthIndicator indicator = new DistributedLockHealthIndicator(executor);

        // when
        Health health = indicator.health();

        // then
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails().get("distributedLock")).isEqualTo("비활성 (NoOp)");
        assertThat(health.getDetails().get("failClosed")).isEqualTo(true);
    }

    @Test
    @DisplayName("DEGRADED는 Spring 기본 집계에서 top-level을 DOWN으로 떨어뜨리지 않는다 (로드밸런서 오판 방지)")
    void should_keepTopLevelUp_when_degradedAggregatedWithUp() {
        // given: Spring 기본 집계기로 UP + DEGRADED 를 집계
        SimpleStatusAggregator aggregator = new SimpleStatusAggregator();

        // when
        Status aggregated = aggregator.getAggregateStatus(
                Status.UP, DistributedLockHealthIndicator.DEGRADED);

        // then: 커스텀 DEGRADED는 UP보다 낮은 심각도로 정렬되어 top-level은 UP을 유지
        assertThat(aggregated).isEqualTo(Status.UP);
    }

    @Test
    @DisplayName("DOWN(fail-closed=true)은 집계에서 top-level을 DOWN으로 만든다")
    void should_makeTopLevelDown_when_downAggregatedWithUp() {
        // given
        SimpleStatusAggregator aggregator = new SimpleStatusAggregator();

        // when
        Status aggregated = aggregator.getAggregateStatus(Status.UP, Status.DOWN);

        // then
        assertThat(aggregated).isEqualTo(Status.DOWN);
    }
}
