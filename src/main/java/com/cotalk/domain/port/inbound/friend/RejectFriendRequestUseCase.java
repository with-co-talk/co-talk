package com.cotalk.domain.port.inbound.friend;

/**
 * 친구 요청 거절 유스케이스.
 * 받은 친구 요청을 거절한다.
 *
 * @author seunggu.lee
 */
public interface RejectFriendRequestUseCase {

    /**
     * 친구 요청을 거절한다.
     *
     * @param userId 사용자 ID
     * @param requestId 친구 요청 ID
     */
    void rejectFriendRequest(Long userId, Long requestId);
}
