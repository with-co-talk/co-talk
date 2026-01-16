package com.cotalk.domain.port.inbound;



public interface CreateChatRoomUseCase {
    Long createChatRoom(Long userId1, Long userId2);
}
