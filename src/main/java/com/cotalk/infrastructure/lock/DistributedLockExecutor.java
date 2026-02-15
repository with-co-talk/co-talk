package com.cotalk.infrastructure.lock;

import com.cotalk.domain.port.outbound.DistributedLockPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Redisson 기반 분산락 실행기.
 *
 * <p>분산 환경에서 동시성 제어가 필요한 작업을 안전하게 실행한다.
 * Redis를 사용하여 여러 서버 인스턴스 간에 락을 공유한다.
 *
 * <p>사용 예시:
 * <pre>{@code
 * Long result = lockExecutor.executeWithLock(
 *     "friend-request:" + requesterId + ":" + receiverId,
 *     3, 10, TimeUnit.SECONDS,
 *     () -> friendRequestService.send(requesterId, receiverId)
 * );
 * }</pre>
 *
 * <p>RedissonClient 빈이 없는 경우(테스트 환경 등) 이 빈은 생성되지 않으며,
 * {@link NoOpDistributedLockExecutor}가 대신 사용된다.
 *
 * @author seunggu.lee
 * @see NoOpDistributedLockExecutor
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(RedissonClient.class)
public class DistributedLockExecutor implements DistributedLockPort {

    private static final String LOCK_PREFIX = "lock:";

    private final RedissonClient redissonClient;

    /**
     * 분산락을 획득한 후 작업을 실행한다.
     *
     * @param lockKey   락 키 (자동으로 "lock:" 접두사 추가)
     * @param waitTime  락 획득 대기 시간
     * @param leaseTime 락 유지 시간 (작업 완료 전 자동 해제 방지)
     * @param timeUnit  시간 단위
     * @param supplier  실행할 작업
     * @param <T>       반환 타입
     * @return 작업 실행 결과
     * @throws DistributedLockException 락 획득 실패 시
     */
    @Override
    public <T> T executeWithLock(String lockKey, long waitTime, long leaseTime,
                                  TimeUnit timeUnit, Supplier<T> supplier) {
        RLock lock = redissonClient.getLock(LOCK_PREFIX + lockKey);

        try {
            boolean acquired = lock.tryLock(waitTime, leaseTime, timeUnit);
            if (!acquired) {
                throw new DistributedLockException("락 획득에 실패했습니다: " + lockKey);
            }

            log.debug("Distributed lock acquired: {}", lockKey);
            return supplier.get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DistributedLockException("락 획득 중 인터럽트 발생: " + lockKey, e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("Distributed lock released: {}", lockKey);
            }
        }
    }

    /**
     * 분산락을 획득한 후 작업을 실행한다 (반환값 없음).
     *
     * @param lockKey   락 키
     * @param waitTime  락 획득 대기 시간
     * @param leaseTime 락 유지 시간
     * @param timeUnit  시간 단위
     * @param runnable  실행할 작업
     * @throws DistributedLockException 락 획득 실패 시
     */
    @Override
    public void executeWithLock(String lockKey, long waitTime, long leaseTime,
                                TimeUnit timeUnit, Runnable runnable) {
        executeWithLock(lockKey, waitTime, leaseTime, timeUnit, () -> {
            runnable.run();
            return null;
        });
    }

    /**
     * 기본 설정으로 분산락을 획득한 후 작업을 실행한다.
     * 대기 시간 3초, 유지 시간 10초.
     *
     * @param lockKey  락 키
     * @param supplier 실행할 작업
     * @param <T>      반환 타입
     * @return 작업 실행 결과
     */
    @Override
    public <T> T executeWithLock(String lockKey, Supplier<T> supplier) {
        return executeWithLock(lockKey, 3, 10, TimeUnit.SECONDS, supplier);
    }

    /**
     * 기본 설정으로 분산락을 획득한 후 작업을 실행한다 (반환값 없음).
     *
     * @param lockKey  락 키
     * @param runnable 실행할 작업
     */
    @Override
    public void executeWithLock(String lockKey, Runnable runnable) {
        executeWithLock(lockKey, 3, 10, TimeUnit.SECONDS, runnable);
    }
}
