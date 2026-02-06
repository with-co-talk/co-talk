package com.cotalk.adapter.outbound.persistence.friend;

import com.cotalk.domain.entity.HiddenFriend;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 친구 숨김 JPA 리포지토리.
 * Spring Data JPA를 통해 친구 숨김 데이터에 접근한다.
 *
 * @author seunggu.lee
 */
public interface HiddenFriendJpaRepository extends JpaRepository<HiddenFriend, Long> {

    /**
     * 사용자 ID와 친구 ID로 친구 숨김 정보를 조회한다.
     *
     * @param userId 사용자 ID
     * @param friendId 친구 ID
     * @return 친구 숨김 정보 (Optional)
     */
    Optional<HiddenFriend> findByUserIdAndFriendId(Long userId, Long friendId);

    /**
     * 사용자 ID로 친구 숨김 목록을 조회한다.
     *
     * @param userId 사용자 ID
     * @return 친구 숨김 목록
     */
    List<HiddenFriend> findByUserId(Long userId);

    /**
     * 사용자 ID와 친구 ID로 친구 숨김 관계가 존재하는지 확인한다.
     *
     * @param userId 사용자 ID
     * @param friendId 친구 ID
     * @return 친구 숨김 관계 존재 여부
     */
    boolean existsByUserIdAndFriendId(Long userId, Long friendId);

    /**
     * 사용자 ID로 모든 친구 숨김 정보를 삭제한다.
     *
     * @param userId 사용자 ID
     */
    void deleteByUserId(Long userId);
}
