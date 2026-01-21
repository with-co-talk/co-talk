package com.cotalk.application.service.friend;

import com.cotalk.domain.entity.FriendRequest;
import com.cotalk.domain.port.inbound.friend.GetSentFriendRequestsUseCase;
import com.cotalk.domain.port.outbound.FriendRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 보낸 친구 요청 목록 조회 유스케이스 구현체.
 * 사용자가 보낸 대기 중인 친구 요청 목록을 조회한다.
 *
 * @author seunggu.lee
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetSentFriendRequestsService implements GetSentFriendRequestsUseCase {

    private final FriendRequestRepository friendRequestRepository;

    /**
     * 사용자가 보낸 대기 중인 친구 요청 목록을 조회한다.
     *
     * @param requesterId 요청자 ID
     * @return 보낸 친구 요청 목록
     */
    @Override
    public List<FriendRequest> getSentFriendRequests(Long requesterId) {
        return friendRequestRepository.findPendingByRequesterId(requesterId);
    }
}
