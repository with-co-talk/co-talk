package com.cotalk.domain.exception;

/**
 * 메시지 접근 권한이 없을 때 발생하는 예외.
 *
 * @author seunggu.lee
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

    /**
     * 수정/삭제 가능 시간이 초과된 경우 예외를 생성한다.
     *
     * @return 시간 초과 예외
     */
    public static MessageAccessDeniedException timeExpired() {
        return new MessageAccessDeniedException("메시지 작성 후 5분이 지나 수정/삭제할 수 없습니다.");
    }
}
