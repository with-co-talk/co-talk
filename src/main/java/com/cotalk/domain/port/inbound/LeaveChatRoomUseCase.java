package com.cotalk.domain.port.inbound;

public interface LeaveChatRoomUseCase {
    void leaveChatRoom(Long userId, Long chatRoomId);
}
