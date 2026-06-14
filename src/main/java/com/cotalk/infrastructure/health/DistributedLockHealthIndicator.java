package com.cotalk.infrastructure.health;

import com.cotalk.infrastructure.lock.DistributedLockExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Component;

/**
 * 분산락 헬스 체크 인디케이터.
 *
 * <p>분산락이 NoOp으로 강등(RedissonClient 미가용)되었는지 운영 가시성으로 노출한다.
 * NoOp 강등은 동시성 보호가 사라진 상태이므로 {@code log.warn} 한 줄에만 의존하지 않고
 * 헬스 엔드포인트에서도 명확히 드러나게 한다.</p>
 *
 * <p>상태 매핑은 fail-closed 정책에 따라 분기한다. fail-closed=false(기본, 하위호환)는
 * "앱이 의도대로 정상 기동·요청 처리 중"인 상태이므로 top-level {@code /actuator/health}를
 * {@code DOWN}으로 떨어뜨리면 로드밸런서·k8s가 정상 인스턴스를 죽었다고 오판할 수 있다.
 * 따라서 이 경우는 강등을 알리는 커스텀 상태 {@code DEGRADED}로 보고한다. Spring 기본
 * {@code SimpleStatusAggregator}는 알 수 없는 커스텀 상태를 가장 낮은 심각도로 정렬하므로
 * top-level 집계는 {@code UP}으로 유지되고(HTTP 200), 상세에서 강등 사실만 드러난다.
 * 반면 fail-closed=true는 락 보호 작업이 실제로 예외 거부되는 치명 상태이므로 {@code DOWN}으로
 * 보고한다.</p>
 *
 * <ul>
 *   <li>정상(분산락 활성): {@code UP}, detail {@code distributedLock=활성}</li>
 *   <li>NoOp + fail-closed=false: 커스텀 {@code DEGRADED}. 동시성 보호는 사라졌지만 요청 처리는
 *       정상 진행 중(하위호환). top-level 집계는 UP을 유지하고 상세로 강등을 알린다.</li>
 *   <li>NoOp + fail-closed=true: {@code DOWN}. 락 보호 작업은 예외로 거부되는 상태임을 알린다.</li>
 * </ul>
 *
 * @author seunggu.lee
 */
@Component
@RequiredArgsConstructor
public class DistributedLockHealthIndicator implements HealthIndicator {

    /**
     * NoOp + fail-closed=false 상태를 나타내는 커스텀 헬스 상태.
     * top-level 집계를 DOWN으로 떨어뜨리지 않으면서 강등 사실을 드러내기 위함이다.
     */
    static final Status DEGRADED = new Status("DEGRADED",
            "분산락 비활성(NoOp): 동시성 보호 없이 요청 처리 중");

    private final DistributedLockExecutor lockExecutor;

    /**
     * 분산락 헬스 상태를 확인한다.
     *
     * @return 분산락 활성이면 {@code UP}, NoOp+fail-closed=false면 {@code DEGRADED},
     *         NoOp+fail-closed=true면 {@code DOWN}
     */
    @Override
    public Health health() {
        if (!lockExecutor.isNoOpMode()) {
            return Health.up()
                    .withDetail("distributedLock", "활성")
                    .build();
        }

        if (lockExecutor.isFailClosed()) {
            return Health.down()
                    .withDetail("distributedLock", "비활성 (NoOp)")
                    .withDetail("reason", "RedissonClient 미가용 - 락 보호 작업이 예외로 거부됩니다 (fail-closed)")
                    .withDetail("failClosed", true)
                    .build();
        }

        return Health.status(DEGRADED)
                .withDetail("distributedLock", "비활성 (NoOp)")
                .withDetail("reason", "RedissonClient 미가용 - 동시성 락이 적용되지 않습니다 (요청 처리는 계속됨)")
                .withDetail("failClosed", false)
                .build();
    }
}
