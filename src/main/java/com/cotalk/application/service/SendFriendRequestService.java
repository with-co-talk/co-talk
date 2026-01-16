package com.cotalk.application.service;

import com.cotalk.domain.entity.FriendRequest;
import com.cotalk.domain.exception.InvalidFriendRequestException;
import com.cotalk.domain.exception.UserNotFoundException;
import com.cotalk.domain.port.inbound.SendFriendRequestUseCase;
import com.cotalk.domain.port.outbound.FriendRepository;
import com.cotalk.domain.port.outbound.FriendRequestRepository;
import com.cotalk.domain.port.outbound.UserRepository;
import com.cotalk.infrastructure.id.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class SendFriendRequestService implements SendFriendRequestUseCase {

    private final FriendRequestRepository friendRequestRepository;
    private final FriendRepository friendRepository;
    private final UserRepository userRepository;
    private final SnowflakeIdGenerator idGenerator;

    @Override
    public Long sendFriendRequest(Long requesterId, Long receiverId) {
        if (requesterId.equals(receiverId)) {
            throw new InvalidFriendRequestException("자기 자신에게 친구 요청을 보낼 수 없습니다.");
        }

        userRepository.findById(receiverId)
                .orElseThrow(() -> new UserNotFoundException(receiverId));

        if (friendRepository.existsByUserIdAndFriendId(requesterId, receiverId)) {
            throw new InvalidFriendRequestException("이미 친구입니다.");
        }

        if (friendRequestRepository.existsByRequesterIdAndReceiverId(requesterId, receiverId)) {
            throw new InvalidFriendRequestException("이미 친구 요청을 보냈습니다.");
        }

        FriendRequest friendRequest = FriendRequest.builder()
                .id(idGenerator.nextId())
                .requesterId(requesterId)
                .receiverId(receiverId)
                .status(FriendRequest.RequestStatus.PENDING)
                .build();

        friendRequestRepository.save(friendRequest);

        return friendRequest.getId();
    }
}
