package com.cotalk.infrastructure.health;

import com.cotalk.infrastructure.lock.DistributedLockExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * 분산락 헬스 체크 인디케이터.
 *
 * <p>분산락이 NoOp으로 강등(RedissonClient 미가용)되었는지 운영 가시성으로 노출한다.
 * NoOp 강등은 동시성 보호가 사라진 상태이므로 {@code log.warn} 한 줄에만 의존하지 않고
 * 헬스 엔드포인트에서도 명확히 드러나게 한다.</p>
 *
 * <ul>
 *   <li>정상(분산락 활성): {@code UP}, detail {@code distributedLock=enabled}</li>
 *   <li>NoOp + fail-closed=false: {@code DOWN} 으로 보고하여(상세에 noOp/failClosed 표기)
 *       운영 모니터링이 강등을 감지하게 한다. 단, 앱 기동/요청 처리 자체는 계속된다(하위호환).</li>
 *   <li>NoOp + fail-closed=true: {@code DOWN}. 락 보호 작업은 예외로 거부되는 상태임을 알린다.</li>
 * </ul>
 *
 * @author seunggu.lee
 */
@Component
@RequiredArgsConstructor
public class DistributedLockHealthIndicator implements HealthIndicator {

    private final DistributedLockExecutor lockExecutor;

    /**
     * 분산락 헬스 상태를 확인한다.
     *
     * @return 분산락이 활성이면 UP, NoOp으로 강등되었으면 DOWN
     */
    @Override
    public Health health() {
        if (!lockExecutor.isNoOpMode()) {
            return Health.up()
                    .withDetail("distributedLock", "enabled")
                    .build();
        }

        return Health.down()
                .withDetail("distributedLock", "disabled (NoOp)")
                .withDetail("reason", "RedissonClient unavailable — concurrency locks are not applied")
                .withDetail("failClosed", lockExecutor.isFailClosed())
                .build();
    }
}
