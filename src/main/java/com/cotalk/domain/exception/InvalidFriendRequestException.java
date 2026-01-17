package com.cotalk.domain.exception;

/**
 * 유효하지 않은 친구 요청일 때 발생하는 예외.
 *
 * @author seunggu.lee
 */
public class InvalidFriendRequestException extends DomainException {

    public InvalidFriendRequestException(String message) {
        super(message);
    }
}
