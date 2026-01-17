package com.cotalk.application.service.friend;

import com.cotalk.domain.entity.FriendRequest;
import com.cotalk.domain.exception.DomainException;
import com.cotalk.domain.port.inbound.friend.RejectFriendRequestUseCase;
import com.cotalk.domain.port.outbound.FriendRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 친구 요청 거절 유스케이스 구현체.
 * 받은 친구 요청을 거절 처리한다.
 *
 * @author seunggu.lee
 */
@Service
@RequiredArgsConstructor
@Transactional
public class RejectFriendRequestService implements RejectFriendRequestUseCase {

    private final FriendRequestRepository friendRequestRepository;

    /**
     * 친구 요청을 거절한다.
     * 본인이 받은 요청인 경우에만 거절 처리한다.
     *
     * @param userId    요청을 거절하는 사용자 ID
     * @param requestId 친구 요청 ID
     * @throws DomainException 친구 요청을 찾을 수 없거나 권한이 없는 경우
     */
    @Override
    public void rejectFriendRequest(Long userId, Long requestId) {
        FriendRequest friendRequest = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new DomainException("친구 요청을 찾을 수 없습니다."));

        if (!friendRequest.getReceiverId().equals(userId)) {
            throw new DomainException("해당 요청을 거절할 권한이 없습니다.");
        }

        friendRequest.reject();
        friendRequestRepository.save(friendRequest);
    }
}
