package com.cotalk.application.service.friend;

import com.cotalk.domain.entity.Friend;
import com.cotalk.domain.entity.FriendRequest;
import com.cotalk.domain.exception.InvalidFriendRequestException;
import com.cotalk.domain.port.inbound.friend.SendFriendRequestUseCase;
import com.cotalk.domain.port.outbound.persistence.FriendRepository;
import com.cotalk.domain.port.outbound.persistence.FriendRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 친구 요청 전송 Use Case 구현체.
 *
 * @author Co-Talk Team
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Transactional
public class SendFriendRequestService implements SendFriendRequestUseCase {

    private final FriendRequestRepository friendRequestRepository;
    private final FriendRepository friendRepository;

    @Override
    public UUID sendFriendRequest(UUID requesterId, UUID receiverId) {
        // 자기 자신에게 요청하는 경우
        if (requesterId.equals(receiverId)) {
            throw new InvalidFriendRequestException("자기 자신에게 친구 요청을 보낼 수 없습니다.");
        }

        // 이미 친구인 경우
        if (friendRepository.existsByUserIdAndFriendId(requesterId, receiverId)) {
            throw new InvalidFriendRequestException("이미 친구입니다.");
        }

        // 이미 요청을 보낸 경우
        if (friendRequestRepository.existsByRequesterIdAndReceiverId(requesterId, receiverId)) {
            throw new InvalidFriendRequestException("이미 친구 요청을 보냈습니다.");
        }

        // 친구 요청 생성
        FriendRequest friendRequest = FriendRequest.builder()
                .requesterId(requesterId)
                .receiverId(receiverId)
                .status(FriendRequest.RequestStatus.PENDING)
                .build();

        FriendRequest savedRequest = friendRequestRepository.save(friendRequest);
        return savedRequest.getId();
    }
}
