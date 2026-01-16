package com.cotalk.domain.exception;

/**
 * 메시지 반응을 찾을 수 없을 때 발생하는 예외
 */
public class MessageReactionNotFoundException extends DomainException {

    public MessageReactionNotFoundException(Long messageId, Long userId, String emoji) {
        super("메시지 반응을 찾을 수 없습니다: messageId=" + messageId + ", userId=" + userId + ", emoji=" + emoji);
    }
}
