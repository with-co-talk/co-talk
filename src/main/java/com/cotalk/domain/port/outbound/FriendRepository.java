package com.cotalk.domain.port.outbound;

import com.cotalk.domain.entity.Friend;
import com.cotalk.domain.entity.User;

import java.util.List;
import java.util.Optional;


public interface FriendRepository {
    Friend save(Friend friend);
    Optional<Friend> findById(Long id);
    Optional<Friend> findByUserIdAndFriendId(Long userId, Long friendId);
    List<Friend> findAcceptedFriendsByUserId(Long userId);
    List<User> findAcceptedFriendsWithUserData(Long userId);
    boolean existsByUserIdAndFriendId(Long userId, Long friendId);
    void delete(Friend friend);
    void deleteByUserId(Long userId);
}
