package com.cotalk.domain.exception;

/**
 * 유효하지 않은 채팅방 요청일 때 발생하는 예외.
 *
 * @author seunggu.lee
 */
public class InvalidChatRoomException extends DomainException {

    public InvalidChatRoomException(String message) {
        super(message);
    }
}
