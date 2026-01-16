package com.cotalk.adapter.outbound.persistence;

import com.cotalk.domain.entity.Friend;
import com.cotalk.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendJpaRepository extends JpaRepository<Friend, Long> {
    Optional<Friend> findByUserIdAndFriendId(Long userId, Long friendId);
    List<Friend> findByUserIdAndStatus(Long userId, Friend.FriendStatus status);
    boolean existsByUserIdAndFriendId(Long userId, Long friendId);
    void deleteByUserIdOrFriendId(Long userId, Long friendId);

    /**
     * N+1 쿼리를 방지하기 위한 JOIN 쿼리
     * 사용자의 수락된 친구 목록을 User 엔티티와 함께 조회
     */
    @Query("SELECT u FROM User u " +
           "WHERE u.id IN (" +
           "  SELECT f.friendId FROM Friend f " +
           "  WHERE f.userId = :userId AND f.status = 'ACCEPTED'" +
           ")")
    List<User> findAcceptedFriendsWithUserData(@Param("userId") Long userId);
}
