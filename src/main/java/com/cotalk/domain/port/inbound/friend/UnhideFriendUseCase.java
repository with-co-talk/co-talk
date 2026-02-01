package com.cotalk.domain.port.inbound.friend;

/**
 * 친구 숨김 해제 유스케이스.
 * 기존에 숨긴 친구의 숨김을 해제한다.
 *
 * @author seunggu.lee
 */
public interface UnhideFriendUseCase {

    /**
     * 친구 숨김을 해제한다.
     *
     * @param userId 숨김 해제를 수행하는 사용자 ID
     * @param friendId 숨김 해제할 친구 ID
     */
    void unhideFriend(Long userId, Long friendId);
}
