package com.cotalk.domain.exception;

/**
 * 친구 요청에 대한 권한이 없을 때 발생하는 예외
 */
public class FriendRequestAccessDeniedException extends DomainException {

    public FriendRequestAccessDeniedException(String message) {
        super(message);
    }

    public static FriendRequestAccessDeniedException notReceiver() {
        return new FriendRequestAccessDeniedException("해당 요청을 거절할 권한이 없습니다.");
    }

    public static FriendRequestAccessDeniedException notRequester() {
        return new FriendRequestAccessDeniedException("해당 요청을 취소할 권한이 없습니다.");
    }
}
