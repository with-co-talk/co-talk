package com.cotalk.domain.exception;

/**
 * 채팅방을 찾을 수 없을 때 발생하는 예외.
 *
 * @author seunggu.lee
 */
public class ChatRoomNotFoundException extends DomainException {

    public ChatRoomNotFoundException(String message) {
        super(message, "CHAT_ROOM_NOT_FOUND", HttpStatusHint.NOT_FOUND);
    }

    public ChatRoomNotFoundException(Long chatRoomId) {
        super("채팅방을 찾을 수 없습니다: " + chatRoomId, "CHAT_ROOM_NOT_FOUND", HttpStatusHint.NOT_FOUND);
    }
}
