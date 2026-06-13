package com.cotalk.domain.exception;

/**
 * 차단 관계가 존재하여 상호작용이 거부될 때 발생하는 예외.
 * <p>
 * 두 사용자 사이에 (어느 방향이든) 차단 관계가 존재하면 메시지 전송, 친구 요청,
 * 1:1 채팅방 생성/재초대 등의 상호작용이 거부된다. HTTP 403 Forbidden과 매핑된다.
 * </p>
 *
 * @author seunggu.lee
 */
public class BlockedRelationshipException extends DomainException {

    /**
     * 메시지를 지정하여 예외를 생성한다.
     *
     * @param message 에러 메시지
     */
    public BlockedRelationshipException(String message) {
        super(message, "BLOCKED_RELATIONSHIP", HttpStatusHint.FORBIDDEN);
    }

    /**
     * 기본 메시지로 예외를 생성한다.
     */
    public BlockedRelationshipException() {
        this("차단 관계가 존재하여 요청을 처리할 수 없습니다.");
    }
}
