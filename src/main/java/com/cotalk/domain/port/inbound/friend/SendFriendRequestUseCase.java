package com.cotalk.domain.port.inbound.friend;

/**
 * 친구 요청 전송 유스케이스.
 * 다른 사용자에게 친구 요청을 전송한다.
 *
 * @author seunggu.lee
 */
public interface SendFriendRequestUseCase {

    /**
     * 친구 요청을 전송한다.
     *
     * @param requesterId 요청자 ID
     * @param receiverId 수신자 ID
     * @return 생성된 친구 요청 ID
     */
    Long sendFriendRequest(Long requesterId, Long receiverId);
}
