package com.cotalk.domain.exception;

/**
 * 유효하지 않은 채팅방 요청일 때 발생하는 예외.
 *
 * @author seunggu.lee
 */
public class InvalidChatRoomException extends DomainException {

    /**
     * 메시지를 지정하여 예외를 생성한다.
     *
     * @param message 에러 메시지
     */
    public InvalidChatRoomException(String message) {
        super(message, "INVALID_CHAT_ROOM", HttpStatusHint.BAD_REQUEST);
    }

    /**
     * 유효하지 않은 그룹 채팅 요청 예외를 생성한다.
     *
     * @param message 에러 메시지
     * @return 그룹 채팅 유효성 위반 예외
     */
    public static InvalidChatRoomException invalidGroupChat(String message) {
        return new InvalidChatRoomException(message);
    }

    /**
     * 유효하지 않은 1:1 채팅 요청 예외를 생성한다.
     *
     * @param message 에러 메시지
     * @return 1:1 채팅 유효성 위반 예외
     */
    public static InvalidChatRoomException invalidDirectChat(String message) {
        return new InvalidChatRoomException(message);
    }
}
