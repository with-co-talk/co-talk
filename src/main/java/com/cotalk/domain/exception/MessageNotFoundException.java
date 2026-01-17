package com.cotalk.domain.exception;

/**
 * 메시지를 찾을 수 없을 때 발생하는 예외.
 *
 * @author seunggu.lee
 */
public class MessageNotFoundException extends DomainException {

    public MessageNotFoundException(Long messageId) {
        super("메시지를 찾을 수 없습니다: " + messageId);
    }

    public MessageNotFoundException(String message) {
        super(message);
    }
}
