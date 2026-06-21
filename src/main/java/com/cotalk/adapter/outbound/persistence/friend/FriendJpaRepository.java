package com.cotalk.adapter.outbound.persistence.friend;

import com.cotalk.adapter.outbound.persistence.entity.FriendJpaEntity;
import com.cotalk.adapter.outbound.persistence.entity.UserJpaEntity;
import com.cotalk.domain.entity.Friend;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 친구 JPA 리포지토리.
 * persistence 계층 전용이며, 도메인 반환은 Adapter에서 매핑한다.
 *
 * @author seunggu.lee
 */
public interface FriendJpaRepository extends JpaRepository<FriendJpaEntity, Long> {

    /**
     * 사용자 ID와 친구 ID로 친구 관계를 조회한다.
     *
     * @param userId 사용자 ID
     * @param friendId 친구 ID
     * @return 친구 관계 (Optional)
     */
    Optional<FriendJpaEntity> findByUserIdAndFriendId(Long userId, Long friendId);

    /**
     * 사용자 ID와 상태로 친구 목록을 조회한다.
     *
     * @param userId 사용자 ID
     * @param status 친구 상태
     * @return 친구 목록
     */
    List<FriendJpaEntity> findByUserIdAndStatus(Long userId, Friend.FriendStatus status);

    /**
     * 사용자 ID와 친구 ID로 친구 관계가 존재하는지 확인한다.
     *
     * @param userId 사용자 ID
     * @param friendId 친구 ID
     * @return 친구 관계 존재 여부
     */
    boolean existsByUserIdAndFriendId(Long userId, Long friendId);

    /**
     * 사용자 ID 또는 친구 ID로 친구 관계를 삭제한다.
     *
     * @param userId 사용자 ID
     * @param friendId 친구 ID
     */
    void deleteByUserIdOrFriendId(Long userId, Long friendId);

    /**
     * N+1 쿼리를 방지하기 위한 JOIN 쿼리.
     * 사용자의 수락된 친구 목록을 User 엔티티와 함께 조회한다.
     *
     * @param userId 사용자 ID
     * @return 친구 User 목록
     */
    @Query("SELECT u FROM UserJpaEntity u " +
           "WHERE u.id IN (" +
           "  SELECT f.friendId FROM FriendJpaEntity f " +
           "  WHERE f.userId = :userId AND f.status = 'ACCEPTED'" +
           ") AND u.id NOT IN (" +
           "  SELECT h.friendId FROM HiddenFriendJpaEntity h " +
           "  WHERE h.userId = :userId" +
           ")")
    List<UserJpaEntity> findAcceptedFriendsWithUserData(@Param("userId") Long userId);

    /**
     * N+1 쿼리를 방지하기 위한 JOIN 쿼리 (페이지네이션).
     * 사용자의 수락된 친구 목록을 User 엔티티와 함께 페이지네이션하여 조회한다.
     *
     * @param userId   사용자 ID
     * @param pageable 페이지네이션 정보
     * @return 페이지네이션된 친구 User 목록
     */
    @Query("SELECT u FROM UserJpaEntity u " +
           "WHERE u.id IN (" +
           "  SELECT f.friendId FROM FriendJpaEntity f " +
           "  WHERE f.userId = :userId AND f.status = 'ACCEPTED'" +
           ") AND u.id NOT IN (" +
           "  SELECT h.friendId FROM HiddenFriendJpaEntity h " +
           "  WHERE h.userId = :userId" +
           ")")
    Page<UserJpaEntity> findAcceptedFriendsWithUserData(@Param("userId") Long userId, Pageable pageable);
}
