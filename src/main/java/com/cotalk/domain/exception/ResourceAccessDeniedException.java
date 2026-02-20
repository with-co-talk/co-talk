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
        super(message, "ACCESS_DENIED", HttpStatusHint.FORBIDDEN);
    }

    /**
     * 기본 메시지로 예외를 생성한다.
     */
    public ResourceAccessDeniedException() {
        super("자신의 리소스만 접근할 수 있습니다.", "ACCESS_DENIED", HttpStatusHint.FORBIDDEN);
    }

    /**
     * 본인이 보낸 메시지가 아닌 경우 예외를 생성한다.
     *
     * @return 메시지 발신자 불일치 예외
     */
    public static ResourceAccessDeniedException messageNotSender() {
        return new ResourceAccessDeniedException("본인이 보낸 메시지만 수정/삭제할 수 있습니다.");
    }

    /**
     * 이미 삭제된 메시지에 접근하는 경우 예외를 생성한다.
     *
     * @return 이미 삭제된 메시지 예외
     */
    public static ResourceAccessDeniedException messageAlreadyDeleted() {
        return new ResourceAccessDeniedException("이미 삭제된 메시지입니다.");
    }

    /**
     * 메시지 수정/삭제 가능 시간이 초과된 경우 예외를 생성한다.
     *
     * @return 시간 초과 예외
     */
    public static ResourceAccessDeniedException messageTimeExpired() {
        return new ResourceAccessDeniedException("메시지 작성 후 5분이 지나 수정/삭제할 수 없습니다.");
    }

    /**
     * 친구 요청의 수신자가 아닌 경우 예외를 생성한다.
     *
     * @return 친구 요청 수신자 불일치 예외
     */
    public static ResourceAccessDeniedException friendRequestNotReceiver() {
        return new ResourceAccessDeniedException("해당 요청을 거절할 권한이 없습니다.");
    }

    /**
     * 친구 요청의 발신자가 아닌 경우 예외를 생성한다.
     *
     * @return 친구 요청 발신자 불일치 예외
     */
    public static ResourceAccessDeniedException friendRequestNotRequester() {
        return new ResourceAccessDeniedException("해당 요청을 취소할 권한이 없습니다.");
    }
}
