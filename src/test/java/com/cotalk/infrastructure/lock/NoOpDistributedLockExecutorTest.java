package com.cotalk.infrastructure.lock;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.cotalk.infrastructure.config.properties.AppProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DistributedLockExecutor - NoOp 모드 (RedissonClient 없음)")
class NoOpDistributedLockExecutorTest {

    private static AppProperties appProperties(boolean failClosed) {
        return new AppProperties(null, null, null, null, null, null, null, null,
                new AppProperties.Lock(failClosed));
    }

    private final DistributedLockExecutor executor =
            new DistributedLockExecutor(null, appProperties(false));

    @Nested
    @DisplayName("executeWithLock - Supplier")
    class ExecuteWithLockSupplier {

        @Test
        @DisplayName("락 없이 Supplier를 즉시 실행한다")
        void should_executeSupplier_immediately() {
            // given
            String lockKey = "test-lock";
            AtomicInteger counter = new AtomicInteger(0);

            // when
            Integer result = executor.executeWithLock(lockKey, () -> {
                counter.incrementAndGet();
                return 42;
            });

            // then
            assertThat(result).isEqualTo(42);
            assertThat(counter.get()).isEqualTo(1);
        }

        @Test
        @DisplayName("파라미터가 있는 executeWithLock도 즉시 실행한다")
        void should_executeSupplierWithParams_immediately() {
            // given
            String lockKey = "test-lock";
            long waitTime = 100L;
            long leaseTime = 200L;
            AtomicInteger counter = new AtomicInteger(0);

            // when
            String result = executor.executeWithLock(
                    lockKey, waitTime, leaseTime, TimeUnit.MILLISECONDS,
                    () -> {
                        counter.incrementAndGet();
                        return "result";
                    }
            );

            // then
            assertThat(result).isEqualTo("result");
            assertThat(counter.get()).isEqualTo(1);
        }

