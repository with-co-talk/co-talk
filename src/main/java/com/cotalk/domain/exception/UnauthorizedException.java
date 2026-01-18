package com.cotalk.domain.exception;

/**
 * 인증되지 않은 사용자가 보호된 리소스에 접근할 때 발생하는 예외.
 *
 * @author seunggu.lee
 */
public class UnauthorizedException extends DomainException {

    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}
