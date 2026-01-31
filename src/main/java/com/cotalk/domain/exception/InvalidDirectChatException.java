package com.cotalk.domain.exception;

/**
 * 유효하지 않은 1:1 채팅 요청일 때 발생하는 예외.
 *
 * @author seunggu.lee
 */
public class InvalidDirectChatException extends DomainException {

    public InvalidDirectChatException(String message) {
        super(message);
    }
}
