package com.cotalk.domain.exception;

/**
 * 도메인 계층에서 발생하는 모든 예외의 기본 클래스.
 *
 * @author seunggu.lee
 */
public class DomainException extends RuntimeException {

    private final String errorCode;
    private final HttpStatusHint statusHint;

    public DomainException(String message) {
        this(message, "BAD_REQUEST", HttpStatusHint.BAD_REQUEST);
    }

    public DomainException(String message, Throwable cause) {
        this(message, "BAD_REQUEST", HttpStatusHint.BAD_REQUEST, cause);
    }

    public DomainException(String message, String errorCode, HttpStatusHint statusHint) {
        super(message);
        this.errorCode = errorCode;
        this.statusHint = statusHint;
    }

    public DomainException(String message, String errorCode, HttpStatusHint statusHint, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.statusHint = statusHint;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public HttpStatusHint getStatusHint() {
        return statusHint;
    }
}
