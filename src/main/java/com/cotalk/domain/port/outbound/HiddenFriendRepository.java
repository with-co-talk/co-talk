package com.cotalk.domain.port.outbound;

import com.cotalk.domain.entity.HiddenFriend;

import java.util.List;
import java.util.Optional;

/**
 * 친구 숨김 레포지토리 포트.
 * 친구 숨김 데이터 저장 및 조회를 위한 인터페이스를 정의한다.
 *
 * @author seunggu.lee
 */
public interface HiddenFriendRepository {

    /**
     * 친구 숨김 정보를 저장한다.
     *
     * @param hiddenFriend 저장할 친구 숨김 정보
     * @return 저장된 친구 숨김 정보
     */
    HiddenFriend save(HiddenFriend hiddenFriend);

    /**
     * ID로 친구 숨김 정보를 조회한다.
     *
     * @param id 친구 숨김 ID
     * @return 조회된 친구 숨김 정보 (Optional)
     */
    Optional<HiddenFriend> findById(Long id);

    /**
     * 사용자 ID와 친구 ID로 친구 숨김 정보를 조회한다.
     *
     * @param userId 사용자 ID
     * @param friendId 친구 ID
     * @return 조회된 친구 숨김 정보 (Optional)
     */
    Optional<HiddenFriend> findByUserIdAndFriendId(Long userId, Long friendId);

    /**
     * 특정 사용자가 숨긴 모든 친구 숨김 정보를 조회한다.
     *
     * @param userId 사용자 ID
     * @return 친구 숨김 정보 목록
     */
    List<HiddenFriend> findByUserId(Long userId);

    /**
     * 친구 숨김 관계 존재 여부를 확인한다.
     *
     * @param userId 사용자 ID
     * @param friendId 친구 ID
     * @return 존재 여부
     */
    boolean existsByUserIdAndFriendId(Long userId, Long friendId);

    /**
     * 친구 숨김 정보를 삭제한다.
     *
     * @param hiddenFriend 삭제할 친구 숨김 정보
     */
    void delete(HiddenFriend hiddenFriend);
}
