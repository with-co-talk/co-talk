package com.cotalk.domain.exception;

/**
 * 메시지 접근 권한이 없을 때 발생하는 예외
 */
public class MessageAccessDeniedException extends DomainException {

    public MessageAccessDeniedException(String message) {
        super(message);
    }

    public static MessageAccessDeniedException notSender() {
        return new MessageAccessDeniedException("본인이 보낸 메시지만 수정/삭제할 수 있습니다.");
    }

    public static MessageAccessDeniedException alreadyDeleted() {
        return new MessageAccessDeniedException("이미 삭제된 메시지입니다.");
    }
}
