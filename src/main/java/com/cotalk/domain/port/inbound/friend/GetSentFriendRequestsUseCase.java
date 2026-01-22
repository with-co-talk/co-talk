package com.cotalk.domain.port.inbound.friend;

import com.cotalk.domain.entity.FriendRequest;

import java.util.List;

/**
 * 보낸 친구 요청 목록 조회 유스케이스.
 * 사용자가 보낸 대기 중인 친구 요청 목록을 조회한다.
 *
 * @author seunggu.lee
 */
public interface GetSentFriendRequestsUseCase {

    /**
     * 사용자가 보낸 대기 중인 친구 요청 목록을 조회한다.
     *
     * @param requesterId 요청자 ID
     * @return 보낸 친구 요청 목록
     */
    List<FriendRequest> getSentFriendRequests(Long requesterId);
}
