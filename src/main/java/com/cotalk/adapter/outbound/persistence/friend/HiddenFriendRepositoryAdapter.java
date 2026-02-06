package com.cotalk.adapter.outbound.persistence.friend;

import com.cotalk.domain.entity.HiddenFriend;
import com.cotalk.domain.port.outbound.HiddenFriendRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 친구 숨김 영속성 어댑터.
 * JPA를 통해 친구 숨김 데이터를 저장하고 조회한다.
 *
 * @author seunggu.lee
 */
@Repository
@RequiredArgsConstructor
public class HiddenFriendRepositoryAdapter implements HiddenFriendRepository {

    private final HiddenFriendJpaRepository hiddenFriendJpaRepository;

    /**
     * 친구 숨김 정보를 저장한다.
     *
     * @param hiddenFriend 저장할 친구 숨김 엔티티
     * @return 저장된 친구 숨김 엔티티
     */
    @Override
    public HiddenFriend save(HiddenFriend hiddenFriend) {
        return hiddenFriendJpaRepository.save(hiddenFriend);
    }

    /**
     * ID로 친구 숨김 정보를 조회한다.
     *
     * @param id 친구 숨김 ID
     * @return 친구 숨김 정보 (Optional)
     */
    @Override
    public Optional<HiddenFriend> findById(Long id) {
        return hiddenFriendJpaRepository.findById(id);
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
        return hiddenFriendJpaRepository.findByUserIdAndFriendId(userId, friendId);
    }

    /**
     * 사용자 ID로 친구 숨김 목록을 조회한다.
     *
     * @param userId 사용자 ID
     * @return 친구 숨김 목록
     */
    @Override
    public List<HiddenFriend> findByUserId(Long userId) {
        return hiddenFriendJpaRepository.findByUserId(userId);
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
        hiddenFriendJpaRepository.delete(hiddenFriend);
    }

    /**
     * 사용자 ID로 모든 친구 숨김 정보를 삭제한다.
     *
     * @param userId 사용자 ID
     */
    @Override
    public void deleteByUserId(Long userId) {
        hiddenFriendJpaRepository.deleteByUserId(userId);
    }
}
