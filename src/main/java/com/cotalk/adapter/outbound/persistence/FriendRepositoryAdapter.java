package com.cotalk.adapter.outbound.persistence;

import com.cotalk.domain.entity.Friend;
import com.cotalk.domain.port.outbound.persistence.FriendRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 친구 관계 Repository 어댑터.
 * 
 * <p>도메인 Repository 인터페이스를 JPA Repository로 구현합니다.
 *
 * @author Co-Talk Team
 * @since 1.0
 */
@Component
@RequiredArgsConstructor
public class FriendRepositoryAdapter implements FriendRepository {

    private final FriendJpaRepository friendJpaRepository;

    @Override
    public Friend save(Friend friend) {
        return friendJpaRepository.save(friend);
    }

    @Override
    public Optional<Friend> findById(UUID id) {
        return friendJpaRepository.findById(id);
    }

    @Override
    public Optional<Friend> findByUserIdAndFriendId(UUID userId, UUID friendId) {
        return friendJpaRepository.findByUserIdAndFriendId(userId, friendId);
    }

    @Override
    public List<Friend> findByUserId(UUID userId) {
        return friendJpaRepository.findByUserId(userId);
    }

    @Override
    public List<Friend> findAcceptedFriendsByUserId(UUID userId) {
        return friendJpaRepository.findAcceptedFriendsByUserId(userId);
    }

    @Override
    public boolean existsByUserIdAndFriendId(UUID userId, UUID friendId) {
        return friendJpaRepository.existsByUserIdAndFriendId(userId, friendId);
    }

    @Override
    public void delete(Friend friend) {
        friendJpaRepository.delete(friend);
    }
}
