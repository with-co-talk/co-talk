package com.cotalk.domain.exception;



public class ChatRoomNotFoundException extends DomainException {

    public ChatRoomNotFoundException(String message) {
        super(message);
    }

    public ChatRoomNotFoundException(Long chatRoomId) {
        super("채팅방을 찾을 수 없습니다: " + chatRoomId);
    }
}
