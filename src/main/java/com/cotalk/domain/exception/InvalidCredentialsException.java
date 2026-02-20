package com.cotalk.domain.exception;

/**
 * 인증 정보가 유효하지 않을 때 발생하는 예외.
 *
 * @author seunggu.lee
 */
public class InvalidCredentialsException extends DomainException {

    public InvalidCredentialsException() {
        super("이메일 또는 비밀번호가 올바르지 않습니다.", "INVALID_CREDENTIALS", HttpStatusHint.UNAUTHORIZED);
    }

    public InvalidCredentialsException(String message) {
        super(message, "INVALID_CREDENTIALS", HttpStatusHint.UNAUTHORIZED);
    }
}
