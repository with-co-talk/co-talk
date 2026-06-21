package com.cotalk.domain.port.outbound;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 분산락 실행을 위한 포트 인터페이스.
 * Application 레이어에서 인프라스트럭처의 분산락 구현에 직접 의존하지 않도록
 * 포트를 통해 간접적으로 접근한다.
 *
 * @author seunggu.lee
 */
public interface DistributedLockPort {

    /**
     * 분산락을 획득한 후 작업을 실행한다.
     *
     * @param lockKey   락 키
     * @param waitTime  락 획득 대기 시간
     * @param leaseTime 락 유지 시간
     * @param timeUnit  시간 단위
     * @param supplier  실행할 작업
     * @param <T>       반환 타입
     * @return 작업 실행 결과
     */
    <T> T executeWithLock(String lockKey, long waitTime, long leaseTime,
                          TimeUnit timeUnit, Supplier<T> supplier);

    /**
     * 분산락을 획득한 후 작업을 실행한다 (반환값 없음).
     *
     * @param lockKey   락 키
     * @param waitTime  락 획득 대기 시간
     * @param leaseTime 락 유지 시간
     * @param timeUnit  시간 단위
     * @param runnable  실행할 작업
     */
    void executeWithLock(String lockKey, long waitTime, long leaseTime,
                         TimeUnit timeUnit, Runnable runnable);

    /**
     * 기본 설정(대기 3초, 유지 시간은 워치독 자동 연장)으로 분산락을 획득한 후 작업을 실행한다.
     *
     * <p>락 임계영역 안의 DB 트랜잭션이 길어져도 락이 트랜잭션 도중 만료되지 않도록
     * 구현체가 락 유지 시간을 자동 연장(워치독)한다.</p>
     *
     * @param lockKey  락 키
     * @param supplier 실행할 작업
     * @param <T>      반환 타입
     * @return 작업 실행 결과
     */
    <T> T executeWithLock(String lockKey, Supplier<T> supplier);

    /**
     * 기본 설정(대기 3초, 유지 시간은 워치독 자동 연장)으로 분산락을 획득한 후 작업을 실행한다 (반환값 없음).
     *
     * @param lockKey  락 키
     * @param runnable 실행할 작업
     */
    void executeWithLock(String lockKey, Runnable runnable);
}
