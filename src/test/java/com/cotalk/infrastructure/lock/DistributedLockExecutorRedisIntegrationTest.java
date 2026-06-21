package com.cotalk.infrastructure.lock;

import com.cotalk.infrastructure.config.properties.AppProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <b>실제 Redisson 분산락 경합 통합 테스트 (Redis testcontainer).</b>
 *
 * <p>{@link DistributedLockExecutorTest}는 {@code RLock}을 모킹하므로 "락이 실제로 상호배제를
 * 보장하는가"를 증명하지 못한다. 이 테스트는 실제 Redis + 실제 Redisson 락으로:
 * <ul>
 *   <li>두 스레드가 같은 키를 경합할 때 임계영역 동시 진입이 절대 없음(상호배제)을 검증한다.</li>
 *   <li>{@code finally}의 {@code unlock()}으로 락이 깨끗이 해제되어 다음 획득이 가능함을 검증한다.</li>
 *   <li>워치독(leaseTime=-1)이 기본 lease를 넘겨 오래 잡힌 락을 살려두는지(자동 연장) 검증한다.</li>
 * </ul>
 *
 * <p>결정성: 슬립을 단정의 근거로 쓰지 않는다. {@link CountDownLatch}/배리어로 스레드 진입 순서를
 * 동기화하고, 임계영역 동시 점유 카운터의 최대값으로 상호배제를 단정한다.</p>
 *
 * @author seunggu.lee
 */
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("실제 Redisson 분산락 경합 통합")
class DistributedLockExecutorRedisIntegrationTest {

    private static final DockerImageName REDIS_IMAGE = DockerImageName.parse("redis:7-alpine");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(REDIS_IMAGE)
            .withExposedPorts(6379);

    private static RedissonClient redissonClient;
    private static DistributedLockExecutor lockExecutor;

