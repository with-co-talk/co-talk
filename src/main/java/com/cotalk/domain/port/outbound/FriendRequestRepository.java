package com.cotalk.domain.port.outbound;

import com.cotalk.domain.entity.FriendRequest;

import java.util.List;
import java.util.Optional;


public interface FriendRequestRepository {
    FriendRequest save(FriendRequest friendRequest);
    Optional<FriendRequest> findById(Long id);
    List<FriendRequest> findPendingByReceiverId(Long receiverId);
    boolean existsByRequesterIdAndReceiverId(Long requesterId, Long receiverId);
    void delete(FriendRequest friendRequest);
    void deleteByUserId(Long userId);
}
