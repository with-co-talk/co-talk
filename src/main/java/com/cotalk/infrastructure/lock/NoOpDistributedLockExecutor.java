package com.cotalk.infrastructure.lock;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * No-op 분산락 실행기.
 *
 * <p>RedissonClient가 없는 환경(테스트 환경 등)에서 사용되는 대체 구현이다.
 * 락을 사용하지 않고 즉시 작업을 실행한다.
 *
 * <p><strong>주의:</strong> 이 구현은 실제 락을 사용하지 않으므로
 * 분산 환경에서 동시성 제어가 필요한 경우 사용하면 안 된다.
 *
 * @author seunggu.lee
 * @see DistributedLockExecutor
 */
@Slf4j
@Component
@ConditionalOnMissingBean(RedissonClient.class)
public class NoOpDistributedLockExecutor extends DistributedLockExecutor {

    /**
     * NoOpDistributedLockExecutor 생성자.
     */
    public NoOpDistributedLockExecutor() {
        super(null);
        log.warn("NoOpDistributedLockExecutor가 활성화되었습니다. 분산락이 비활성화됩니다.");
    }

    /**
     * {@inheritDoc}
     *
     * <p>락 없이 즉시 작업을 실행한다.
     */
    @Override
    public <T> T executeWithLock(String lockKey, long waitTime, long leaseTime,
                                  TimeUnit timeUnit, Supplier<T> supplier) {
        log.trace("NoOp lock execution: {}", lockKey);
        return supplier.get();
    }

    /**
     * {@inheritDoc}
     *
     * <p>락 없이 즉시 작업을 실행한다.
     */
    @Override
    public void executeWithLock(String lockKey, long waitTime, long leaseTime,
                                TimeUnit timeUnit, Runnable runnable) {
        log.trace("NoOp lock execution: {}", lockKey);
        runnable.run();
    }

    /**
     * {@inheritDoc}
     *
     * <p>락 없이 즉시 작업을 실행한다.
     */
    @Override
    public <T> T executeWithLock(String lockKey, Supplier<T> supplier) {
        log.trace("NoOp lock execution: {}", lockKey);
        return supplier.get();
    }

    /**
     * {@inheritDoc}
     *
     * <p>락 없이 즉시 작업을 실행한다.
     */
    @Override
    public void executeWithLock(String lockKey, Runnable runnable) {
        log.trace("NoOp lock execution: {}", lockKey);
        runnable.run();
    }
}