    @BeforeAll
    static void startClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + REDIS.getHost() + ":" + REDIS.getFirstMappedPort());
        // 워치독 lease를 짧게(2초) 잡아 "기본 lease 초과 생존"을 빠르고 결정적으로 검증한다.
        config.setLockWatchdogTimeout(2_000L);
        redissonClient = Redisson.create(config);
        lockExecutor = new DistributedLockExecutor(redissonClient, lockProps());
    }

    @AfterAll
    static void stopClient() {
        if (redissonClient != null) {
            redissonClient.shutdown();
        }
    }

    private static AppProperties lockProps() {
        return new AppProperties(null, null, null, null, null, null, null, null,
                new AppProperties.Lock(false));
    }

    @Test
    @DisplayName("같은 키를 두 스레드가 경합해도 임계영역에 동시에 둘 이상 진입하지 못한다 (상호배제)")
    void should_enforceMutualExclusion_when_twoThreadsContendSameKey() throws Exception {
        String lockKey = "contention-test-key";
        AtomicInteger concurrentOccupancy = new AtomicInteger(0);
        AtomicInteger maxConcurrentObserved = new AtomicInteger(0);
        AtomicInteger executionCount = new AtomicInteger(0);

        // 두 스레드가 동시에 출발하도록 배리어 역할의 latch
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);

        Runnable contender = () -> {
            readyLatch.countDown();
            try {
                startLatch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            lockExecutor.executeWithLock(lockKey, 5, 5, TimeUnit.SECONDS, () -> {
                int now = concurrentOccupancy.incrementAndGet();
                // 관측된 최대 동시 점유 갱신
                maxConcurrentObserved.accumulateAndGet(now, Math::max);
                executionCount.incrementAndGet();
                // 임계영역을 인위적으로 잠깐 점유(상호배제 위반 시 다른 스레드와 겹치게 만들려는 의도).
                // 슬립을 단정 근거로 쓰지 않는다 — 단정은 maxConcurrentObserved로 한다.
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                concurrentOccupancy.decrementAndGet();
                return null;
            });
        };

        pool.submit(contender);
        pool.submit(contender);

        readyLatch.await(5, TimeUnit.SECONDS); // 두 스레드가 진입 직전까지 대기
        startLatch.countDown();                 // 동시 출발
        pool.shutdown();
        boolean finished = pool.awaitTermination(20, TimeUnit.SECONDS);

        assertThat(finished).as("두 작업 모두 데드락 없이 완료되어야 함").isTrue();
        assertThat(executionCount.get()).as("두 작업 모두 임계영역을 실행해야 함").isEqualTo(2);
        assertThat(maxConcurrentObserved.get())
                .as("임계영역 동시 점유 최대값은 1이어야 함 (상호배제)")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("락은 finally에서 깨끗이 해제되어 같은 키의 다음 획득이 즉시 가능하다")
    void should_releaseLockCleanly_after_criticalSection() {
        String lockKey = "clean-release-key";

        String first = lockExecutor.executeWithLock(lockKey, 3, 3, TimeUnit.SECONDS, () -> "first");
        assertThat(first).isEqualTo("first");

        // 직전 작업이 락을 해제했으므로 즉시 재획득되어야 한다(대기 없이).
        String second = lockExecutor.executeWithLock(lockKey, 1, 3, TimeUnit.SECONDS, () -> "second");
        assertThat(second).isEqualTo("second");

        // Redis에 락 키가 남아있지 않아야 한다(누수 없음).
        RLock lock = redissonClient.getLock("lock:" + lockKey);
        assertThat(lock.isLocked()).as("작업 종료 후 락이 남아있으면 안 됨").isFalse();
    }

    @Test
    @DisplayName("워치독이 기본 lease를 넘겨 오래 잡힌 락의 만료를 자동 연장한다")
    void should_renewLeaseViaWatchdog_when_lockHeldBeyondLease() throws Exception {
        // watchdogTimeout=2초로 설정됨. leaseTime=-1(워치독 위임)로 잡고 3초간(>2초) 보유한다.
        // 워치독이 없다면 2초 후 락이 만료돼 isLocked()=false가 되겠지만, 워치독이 연장하므로
        // 보유 중에는 계속 잠겨 있어야 한다.
        String lockKey = "watchdog-key";
        CountDownLatch insideCritical = new CountDownLatch(1);
        CountDownLatch releaseSignal = new CountDownLatch(1);

        ExecutorService holder = Executors.newSingleThreadExecutor();
        holder.submit(() -> lockExecutor.executeWithLock(lockKey, supplierHolding(insideCritical, releaseSignal)));

        // 보유 스레드가 임계영역에 진입할 때까지 대기
        assertThat(insideCritical.await(5, TimeUnit.SECONDS)).isTrue();

        RLock observer = redissonClient.getLock("lock:" + lockKey);
        // 기본 lease(2초)를 확실히 넘긴 시점(3초)에서도 여전히 잠겨 있어야 한다(워치독 연장).
        Thread.sleep(3_000L);
        boolean stillLocked = observer.isLocked();

        releaseSignal.countDown(); // 보유 스레드 해제
        holder.shutdown();
        holder.awaitTermination(10, TimeUnit.SECONDS);

        assertThat(stillLocked)
                .as("워치독이 기본 lease(2초)를 넘겨 락을 연장해 3초 시점에도 잠겨 있어야 함")
                .isTrue();
        // 해제 후에는 풀려야 한다.
        assertThat(observer.isLocked()).as("해제 후 락은 풀려야 함").isFalse();
    }

    private static java.util.function.Supplier<Void> supplierHolding(CountDownLatch insideCritical,
                                                                     CountDownLatch releaseSignal) {
        return () -> {
            insideCritical.countDown();
            try {
                // 외부 신호가 올 때까지 임계영역을 보유 (워치독이 그 동안 락을 연장)
                releaseSignal.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return null;
        };
    }
}
