package com.cotalk.domain.port.inbound;

public interface RemoveFriendUseCase {
    void removeFriend(Long userId, Long friendId);
}
