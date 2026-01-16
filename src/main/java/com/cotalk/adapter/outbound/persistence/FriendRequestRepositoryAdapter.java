package com.cotalk.adapter.outbound.persistence;

import com.cotalk.domain.entity.FriendRequest;
import com.cotalk.domain.port.outbound.persistence.FriendRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 친구 요청 Repository 어댑터.
 * 
 * <p>도메인 Repository 인터페이스를 JPA Repository로 구현합니다.
 *
 * @author Co-Talk Team
 * @since 1.0
 */
@Component
@RequiredArgsConstructor
public class FriendRequestRepositoryAdapter implements FriendRequestRepository {

    private final FriendRequestJpaRepository friendRequestJpaRepository;

    @Override
    public FriendRequest save(FriendRequest friendRequest) {
        return friendRequestJpaRepository.save(friendRequest);
    }

    @Override
    public Optional<FriendRequest> findById(UUID id) {
        return friendRequestJpaRepository.findById(id);
    }

    @Override
    public Optional<FriendRequest> findByRequesterIdAndReceiverId(UUID requesterId, UUID receiverId) {
        return friendRequestJpaRepository.findByRequesterIdAndReceiverId(requesterId, receiverId);
    }

    @Override
    public List<FriendRequest> findByReceiverId(UUID receiverId) {
        return friendRequestJpaRepository.findByReceiverId(receiverId);
    }

    @Override
    public List<FriendRequest> findPendingByReceiverId(UUID receiverId) {
        return friendRequestJpaRepository.findPendingByReceiverId(receiverId);
    }

    @Override
    public boolean existsByRequesterIdAndReceiverId(UUID requesterId, UUID receiverId) {
        return friendRequestJpaRepository.existsByRequesterIdAndReceiverId(requesterId, receiverId);
    }

    @Override
    public void delete(FriendRequest friendRequest) {
        friendRequestJpaRepository.delete(friendRequest);
    }
}
