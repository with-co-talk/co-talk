package com.cotalk.infrastructure.lock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DistributedLockExecutor - NoOp 모드 (RedissonClient 없음)")
class NoOpDistributedLockExecutorTest {

    private final DistributedLockExecutor executor = new DistributedLockExecutor(null);

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
}
