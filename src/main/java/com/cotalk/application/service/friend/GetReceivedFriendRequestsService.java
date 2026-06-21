package com.cotalk.application.service.friend;

import com.cotalk.domain.entity.FriendRequest;
import com.cotalk.domain.model.PageQuery;
import com.cotalk.domain.model.PageResult;
import com.cotalk.domain.port.inbound.friend.GetReceivedFriendRequestsUseCase;
import com.cotalk.domain.port.outbound.FriendRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 받은 친구 요청 목록 조회 유스케이스 구현체.
 * 사용자가 받은 대기 중인 친구 요청 목록을 조회한다.
 *
 * @author seunggu.lee
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetReceivedFriendRequestsService implements GetReceivedFriendRequestsUseCase {

    private final FriendRequestRepository friendRequestRepository;

    /**
     * 사용자가 받은 대기 중인 친구 요청 목록을 조회한다.
     *
     * @param receiverId 수신자 ID
     * @return 받은 친구 요청 목록
     */
    @Override
    public List<FriendRequest> getReceivedFriendRequests(Long receiverId) {
        return friendRequestRepository.findPendingByReceiverId(receiverId);
    }

    /**
     * 사용자가 받은 대기 중인 친구 요청 목록을 DB 레벨 페이지네이션으로 조회한다.
     *
     * @param receiverId 수신자 ID
     * @param query      페이지네이션 정보
     * @return 페이지네이션된 받은 친구 요청 목록
     */
    @Override
    public PageResult<FriendRequest> getReceivedFriendRequests(Long receiverId, PageQuery query) {
        return friendRequestRepository.findPendingByReceiverId(receiverId, query);
    }
}
