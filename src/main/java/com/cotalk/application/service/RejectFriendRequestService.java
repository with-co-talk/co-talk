package com.cotalk.application.service;

import com.cotalk.domain.entity.FriendRequest;
import com.cotalk.domain.exception.DomainException;
import com.cotalk.domain.port.inbound.RejectFriendRequestUseCase;
import com.cotalk.domain.port.outbound.FriendRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RejectFriendRequestService implements RejectFriendRequestUseCase {

    private final FriendRequestRepository friendRequestRepository;

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
