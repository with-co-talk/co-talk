package com.cotalk.domain.exception;

public class ChatRoomAccessDeniedException extends DomainException {

    public ChatRoomAccessDeniedException(String message) {
        super(message);
    }

    public ChatRoomAccessDeniedException(Long chatRoomId, Long userId) {
        super(String.format("사용자 %d는 채팅방 %d에 접근할 수 없습니다.", userId, chatRoomId));
    }
}
