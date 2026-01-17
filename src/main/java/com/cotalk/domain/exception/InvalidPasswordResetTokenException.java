package com.cotalk.domain.exception;

/**
 * 비밀번호 재설정 토큰이 유효하지 않을 때 발생하는 예외.
 *
 * @author seunggu.lee
 */
public class InvalidPasswordResetTokenException extends DomainException {

    public InvalidPasswordResetTokenException(String message) {
        super(message);
    }

    public static InvalidPasswordResetTokenException expired() {
        return new InvalidPasswordResetTokenException("비밀번호 재설정 링크가 만료되었습니다.");
    }

    public static InvalidPasswordResetTokenException alreadyUsed() {
        return new InvalidPasswordResetTokenException("이미 사용된 비밀번호 재설정 링크입니다.");
    }

    public static InvalidPasswordResetTokenException notFound() {
        return new InvalidPasswordResetTokenException("유효하지 않은 비밀번호 재설정 링크입니다.");
    }
}