        @Test
        @DisplayName("null을 반환하는 Supplier도 처리한다")
        void should_handleNullResult() {
            // given
            String lockKey = "test-lock";

            // when
            String result = executor.executeWithLock(lockKey, () -> null);

            // then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("executeWithLock - Runnable")
    class ExecuteWithLockRunnable {

        @Test
        @DisplayName("락 없이 Runnable을 즉시 실행한다")
        void should_executeRunnable_immediately() {
            // given
            String lockKey = "test-lock";
            AtomicInteger counter = new AtomicInteger(0);

            // when
            executor.executeWithLock(lockKey, counter::incrementAndGet);

            // then
            assertThat(counter.get()).isEqualTo(1);
        }

        @Test
        @DisplayName("파라미터가 있는 executeWithLock도 즉시 실행한다")
        void should_executeRunnableWithParams_immediately() {
            // given
            String lockKey = "test-lock";
            long waitTime = 100L;
            long leaseTime = 200L;
            AtomicInteger counter = new AtomicInteger(0);

            // when
            executor.executeWithLock(
                    lockKey, waitTime, leaseTime, TimeUnit.MILLISECONDS,
                    counter::incrementAndGet
            );

            // then
            assertThat(counter.get()).isEqualTo(1);
        }

        @Test
        @DisplayName("예외를 던지는 Runnable도 처리한다")
        void should_handleException() {
            // given
            String lockKey = "test-lock";

            // when & then
            org.junit.jupiter.api.Assertions.assertThrows(
                    RuntimeException.class,
                    () -> executor.executeWithLock(lockKey, () -> {
                        throw new RuntimeException("Test exception");
                    })
            );
        }
    }

    @Nested
    @DisplayName("동시성 테스트")
    class ConcurrencyTest {

        @Test
        @DisplayName("여러 스레드에서 동시에 실행해도 모두 즉시 실행된다")
        void should_executeConcurrently() throws InterruptedException {
            // given
            String lockKey = "test-lock";
            AtomicInteger counter = new AtomicInteger(0);
            int threadCount = 10;

            // when
            Thread[] threads = new Thread[threadCount];
            for (int i = 0; i < threadCount; i++) {
                threads[i] = new Thread(() -> {
                    executor.executeWithLock(lockKey, counter::incrementAndGet);
                });
                threads[i].start();
            }

            for (Thread thread : threads) {
                thread.join();
            }

            // then
            assertThat(counter.get()).isEqualTo(threadCount);
        }
    }

    @Nested
    @DisplayName("NoOp 경고 레이트리밋")
    class WarnRateLimit {

        @Test
        @DisplayName("다발 호출돼도 경고는 윈도우당 한 번만 남는다 (폭주 방지)")
        void should_warnOnlyOnce_when_calledManyTimes() {
            // given: DistributedLockExecutor 로거에 ListAppender 부착
            Logger logger = (Logger) LoggerFactory.getLogger(DistributedLockExecutor.class);
            ListAppender<ILoggingEvent> appender = new ListAppender<>();
            appender.start();
            logger.addAppender(appender);
            try {
                DistributedLockExecutor noOpExecutor =
                        new DistributedLockExecutor(null, appProperties(false));
                // 생성자 진입 경고 1회를 캡처에서 제외하기 위해 비운다
                appender.list.clear();

                // when: 동일 키로 여러 번 호출 (Redis 다운 시 락 사용처 다발 호출 시뮬레이션)
                for (int i = 0; i < 100; i++) {
                    noOpExecutor.executeWithLock("friend-accept:" + i, () -> null);
                }

                // then: 윈도우(60s) 내에서는 첫 호출 1회만 warn
                long warnCount = appender.list.stream()
                        .filter(e -> e.getLevel() == Level.WARN)
                        .filter(e -> e.getFormattedMessage().contains("분산락 비활성(NoOp)"))
                        .count();
                assertThat(warnCount).isEqualTo(1);
            } finally {
                logger.detachAppender(appender);
            }
        }
    }

    @Nested
    @DisplayName("상태 노출")
    class StateExposure {

        @Test
        @DisplayName("RedissonClient가 없으면 isNoOpMode()는 true다")
        void should_reportNoOpMode_when_redissonAbsent() {
            assertThat(executor.isNoOpMode()).isTrue();
            assertThat(executor.isFailClosed()).isFalse();
        }
    }

    @Nested
    @DisplayName("fail-closed=true (RedissonClient 없음)")
    class FailClosed {

        private final DistributedLockExecutor failClosedExecutor =
                new DistributedLockExecutor(null, appProperties(true));

        @Test
        @DisplayName("락 보호가 필요한 Supplier 실행 시 예외를 던지고 작업을 실행하지 않는다")
        void should_throw_when_supplierAndFailClosed() {
            // given
            AtomicInteger counter = new AtomicInteger(0);

            // when & then
            assertThatThrownBy(() -> failClosedExecutor.executeWithLock("test-lock", () -> {
                counter.incrementAndGet();
                return 42;
            }))
                    .isInstanceOf(DistributedLockException.class)
                    .hasMessageContaining("fail-closed");
            assertThat(counter.get()).isZero();
        }

        @Test
        @DisplayName("파라미터가 있는 executeWithLock도 예외를 던진다")
        void should_throw_when_supplierWithParamsAndFailClosed() {
            assertThatThrownBy(() -> failClosedExecutor.executeWithLock(
                    "test-lock", 100L, 200L, TimeUnit.MILLISECONDS, () -> "result"))
                    .isInstanceOf(DistributedLockException.class);
        }

        @Test
        @DisplayName("Runnable 실행 시에도 예외를 던지고 작업을 실행하지 않는다")
        void should_throw_when_runnableAndFailClosed() {
            // given
            AtomicInteger counter = new AtomicInteger(0);

            // when & then
            assertThatThrownBy(
                    () -> failClosedExecutor.executeWithLock("test-lock", counter::incrementAndGet))
                    .isInstanceOf(DistributedLockException.class);
            assertThat(counter.get()).isZero();
        }

        @Test
        @DisplayName("isNoOpMode()는 true, isFailClosed()는 true다")
        void should_reportState() {
            assertThat(failClosedExecutor.isNoOpMode()).isTrue();
            assertThat(failClosedExecutor.isFailClosed()).isTrue();
        }
    }
}
