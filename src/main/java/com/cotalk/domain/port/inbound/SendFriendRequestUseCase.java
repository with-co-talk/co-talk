package com.cotalk.domain.port.inbound;



public interface SendFriendRequestUseCase {
    Long sendFriendRequest(Long requesterId, Long receiverId);
}
