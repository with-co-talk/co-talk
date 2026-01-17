package com.cotalk.infrastructure.id;

/**
 * Twitter Snowflake 알고리즘 기반 분산 ID 생성기.
 * 분산 환경에서 중복 없이 순차적이고 고유한 64비트 ID를 생성한다.
 *
 * <p>64비트 ID 구조:
 * <ul>
 *   <li>1비트: 사용 안 함 (항상 0, 양수 보장)</li>
 *   <li>41비트: 타임스탬프 (약 69년 사용 가능)</li>
 *   <li>5비트: 데이터센터 ID (0-31)</li>
 *   <li>5비트: 워커 ID (0-31)</li>
 *   <li>12비트: 시퀀스 번호 (밀리초당 4096개)</li>
 * </ul>
 *
 * <p>특징:
 * <ul>
 *   <li>시간순 정렬 가능 - ID에 타임스탬프가 포함되어 있어 생성 순서대로 정렬됨</li>
 *   <li>분산 환경 지원 - 데이터센터 ID와 워커 ID로 여러 노드에서 충돌 없이 생성 가능</li>
 *   <li>높은 처리량 - 밀리초당 최대 4096개의 고유 ID 생성 가능</li>
 * </ul>
 *
 * <p>에포크는 2024-01-01 00:00:00 UTC로 설정되어 있다.
 *
 * @author seunggu.lee
 */
public class SnowflakeIdGenerator {

    /** 에포크 시작 시간 (2024-01-01 00:00:00 UTC) */
    private static final long EPOCH = 1704067200000L;

    /** 데이터센터 ID에 할당된 비트 수 */
    private static final long DATACENTER_ID_BITS = 5L;

    /** 워커 ID에 할당된 비트 수 */
    private static final long WORKER_ID_BITS = 5L;

    /** 시퀀스 번호에 할당된 비트 수 */
    private static final long SEQUENCE_BITS = 12L;

    /** 데이터센터 ID 최대값 (31) */
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);

    /** 워커 ID 최대값 (31) */
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);

    /** 시퀀스 번호 최대값 (4095) */
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);

    /** 워커 ID 비트 시프트 */
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;

    /** 데이터센터 ID 비트 시프트 */
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

    /** 타임스탬프 비트 시프트 */
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;

    private final long datacenterId;
    private final long workerId;

    private long sequence = 0L;
    private long lastTimestamp = -1L;

    /**
     * SnowflakeIdGenerator를 생성한다.
     *
     * @param datacenterId 데이터센터 ID (0-31)
     * @param workerId     워커 ID (0-31)
     * @throws IllegalArgumentException 데이터센터 ID 또는 워커 ID가 유효 범위를 벗어난 경우
     */
    public SnowflakeIdGenerator(long datacenterId, long workerId) {
        if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
            throw new IllegalArgumentException(
                    "Datacenter ID must be between 0 and " + MAX_DATACENTER_ID);
        }
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException(
                    "Worker ID must be between 0 and " + MAX_WORKER_ID);
        }
        this.datacenterId = datacenterId;
        this.workerId = workerId;
    }

    /**
     * 새로운 고유 ID를 생성한다.
     *
     * <p>동일 밀리초 내에서는 시퀀스 번호를 증가시키고,
     * 시퀀스가 오버플로우되면 다음 밀리초까지 대기한다.
     *
     * @return 생성된 64비트 고유 ID
     * @throws IllegalStateException 시스템 시계가 역행한 경우
     */
    public synchronized long nextId() {
        long timestamp = currentTimeMillis();

        if (timestamp < lastTimestamp) {
            throw new IllegalStateException(
                    "Clock moved backwards. Refusing to generate ID.");
        }

        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                // 시퀀스 오버플로우, 다음 밀리초까지 대기
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    /**
     * ID에서 타임스탬프를 추출한다.
     *
     * @param id Snowflake ID
     * @return ID가 생성된 시점의 Unix 타임스탬프 (밀리초)
     */
    public long getTimestamp(long id) {
        return (id >> TIMESTAMP_SHIFT) + EPOCH;
    }

    /**
     * 다음 밀리초까지 대기한다.
     *
     * @param lastTimestamp 마지막으로 기록된 타임스탬프
     * @return 다음 밀리초의 타임스탬프
     */
    private long waitNextMillis(long lastTimestamp) {
        long timestamp = currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = currentTimeMillis();
        }
        return timestamp;
    }

    /**
     * 현재 시간을 밀리초로 반환한다.
     * 테스트를 위해 오버라이드 가능하다.
     *
     * @return 현재 Unix 타임스탬프 (밀리초)
     */
    protected long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}
