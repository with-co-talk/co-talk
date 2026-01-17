package com.cotalk.domain.port.inbound.friend;

/**
 * 친구 요청 수락 유스케이스.
 * 받은 친구 요청을 수락한다.
 *
 * @author seunggu.lee
 */
public interface AcceptFriendRequestUseCase {

    /**
     * 친구 요청을 수락한다.
     *
     * @param receiverId 수신자 ID
     * @param requestId 친구 요청 ID
     * @return 생성된 친구 관계 ID
     */
    Long acceptFriendRequest(Long receiverId, Long requestId);
}
