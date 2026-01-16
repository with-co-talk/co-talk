package com.cotalk.adapter.outbound.persistence;

import com.cotalk.domain.entity.Friend;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FriendJpaRepository extends JpaRepository<Friend, Long> {
    Optional<Friend> findByUserIdAndFriendId(Long userId, Long friendId);
    List<Friend> findByUserIdAndStatus(Long userId, Friend.FriendStatus status);
    boolean existsByUserIdAndFriendId(Long userId, Long friendId);
}
