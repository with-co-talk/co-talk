package com.cotalk.domain.exception;



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
