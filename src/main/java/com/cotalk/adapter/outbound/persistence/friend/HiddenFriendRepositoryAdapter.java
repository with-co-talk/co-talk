package com.cotalk.adapter.outbound.persistence.friend;

import com.cotalk.adapter.outbound.persistence.entity.HiddenFriendJpaEntity;
import com.cotalk.adapter.outbound.persistence.mapper.HiddenFriendMapper;
import com.cotalk.domain.entity.HiddenFriend;
import com.cotalk.domain.port.outbound.HiddenFriendRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 친구 숨김 영속성 어댑터.
 * JPA 엔티티와 도메인 간 매핑을 수행하며, 도메인 포트를 구현한다.
 *
 * @author seunggu.lee
 */
@Repository
@RequiredArgsConstructor
public class HiddenFriendRepositoryAdapter implements HiddenFriendRepository {

    private final HiddenFriendJpaRepository hiddenFriendJpaRepository;
    private final HiddenFriendMapper mapper;

    /**
     * 친구 숨김 정보를 저장한다.
     *
     * @param hiddenFriend 저장할 친구 숨김 엔티티
     * @return 저장된 친구 숨김 엔티티
     */
    @Override
    public HiddenFriend save(HiddenFriend hiddenFriend) {
        HiddenFriendJpaEntity saved = hiddenFriendJpaRepository.save(mapper.toJpa(hiddenFriend));
        return mapper.toDomain(saved);
    }

    /**
     * ID로 친구 숨김 정보를 조회한다.
     *
     * @param id 친구 숨김 ID
     * @return 친구 숨김 정보 (Optional)
     */
    @Override
    public Optional<HiddenFriend> findById(Long id) {
        return hiddenFriendJpaRepository.findById(id).map(mapper::toDomain);
    }

    /**
     * 사용자 ID와 친구 ID로 친구 숨김 정보를 조회한다.
     *
     * @param userId 사용자 ID
     * @param friendId 친구 ID
     * @return 친구 숨김 정보 (Optional)
     */
    @Override
    public Optional<HiddenFriend> findByUserIdAndFriendId(Long userId, Long friendId) {
        return hiddenFriendJpaRepository.findByUserIdAndFriendId(userId, friendId).map(mapper::toDomain);
    }

    /**
     * 사용자 ID로 친구 숨김 목록을 조회한다.
     *
     * @param userId 사용자 ID
     * @return 친구 숨김 목록
     */
    @Override
    public List<HiddenFriend> findByUserId(Long userId) {
        return hiddenFriendJpaRepository.findByUserId(userId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    /**
     * 사용자 ID와 친구 ID로 친구 숨김 관계가 존재하는지 확인한다.
     *
     * @param userId 사용자 ID
     * @param friendId 친구 ID
     * @return 친구 숨김 관계 존재 여부
     */
    @Override
    public boolean existsByUserIdAndFriendId(Long userId, Long friendId) {
        return hiddenFriendJpaRepository.existsByUserIdAndFriendId(userId, friendId);
    }

    /**
     * 친구 숨김 정보를 삭제한다.
     *
     * @param hiddenFriend 삭제할 친구 숨김 엔티티
     */
    @Override
    public void delete(HiddenFriend hiddenFriend) {
        hiddenFriendJpaRepository.delete(mapper.toJpa(hiddenFriend));
    }

    /**
     * 사용자 ID로 모든 친구 숨김 정보를 삭제한다.
     * 회원 탈퇴 정합성을 위해 해당 사용자가 숨긴 레코드(user_id)뿐 아니라
     * 타인이 해당 사용자를 숨긴 레코드(friend_id)도 함께 삭제한다.
     *
     * @param userId 사용자 ID
     */
    @Override
    public void deleteByUserId(Long userId) {
        hiddenFriendJpaRepository.deleteByUserIdOrFriendId(userId, userId);
    }
}
