package com.cotalk.domain.port.inbound.friend;

import com.cotalk.domain.entity.FriendRequest;

import java.util.List;

/**
 * 받은 친구 요청 목록 조회 유스케이스.
 * 사용자가 받은 대기 중인 친구 요청 목록을 조회한다.
 *
 * @author seunggu.lee
 */
public interface GetReceivedFriendRequestsUseCase {

    /**
     * 사용자가 받은 대기 중인 친구 요청 목록을 조회한다.
     *
     * @param receiverId 수신자 ID
     * @return 받은 친구 요청 목록
     */
    List<FriendRequest> getReceivedFriendRequests(Long receiverId);
}
