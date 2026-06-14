package com.cotalk.infrastructure.lock;

import com.cotalk.domain.port.outbound.DistributedLockPort;
import com.cotalk.infrastructure.config.properties.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 분산락 실행기.
 *
 * <p>분산 환경에서 동시성 제어가 필요한 작업을 안전하게 실행한다.
 * Redis를 사용하여 여러 서버 인스턴스 간에 락을 공유한다.
 *
 * <p>RedissonClient가 없으면(예: 기동 시 Redis 미가용으로
 * {@link com.cotalk.infrastructure.config.DockerRedissonConfig}가 빈을 null로 반환) 분산락이
 * 비활성화된다. 이때 동작은 {@code app.lock.fail-closed} 설정에 따른다:</p>
 * <ul>
 *   <li>{@code false}(기본, 하위호환): NoOp 모드. 락 없이 즉시 작업을 실행하되, 호출마다 경고를
 *       남겨 동시성 보호가 사라진 사실을 운영자가 인지할 수 있게 한다.</li>
 *   <li>{@code true}(운영 권장): fail-closed. 락 보호가 필요한 작업을 락 없이 실행하지 않고
 *       {@link DistributedLockException}을 던져 임계영역이 조용히 무방비로 실행되는 것을 차단한다.</li>
 * </ul>
 *
 * <p>NoOp 강등 상태는 {@link #isNoOpMode()}로 노출되며, 헬스 인디케이터가 이를 운영 가시성으로
 * 변환한다.</p>
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
 * @author seunggu.lee
 */
@Slf4j
@Component
public class DistributedLockExecutor implements DistributedLockPort {

    private static final String LOCK_PREFIX = "lock:";

    private final RedissonClient redissonClient;
    private final boolean failClosed;

    /**
     * DistributedLockExecutor 생성자.
     * RedissonClient가 없으면 분산락이 비활성화된다({@code app.lock.fail-closed} 정책에 따라
     * NoOp 또는 fail-closed로 동작).
     *
     * @param redissonClient Redisson 클라이언트 (선택적, 없으면 null)
     * @param appProperties  애플리케이션 설정 (분산락 fail-closed 정책 포함)
     */
    public DistributedLockExecutor(@Autowired(required = false) RedissonClient redissonClient,
                                   AppProperties appProperties) {
        this.redissonClient = redissonClient;
        this.failClosed = appProperties.lock().failClosed();
        if (redissonClient == null) {
            if (failClosed) {
                log.error("RedissonClient not available and app.lock.fail-closed=true. "
                        + "Lock-protected operations will FAIL until Redis is reachable and the app is restarted.");
            } else {
                log.warn("RedissonClient not available. Distributed locks disabled (NoOp mode). "
                        + "Concurrency-critical sections run WITHOUT locks. "
                        + "Set app.lock.fail-closed=true to reject such operations instead.");
            }
        } else {
            log.info("Distributed locks enabled with Redisson.");
        }
    }

    /**
     * 분산락이 NoOp으로 강등된 상태인지 여부.
     *
     * <p>RedissonClient가 없으면 분산락이 비활성화된 것이며, 이때 락 보호가 사라진다
     * (fail-closed가 켜져 있으면 예외로 차단). 헬스 인디케이터/운영 가시성 용도로 노출한다.</p>
     *
     * @return RedissonClient가 없으면 true (분산락 비활성)
     */
    public boolean isNoOpMode() {
        return redissonClient == null;
    }

    /**
     * fail-closed 정책 활성화 여부.
     *
     * @return {@code app.lock.fail-closed} 설정값
     */
    public boolean isFailClosed() {
        return failClosed;
    }

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
     * @throws DistributedLockException 락 획득 실패 시, 또는 분산락 비활성 + fail-closed인 경우
     */
    @Override
    public <T> T executeWithLock(String lockKey, long waitTime, long leaseTime,
                                  TimeUnit timeUnit, Supplier<T> supplier) {
        if (redissonClient == null) {
            if (failClosed) {
                throw new DistributedLockException(
                        "분산락이 비활성화되어(Redis 미가용) 작업을 거부합니다 (fail-closed): " + lockKey);
            }
            log.warn("분산락 비활성(NoOp): 락 없이 작업을 실행합니다. 동시성 보호가 적용되지 않습니다. key={}",
                    lockKey);
            return supplier.get();
        }

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
