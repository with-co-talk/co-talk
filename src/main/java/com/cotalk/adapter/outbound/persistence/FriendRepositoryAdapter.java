package com.cotalk.adapter.outbound.persistence;

import com.cotalk.domain.entity.Friend;
import com.cotalk.domain.port.outbound.FriendRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class FriendRepositoryAdapter implements FriendRepository {

    private final FriendJpaRepository friendJpaRepository;

    @Override
    public Friend save(Friend friend) {
        return friendJpaRepository.save(friend);
    }

    @Override
    public Optional<Friend> findById(Long id) {
        return friendJpaRepository.findById(id);
    }

    @Override
    public Optional<Friend> findByUserIdAndFriendId(Long userId, Long friendId) {
        return friendJpaRepository.findByUserIdAndFriendId(userId, friendId);
    }

    @Override
    public List<Friend> findAcceptedFriendsByUserId(Long userId) {
        return friendJpaRepository.findByUserIdAndStatus(userId, Friend.FriendStatus.ACCEPTED);
    }

    @Override
    public boolean existsByUserIdAndFriendId(Long userId, Long friendId) {
        return friendJpaRepository.existsByUserIdAndFriendId(userId, friendId);
    }

    @Override
    public void delete(Friend friend) {
        friendJpaRepository.delete(friend);
    }
}
