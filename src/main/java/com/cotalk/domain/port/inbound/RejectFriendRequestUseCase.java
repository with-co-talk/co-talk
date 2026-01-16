package com.cotalk.domain.port.inbound;

public interface RejectFriendRequestUseCase {
    void rejectFriendRequest(Long userId, Long requestId);
}
