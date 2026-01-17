package com.cotalk.domain.exception;

/**
 * 채팅방에 대한 접근 권한이 없을 때 발생하는 예외.
 *
 * @author seunggu.lee
 */
public class ChatRoomAccessDeniedException extends DomainException {

    public ChatRoomAccessDeniedException(String message) {
        super(message);
    }

    public ChatRoomAccessDeniedException(Long chatRoomId, Long userId) {
        super(String.format("사용자 %d는 채팅방 %d에 접근할 수 없습니다.", userId, chatRoomId));
    }
}
