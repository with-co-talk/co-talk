package com.cotalk.domain.exception;

/**
 * 친구를 찾을 수 없을 때 발생하는 예외.
 *
 * @author seunggu.lee
 */
public class FriendNotFoundException extends DomainException {

    public FriendNotFoundException() {
        super("친구를 찾을 수 없습니다.", "FRIEND_NOT_FOUND", HttpStatusHint.NOT_FOUND);
    }

    public FriendNotFoundException(String message) {
        super(message, "FRIEND_NOT_FOUND", HttpStatusHint.NOT_FOUND);
    }

    public FriendNotFoundException(Long friendId) {
        super("친구를 찾을 수 없습니다: " + friendId, "FRIEND_NOT_FOUND", HttpStatusHint.NOT_FOUND);
    }
}
