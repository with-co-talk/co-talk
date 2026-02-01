package com.cotalk.domain.exception;

/**
 * 숨긴 친구 관계를 찾을 수 없을 때 발생하는 예외.
 *
 * @author seunggu.lee
 */
public class HiddenFriendNotFoundException extends DomainException {

    public HiddenFriendNotFoundException(String message) {
        super(message);
    }
}
