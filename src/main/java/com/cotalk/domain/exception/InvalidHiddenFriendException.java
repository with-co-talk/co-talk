package com.cotalk.domain.exception;

/**
 * 유효하지 않은 친구 숨김 요청일 때 발생하는 예외.
 *
 * @author seunggu.lee
 */
public class InvalidHiddenFriendException extends DomainException {

    public InvalidHiddenFriendException(String message) {
        super(message, "INVALID_HIDDEN_FRIEND", HttpStatusHint.BAD_REQUEST);
    }
}
