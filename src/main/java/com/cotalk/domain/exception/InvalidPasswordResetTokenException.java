package com.cotalk.domain.exception;

/**
 * 비밀번호 재설정 토큰이 유효하지 않을 때 발생하는 예외.
 *
 * @author seunggu.lee
 */
public class InvalidPasswordResetTokenException extends DomainException {

    public InvalidPasswordResetTokenException(String message) {
        super(message, "INVALID_PASSWORD_RESET_TOKEN", HttpStatusHint.BAD_REQUEST);
    }

    private InvalidPasswordResetTokenException(String message, String errorCode) {
        super(message, errorCode, HttpStatusHint.BAD_REQUEST);
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

    /**
     * 인증 코드가 일치하지 않을 때 발생하는 예외를 생성한다.
     *
     * @return 인증 코드 불일치 예외
     */
    public static InvalidPasswordResetTokenException invalidCode() {
        return new InvalidPasswordResetTokenException(
                "인증 코드가 일치하지 않습니다. 다시 확인해주세요.",
                "INVALID_VERIFICATION_CODE"
        );
    }

    /**
     * 인증 코드 입력 횟수를 초과했을 때 발생하는 예외를 생성한다.
     *
     * @return 최대 시도 횟수 초과 예외
     */
    public static InvalidPasswordResetTokenException maxAttemptsExceeded() {
        return new InvalidPasswordResetTokenException(
                "인증 코드 입력 횟수를 초과했습니다. 새로운 코드를 요청해주세요.",
                "MAX_ATTEMPTS_EXCEEDED"
        );
    }
}
