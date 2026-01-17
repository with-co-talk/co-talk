package com.cotalk.domain.exception;

/**
 * 친구를 찾을 수 없을 때 발생하는 예외.
 *
 * @author seunggu.lee
 */
public class FriendNotFoundException extends DomainException {

    public FriendNotFoundException() {
        super("친구를 찾을 수 없습니다.");
    }

    public FriendNotFoundException(String message) {
        super(message);
    }

    public FriendNotFoundException(Long friendId) {
        super("친구를 찾을 수 없습니다: " + friendId);
    }
}
