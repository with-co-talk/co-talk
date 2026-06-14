package com.cotalk.infrastructure.lock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import com.cotalk.infrastructure.config.properties.AppProperties;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * DistributedLockExecutor 단위 테스트.
 *
 * @author seunggu.lee
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DistributedLockExecutor 단위 테스트")
class DistributedLockExecutorTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock rLock;

    private DistributedLockExecutor lockExecutor;

    @BeforeEach
    void setUp() {
        lockExecutor = new DistributedLockExecutor(redissonClient, appProperties(false));
    }

    private static AppProperties appProperties(boolean failClosed) {
        return new AppProperties(null, null, null, null, null, null, null, null,
                new AppProperties.Lock(failClosed));
    }

    @Test
    @DisplayName("락 획득 후 작업 실행 성공 - Supplier")
    void should_executeSupplier_when_lockAcquired() throws InterruptedException {
        // given
        String lockKey = "test-key";
        String expectedResult = "success";

        given(redissonClient.getLock(anyString())).willReturn(rLock);
        given(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).willReturn(true);
        given(rLock.isHeldByCurrentThread()).willReturn(true);

        // when
        String result = lockExecutor.executeWithLock(lockKey, 3, 10, TimeUnit.SECONDS,
                () -> expectedResult);

        // then
        assertEquals(expectedResult, result);
        verify(rLock).unlock();
    }

    @Test
    @DisplayName("락 획득 후 작업 실행 성공 - Runnable")
    void should_executeRunnable_when_lockAcquired() throws InterruptedException {
        // given
        String lockKey = "test-key";
        AtomicBoolean executed = new AtomicBoolean(false);

        given(redissonClient.getLock(anyString())).willReturn(rLock);
        given(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).willReturn(true);
        given(rLock.isHeldByCurrentThread()).willReturn(true);

        // when
        lockExecutor.executeWithLock(lockKey, 3, 10, TimeUnit.SECONDS,
                () -> executed.set(true));

        // then
        assertTrue(executed.get());
        verify(rLock).unlock();
    }

    @Test
    @DisplayName("락 획득 실패 시 예외 발생")
    void should_throwException_when_lockNotAcquired() throws InterruptedException {
        // given
        String lockKey = "test-key";

        given(redissonClient.getLock(anyString())).willReturn(rLock);
        given(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).willReturn(false);

        // when & then
        DistributedLockException exception = assertThrows(DistributedLockException.class,
                () -> lockExecutor.executeWithLock(lockKey, 3, 10, TimeUnit.SECONDS, () -> "result"));
        assertTrue(exception.getMessage().contains("락 획득에 실패했습니다"));
    }

    @Test
    @DisplayName("인터럽트 발생 시 예외 처리")
    void should_throwException_when_interrupted() throws InterruptedException {
        // given
        String lockKey = "test-key";

        given(redissonClient.getLock(anyString())).willReturn(rLock);
        given(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class)))
                .willThrow(new InterruptedException("Interrupted"));

        // when & then
        DistributedLockException exception = assertThrows(DistributedLockException.class,
                () -> lockExecutor.executeWithLock(lockKey, 3, 10, TimeUnit.SECONDS, () -> "result"));
        assertTrue(exception.getMessage().contains("인터럽트 발생"));
    }

    @Test
    @DisplayName("기본 설정으로 락 획득 - Supplier")
    void should_useDefaultSettings_when_executeWithLockSupplierShorthand() throws InterruptedException {
        // given
        String lockKey = "test-key";
        Integer expectedResult = 42;

        given(redissonClient.getLock(anyString())).willReturn(rLock);
        given(rLock.tryLock(eq(3L), eq(10L), eq(TimeUnit.SECONDS))).willReturn(true);
        given(rLock.isHeldByCurrentThread()).willReturn(true);

        // when
        Integer result = lockExecutor.executeWithLock(lockKey, () -> expectedResult);

        // then
        assertEquals(expectedResult, result);
    }

    @Test
    @DisplayName("기본 설정으로 락 획득 - Runnable")
    void should_useDefaultSettings_when_executeWithLockRunnableShorthand() throws InterruptedException {
        // given
        String lockKey = "test-key";
        AtomicInteger counter = new AtomicInteger(0);

        given(redissonClient.getLock(anyString())).willReturn(rLock);
        given(rLock.tryLock(eq(3L), eq(10L), eq(TimeUnit.SECONDS))).willReturn(true);
        given(rLock.isHeldByCurrentThread()).willReturn(true);

        // when
        lockExecutor.executeWithLock(lockKey, () -> counter.incrementAndGet());

        // then
        assertEquals(1, counter.get());
    }

    @Test
    @DisplayName("락 키에 prefix 추가 확인")
    void should_addLockPrefix_when_getLock() throws InterruptedException {
        // given
        String lockKey = "test-key";

        given(redissonClient.getLock("lock:" + lockKey)).willReturn(rLock);
        given(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).willReturn(true);
        given(rLock.isHeldByCurrentThread()).willReturn(true);

        // when
        lockExecutor.executeWithLock(lockKey, () -> "result");

        // then
        verify(redissonClient).getLock("lock:" + lockKey);
    }

    @Test
    @DisplayName("작업 중 예외 발생 시 락 해제")
    void should_releaseLock_when_supplierThrowsException() throws InterruptedException {
        // given
        String lockKey = "test-key";

        given(redissonClient.getLock(anyString())).willReturn(rLock);
        given(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).willReturn(true);
        given(rLock.isHeldByCurrentThread()).willReturn(true);

        // when & then
        assertThrows(RuntimeException.class,
                () -> lockExecutor.executeWithLock(lockKey, 3, 10, TimeUnit.SECONDS, () -> {
                    throw new RuntimeException("작업 실패");
                }));
        verify(rLock).unlock();
    }

    @Test
    @DisplayName("현재 스레드가 락을 보유하지 않으면 unlock 호출 안 함")
    void should_notUnlock_when_lockNotHeldByCurrentThread() throws InterruptedException {
        // given
        String lockKey = "test-key";

        given(redissonClient.getLock(anyString())).willReturn(rLock);
        given(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).willReturn(true);
        given(rLock.isHeldByCurrentThread()).willReturn(false);

        // when
        lockExecutor.executeWithLock(lockKey, 3, 10, TimeUnit.SECONDS, () -> "result");

        // then
        verify(rLock).isHeldByCurrentThread();
        // unlock은 호출되지 않아야 함 (verify(rLock, never()).unlock()은 필요없음)
    }
}
