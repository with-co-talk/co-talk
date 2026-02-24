package com.cotalk.domain.exception;

/**
 * 유효하지 않은 차단 요청일 때 발생하는 예외.
 *
 * @author seunggu.lee
 */
public class InvalidBlockException extends DomainException {

    public InvalidBlockException(String message) {
        super(message, "INVALID_BLOCK", HttpStatusHint.BAD_REQUEST);
    }
}
