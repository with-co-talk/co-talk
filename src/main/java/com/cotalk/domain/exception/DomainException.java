package com.cotalk.domain.exception;

/**
 * 도메인 계층에서 발생하는 모든 예외의 기본 클래스.
 *
 * @author seunggu.lee
 */
public class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }

    public DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
