package com.cotalk.adapter.outbound.persistence.friend;

import com.cotalk.adapter.outbound.persistence.entity.FriendJpaEntity;
import com.cotalk.adapter.outbound.persistence.mapper.FriendMapper;
import com.cotalk.adapter.outbound.persistence.mapper.UserMapper;
import com.cotalk.domain.entity.Friend;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.model.PageQuery;
import com.cotalk.domain.model.PageResult;
import com.cotalk.domain.port.outbound.FriendRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 친구 영속성 어댑터.
 * JPA 엔티티와 도메인 간 매핑을 수행하며, 도메인 포트를 구현한다.
 *
 * @author seunggu.lee
 */
@Repository
@RequiredArgsConstructor
public class FriendRepositoryAdapter implements FriendRepository {

    private final FriendJpaRepository friendJpaRepository;
    private final FriendMapper friendMapper;
    private final UserMapper userMapper;

    /**
     * 친구 관계를 저장한다.
     *
     * @param friend 저장할 친구 엔티티
     * @return 저장된 친구 엔티티
     */
    @Override
    public Friend save(Friend friend) {
        FriendJpaEntity saved = friendJpaRepository.save(friendMapper.toJpa(friend));
        return friendMapper.toDomain(saved);
    }

    /**
     * ID로 친구 관계를 조회한다.
     *
     * @param id 친구 관계 ID
     * @return 친구 관계 (Optional)
     */
    @Override
    public Optional<Friend> findById(Long id) {
        return friendJpaRepository.findById(id).map(friendMapper::toDomain);
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
        return friendJpaRepository.findByUserIdAndFriendId(userId, friendId).map(friendMapper::toDomain);
    }

    /**
     * 사용자 ID로 수락된 친구 목록을 조회한다.
     *
     * @param userId 사용자 ID
     * @return 수락된 친구 목록
     */
    @Override
    public List<Friend> findAcceptedFriendsByUserId(Long userId) {
        return friendJpaRepository.findByUserIdAndStatus(userId, Friend.FriendStatus.ACCEPTED).stream()
                .map(friendMapper::toDomain)
                .toList();
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
        friendJpaRepository.delete(friendMapper.toJpa(friend));
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
     * 사용자의 수락된 친구 목록을 DB 레벨 페이지네이션으로 조회한다.
     * N+1 쿼리를 방지하기 위한 최적화된 조회 메서드이다.
     *
     * @param userId 사용자 ID
     * @param query  페이지네이션 정보
     * @return 페이지네이션된 친구 User 목록
     */
    @Override
    public PageResult<User> findAcceptedFriendsWithUserData(Long userId, PageQuery query) {
        Pageable pageable = PageRequest.of(query.page(), query.size());
        Page<User> page = friendJpaRepository.findAcceptedFriendsWithUserData(userId, pageable)
                .map(userMapper::toDomain);
        return new PageResult<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
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
