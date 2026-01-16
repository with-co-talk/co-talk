package com.cotalk.adapter.outbound.persistence;

import com.cotalk.domain.entity.FriendRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FriendRequestJpaRepository extends JpaRepository<FriendRequest, Long> {
    List<FriendRequest> findByReceiverIdAndStatus(Long receiverId, FriendRequest.RequestStatus status);
    boolean existsByRequesterIdAndReceiverId(Long requesterId, Long receiverId);
    void deleteByRequesterIdOrReceiverId(Long requesterId, Long receiverId);
}
