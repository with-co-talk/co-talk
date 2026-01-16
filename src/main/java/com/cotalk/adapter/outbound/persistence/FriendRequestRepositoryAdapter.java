package com.cotalk.adapter.outbound.persistence;

import com.cotalk.domain.entity.FriendRequest;
import com.cotalk.domain.port.outbound.FriendRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class FriendRequestRepositoryAdapter implements FriendRequestRepository {

    private final FriendRequestJpaRepository friendRequestJpaRepository;

    @Override
    public FriendRequest save(FriendRequest friendRequest) {
        return friendRequestJpaRepository.save(friendRequest);
    }

    @Override
    public Optional<FriendRequest> findById(Long id) {
        return friendRequestJpaRepository.findById(id);
    }

    @Override
    public List<FriendRequest> findPendingByReceiverId(Long receiverId) {
        return friendRequestJpaRepository.findByReceiverIdAndStatus(receiverId, FriendRequest.RequestStatus.PENDING);
    }

    @Override
    public boolean existsByRequesterIdAndReceiverId(Long requesterId, Long receiverId) {
        return friendRequestJpaRepository.existsByRequesterIdAndReceiverId(requesterId, receiverId);
    }

    @Override
    public void delete(FriendRequest friendRequest) {
        friendRequestJpaRepository.delete(friendRequest);
    }

    @Override
    public void deleteByUserId(Long userId) {
        friendRequestJpaRepository.deleteByRequesterIdOrReceiverId(userId, userId);
    }
}
