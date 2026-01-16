package com.cotalk.domain.port.inbound;



public interface AcceptFriendRequestUseCase {
    Long acceptFriendRequest(Long receiverId, Long requestId);
}
