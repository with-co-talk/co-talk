package com.cotalk.domain.exception;

/**
 * 유효하지 않은 그룹 채팅 요청일 때 발생하는 예외.
 *
 * @author seunggu.lee
 */
public class InvalidGroupChatException extends DomainException {

    public InvalidGroupChatException(String message) {
        super(message);
    }
}
