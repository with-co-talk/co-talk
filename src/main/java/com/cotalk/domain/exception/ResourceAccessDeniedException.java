package com.cotalk.domain.exception;

/**
 * 인증된 사용자가 자신의 리소스가 아닌 다른 사용자의 리소스에 접근할 때 발생하는 예외.
 * HTTP 403 Forbidden 상태 코드와 매핑된다.
 *
 * @author seunggu.lee
 */
public class ResourceAccessDeniedException extends DomainException {

    /**
     * 메시지를 지정하여 예외를 생성한다.
     *
     * @param message 에러 메시지
     */
    public ResourceAccessDeniedException(String message) {
        super(message);
    }

    /**
     * 기본 메시지로 예외를 생성한다.
     */
    public ResourceAccessDeniedException() {
        super("자신의 리소스만 접근할 수 있습니다.");
    }
}
