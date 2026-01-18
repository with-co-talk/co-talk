package com.cotalk.infrastructure.lock;

/**
 * 분산락 획득 실패 시 발생하는 예외.
 *
 * @author seunggu.lee
 */
public class DistributedLockException extends RuntimeException {

    /**
     * 메시지를 포함한 예외를 생성한다.
     *
     * @param message 예외 메시지
     */
    public DistributedLockException(String message) {
        super(message);
    }

    /**
     * 메시지와 원인을 포함한 예외를 생성한다.
     *
     * @param message 예외 메시지
     * @param cause   원인 예외
     */
    public DistributedLockException(String message, Throwable cause) {
        super(message, cause);
    }
}
