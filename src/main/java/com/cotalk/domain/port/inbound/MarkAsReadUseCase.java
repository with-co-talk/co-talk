package com.cotalk.domain.port.inbound;

public interface MarkAsReadUseCase {
    void markAsRead(Long userId, Long chatRoomId);
}
