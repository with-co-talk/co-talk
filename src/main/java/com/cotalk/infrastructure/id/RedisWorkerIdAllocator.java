package com.cotalk.infrastructure.id;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

/**
 * Redis를 사용한 분산 환경 Worker ID 할당기.
 *
 * <p>Snowflake ID 생성기에서 사용할 Worker ID를 Redis를 통해 자동으로 할당한다.
 * 분산 환경에서 각 인스턴스가 고유한 Worker ID를 가질 수 있도록 보장한다.
 *
 * <p>동작 방식:
 * <ul>
 *   <li>Redis INCR로 순차적 시퀀스 번호 획득</li>
 *   <li>시퀀스 번호를 Worker ID 범위(0-31)로 매핑</li>
 *   <li>SETNX로 해당 Worker ID 잠금 획득 시도</li>
 *   <li>잠금 실패 시 다음 ID로 재시도</li>
 *   <li>TTL 기반 자동 해제로 좀비 잠금 방지</li>
 * </ul>
 *
 * <p>인스턴스 종료 시 {@link #shutdown()}이 자동 호출되어 잠금을 해제한다.
 *
 * @author seunggu.lee
 * @see SnowflakeIdGenerator
 */
@Slf4j
public class RedisWorkerIdAllocator {

    private static final int MAX_WORKER_ID = 31;
    private static final int MAX_RETRY_COUNT = MAX_WORKER_ID + 1;
    private static final Duration LOCK_TTL = Duration.ofHours(24);

    private static final String SEQUENCE_KEY_FORMAT = "snowflake:worker:%d:sequence";
    private static final String LOCK_KEY_FORMAT = "snowflake:worker:%d:lock:%d";

    private final StringRedisTemplate redisTemplate;
    private final long datacenterId;
    private final long workerId;
    private final String lockKey;

    /**
     * RedisWorkerIdAllocator를 생성하고 Worker ID를 할당받는다.
     *
     * @param redisTemplate Redis 템플릿
     * @param datacenterId  데이터센터 ID (0-31)
     * @throws IllegalStateException 사용 가능한 Worker ID가 없는 경우
     */
    public RedisWorkerIdAllocator(StringRedisTemplate redisTemplate, long datacenterId) {
        this.redisTemplate = redisTemplate;
        this.datacenterId = datacenterId;
        this.workerId = allocateWorkerId();
        this.lockKey = String.format(LOCK_KEY_FORMAT, datacenterId, workerId);
        log.info("Worker ID allocated: datacenterId={}, workerId={}", datacenterId, workerId);
    }

    /**
     * 할당된 Worker ID를 반환한다.
     *
     * @return Worker ID (0-31)
     */
    public long getWorkerId() {
        return workerId;
    }

    /**
     * 데이터센터 ID를 반환한다.
     *
     * @return 데이터센터 ID
     */
    public long getDatacenterId() {
        return datacenterId;
    }

    /**
     * 인스턴스 종료 시 Worker ID 잠금을 해제한다.
     */
    @PreDestroy
    public void shutdown() {
        redisTemplate.delete(lockKey);
        log.info("Worker ID lock released: {}", lockKey);
    }

    /**
     * Redis를 통해 고유한 Worker ID를 할당받는다.
     *
     * @return 할당된 Worker ID (0-31)
     * @throws IllegalStateException 모든 Worker ID가 사용 중인 경우
     */
    private long allocateWorkerId() {
        String sequenceKey = String.format(SEQUENCE_KEY_FORMAT, datacenterId);

        for (int retry = 0; retry < MAX_RETRY_COUNT; retry++) {
            Long sequence = redisTemplate.opsForValue().increment(sequenceKey);
            if (sequence == null) {
                throw new IllegalStateException("Redis INCR 실패");
            }

            long candidateWorkerId = (sequence - 1) % (MAX_WORKER_ID + 1);
            String candidateLockKey = String.format(LOCK_KEY_FORMAT, datacenterId, candidateWorkerId);

            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(candidateLockKey, getInstanceId(), LOCK_TTL);

            if (Boolean.TRUE.equals(acquired)) {
                return candidateWorkerId;
            }

            log.debug("Worker ID {} is occupied, trying next...", candidateWorkerId);
        }

        throw new IllegalStateException(
                "사용 가능한 Worker ID가 없습니다. datacenterId=" + datacenterId);
    }

    /**
     * 현재 인스턴스를 식별하는 고유 ID를 반환한다.
     *
     * @return 인스턴스 식별자 (호스트명 + 타임스탬프)
     */
    private String getInstanceId() {
        String hostname = System.getenv("HOSTNAME");
        if (hostname == null || hostname.isBlank()) {
            hostname = "unknown";
        }
        return hostname + ":" + System.currentTimeMillis();
    }
}
