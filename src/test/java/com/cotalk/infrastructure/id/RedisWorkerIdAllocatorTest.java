package com.cotalk.infrastructure.id;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * RedisWorkerIdAllocator 단위 테스트.
 *
 * @author seunggu.lee
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RedisWorkerIdAllocator 테스트")
class RedisWorkerIdAllocatorTest {

    private static final long DATACENTER_ID = 0L;
    private static final int MAX_WORKER_ID = 31;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisWorkerIdAllocator allocator;

    @BeforeEach
    void setUp() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
    }

    @Test
    @DisplayName("첫 번째 인스턴스는 workerId 0을 할당받는다")
    void should_allocate_worker_id_0_when_first_instance() {
        // given
        given(valueOperations.increment(anyString())).willReturn(1L);
        given(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .willReturn(true);

        // when
        allocator = new RedisWorkerIdAllocator(redisTemplate, DATACENTER_ID);
        long workerId = allocator.getWorkerId();

        // then
        assertThat(workerId).isZero();
    }

    @Test
    @DisplayName("두 번째 인스턴스는 workerId 1을 할당받는다")
    void should_allocate_worker_id_1_when_second_instance() {
        // given
        given(valueOperations.increment(anyString())).willReturn(2L);
        given(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .willReturn(true);

        // when
        allocator = new RedisWorkerIdAllocator(redisTemplate, DATACENTER_ID);
        long workerId = allocator.getWorkerId();

        // then
        assertThat(workerId).isEqualTo(1L);
    }

    @Test
    @DisplayName("workerId가 최대값을 초과하면 0부터 다시 순환한다")
    void should_wrap_around_when_worker_id_exceeds_max() {
        // given
        given(valueOperations.increment(anyString())).willReturn(33L); // 32번째 -> workerId 0
        given(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .willReturn(true);

        // when
        allocator = new RedisWorkerIdAllocator(redisTemplate, DATACENTER_ID);
        long workerId = allocator.getWorkerId();

        // then
        assertThat(workerId).isZero();
    }

    @Test
    @DisplayName("이미 사용 중인 workerId는 건너뛰고 다음 ID를 할당한다")
    void should_skip_occupied_worker_id_and_allocate_next() {
        // given
        AtomicLong counter = new AtomicLong(0);
        given(valueOperations.increment(anyString())).willAnswer(inv -> counter.incrementAndGet());
        // 첫 번째 ID(0)는 이미 사용 중, 두 번째 ID(1)는 사용 가능
        given(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .willReturn(false)  // workerId 0 사용 중
                .willReturn(true);  // workerId 1 사용 가능

        // when
        allocator = new RedisWorkerIdAllocator(redisTemplate, DATACENTER_ID);
        long workerId = allocator.getWorkerId();

        // then
        assertThat(workerId).isEqualTo(1L);
    }

    @Test
    @DisplayName("모든 workerId가 사용 중이면 예외를 던진다")
    void should_throw_exception_when_all_worker_ids_occupied() {
        // given
        AtomicLong counter = new AtomicLong(0);
        given(valueOperations.increment(anyString())).willAnswer(inv -> counter.incrementAndGet());
        given(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .willReturn(false); // 모든 ID 사용 중

        // when & then
        assertThatThrownBy(() -> new RedisWorkerIdAllocator(redisTemplate, DATACENTER_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("사용 가능한 Worker ID가 없습니다");
    }

    @Test
    @DisplayName("할당된 workerId는 유효 범위(0-31) 내에 있다")
    void should_allocate_worker_id_within_valid_range() {
        // given
        given(valueOperations.increment(anyString())).willReturn(15L);
        given(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .willReturn(true);

        // when
        allocator = new RedisWorkerIdAllocator(redisTemplate, DATACENTER_ID);
        long workerId = allocator.getWorkerId();

        // then
        assertThat(workerId).isBetween(0L, (long) MAX_WORKER_ID);
    }

    @Test
    @DisplayName("shutdown 호출 시 workerId 잠금을 해제한다")
    void should_release_worker_id_lock_on_shutdown() {
        // given
        given(valueOperations.increment(anyString())).willReturn(1L);
        given(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .willReturn(true);
        allocator = new RedisWorkerIdAllocator(redisTemplate, DATACENTER_ID);

        // when
        allocator.shutdown();

        // then
        verify(redisTemplate).delete(eq("snowflake:worker:0:lock:0"));
    }

    @Test
    @DisplayName("datacenter ID가 다르면 독립적인 workerId 공간을 사용한다")
    void should_use_independent_worker_id_space_for_different_datacenter() {
        // given
        long differentDatacenterId = 1L;
        given(valueOperations.increment(anyString())).willReturn(1L);
        given(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .willReturn(true);

        // when
        allocator = new RedisWorkerIdAllocator(redisTemplate, differentDatacenterId);

        // then
        verify(valueOperations).increment("snowflake:worker:1:sequence");
    }
}
