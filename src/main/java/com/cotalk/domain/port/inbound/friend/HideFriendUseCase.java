package com.cotalk.domain.port.inbound.friend;

/**
 * 친구 숨김 유스케이스.
 * 특정 친구를 숨긴다.
 *
 * @author seunggu.lee
 */
public interface HideFriendUseCase {

    /**
     * 친구를 숨긴다.
     *
     * @param userId 숨김을 수행하는 사용자 ID
     * @param friendId 숨길 친구 ID
     */
    void hideFriend(Long userId, Long friendId);
}
