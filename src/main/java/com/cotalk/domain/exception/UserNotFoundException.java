package com.cotalk.domain.exception;

/**
 * 사용자를 찾을 수 없을 때 발생하는 예외.
 *
 * @author seunggu.lee
 */
public class UserNotFoundException extends DomainException {

    public UserNotFoundException(String message) {
        super(message);
    }

    public UserNotFoundException(Long userId) {
        super("사용자를 찾을 수 없습니다: " + userId);
    }
}
