package com.cotalk.application.service.friend;

import com.cotalk.domain.entity.FriendRequest;
import com.cotalk.domain.exception.DomainException;
import com.cotalk.domain.port.inbound.friend.RejectFriendRequestUseCase;
import com.cotalk.domain.port.outbound.persistence.FriendRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 친구 요청 거절 Use Case 구현체.
 *
 * @author Co-Talk Team
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Transactional
public class RejectFriendRequestService implements RejectFriendRequestUseCase {

    private final FriendRequestRepository friendRequestRepository;

    @Override
    public void rejectFriendRequest(UUID receiverId, UUID requestId) {
        // 친구 요청 조회
        FriendRequest friendRequest = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new DomainException("친구 요청을 찾을 수 없습니다."));

        // 요청 수신자가 맞는지 확인
        if (!friendRequest.getReceiverId().equals(receiverId)) {
            throw new DomainException("이 요청을 거절할 권한이 없습니다.");
        }

        // 이미 처리된 요청인지 확인
        if (!friendRequest.isPending()) {
            throw new DomainException("이미 처리된 요청입니다.");
        }

        // 친구 요청 거절
        friendRequest.reject();
        friendRequestRepository.save(friendRequest);
    }
}
