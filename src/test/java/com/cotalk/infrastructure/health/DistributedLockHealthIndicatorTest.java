package com.cotalk.infrastructure.health;

import com.cotalk.infrastructure.config.properties.AppProperties;
import com.cotalk.infrastructure.lock.DistributedLockExecutor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RedissonClient;
import org.springframework.boot.actuate.health.Health;
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
        assertThat(health.getDetails().get("distributedLock")).isEqualTo("enabled");
    }

    @Test
    @DisplayName("분산락 NoOp 강등(RedissonClient 없음) - DOWN 상태와 failClosed 상세 반환")
    void should_returnDown_when_noOpMode() {
        // given
        DistributedLockExecutor executor =
                new DistributedLockExecutor(null, appProperties(false));
        DistributedLockHealthIndicator indicator = new DistributedLockHealthIndicator(executor);

        // when
        Health health = indicator.health();

        // then
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails().get("distributedLock")).isEqualTo("disabled (NoOp)");
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
        assertThat(health.getDetails().get("failClosed")).isEqualTo(true);
    }
}
