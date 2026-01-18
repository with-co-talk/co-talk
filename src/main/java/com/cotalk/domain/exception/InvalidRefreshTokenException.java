package com.cotalk.domain.exception;

/**
 * 유효하지 않은 Refresh Token 예외.
 * Refresh Token이 만료되었거나, 폐기되었거나, 존재하지 않는 경우 발생한다.
 *
 * @author seunggu.lee
 */
public class InvalidRefreshTokenException extends DomainException {

    private static final String DEFAULT_MESSAGE = "유효하지 않은 리프레시 토큰입니다.";

    public InvalidRefreshTokenException() {
        super(DEFAULT_MESSAGE);
    }

    public InvalidRefreshTokenException(String message) {
        super(message);
    }
}
