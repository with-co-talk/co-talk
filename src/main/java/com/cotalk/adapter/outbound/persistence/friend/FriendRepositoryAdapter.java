package com.cotalk.adapter.outbound.persistence.friend;

import com.cotalk.adapter.outbound.persistence.mapper.UserMapper;
import com.cotalk.domain.entity.Friend;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.port.outbound.FriendRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 친구 영속성 어댑터.
 * JPA를 통해 친구 관계 데이터를 저장하고 조회한다.
 *
 * @author seunggu.lee
 */
@Repository
@RequiredArgsConstructor
public class FriendRepositoryAdapter implements FriendRepository {

    private final FriendJpaRepository friendJpaRepository;
    private final UserMapper userMapper;

    /**
     * 친구 관계를 저장한다.
     *
     * @param friend 저장할 친구 엔티티
     * @return 저장된 친구 엔티티
     */
    @Override
    public Friend save(Friend friend) {
        return friendJpaRepository.save(friend);
    }

    /**
     * ID로 친구 관계를 조회한다.
     *
     * @param id 친구 관계 ID
     * @return 친구 관계 (Optional)
     */
    @Override
    public Optional<Friend> findById(Long id) {
        return friendJpaRepository.findById(id);
    }

    /**
     * 사용자 ID와 친구 ID로 친구 관계를 조회한다.
     *
     * @param userId 사용자 ID
     * @param friendId 친구 ID
     * @return 친구 관계 (Optional)
     */
    @Override
    public Optional<Friend> findByUserIdAndFriendId(Long userId, Long friendId) {
        return friendJpaRepository.findByUserIdAndFriendId(userId, friendId);
    }

    /**
     * 사용자 ID로 수락된 친구 목록을 조회한다.
     *
     * @param userId 사용자 ID
     * @return 수락된 친구 목록
     */
    @Override
    public List<Friend> findAcceptedFriendsByUserId(Long userId) {
        return friendJpaRepository.findByUserIdAndStatus(userId, Friend.FriendStatus.ACCEPTED);
    }

    /**
     * 사용자 ID와 친구 ID로 친구 관계가 존재하는지 확인한다.
     *
     * @param userId 사용자 ID
     * @param friendId 친구 ID
     * @return 친구 관계 존재 여부
     */
    @Override
    public boolean existsByUserIdAndFriendId(Long userId, Long friendId) {
        return friendJpaRepository.existsByUserIdAndFriendId(userId, friendId);
    }

    /**
     * 친구 관계를 삭제한다.
     *
     * @param friend 삭제할 친구 엔티티
     */
    @Override
    public void delete(Friend friend) {
        friendJpaRepository.delete(friend);
    }

    /**
     * 사용자의 수락된 친구 목록을 User 엔티티와 함께 조회한다.
     * N+1 쿼리를 방지하기 위한 최적화된 조회 메서드이다.
     *
     * @param userId 사용자 ID
     * @return 친구 User 목록
     */
    @Override
    public List<User> findAcceptedFriendsWithUserData(Long userId) {
        return friendJpaRepository.findAcceptedFriendsWithUserData(userId).stream()
                .map(userMapper::toDomain)
                .toList();
    }

    /**
     * 사용자 ID로 모든 친구 관계를 삭제한다.
     *
     * @param userId 사용자 ID
     */
    @Override
    public void deleteByUserId(Long userId) {
        friendJpaRepository.deleteByUserIdOrFriendId(userId, userId);
    }
}
