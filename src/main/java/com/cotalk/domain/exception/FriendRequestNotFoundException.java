package com.cotalk.domain.exception;

/**
 * 친구 요청을 찾을 수 없을 때 발생하는 예외.
 *
 * @author seunggu.lee
 */
public class FriendRequestNotFoundException extends DomainException {

    public FriendRequestNotFoundException(Long requestId) {
        super("친구 요청을 찾을 수 없습니다: " + requestId);
    }

    public FriendRequestNotFoundException(String message) {
        super(message);
    }
}
