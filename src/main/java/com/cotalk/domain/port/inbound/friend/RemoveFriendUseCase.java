package com.cotalk.domain.port.inbound.friend;

/**
 * 친구 삭제 유스케이스.
 * 친구 관계를 삭제한다.
 *
 * @author seunggu.lee
 */
public interface RemoveFriendUseCase {

    /**
     * 친구 관계를 삭제한다.
     *
     * @param userId 사용자 ID
     * @param friendId 삭제할 친구 ID
     */
    void removeFriend(Long userId, Long friendId);
}
