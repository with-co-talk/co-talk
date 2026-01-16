package com.cotalk.application.service;

import com.cotalk.domain.entity.Friend;
import com.cotalk.domain.entity.FriendRequest;
import com.cotalk.domain.exception.FriendNotFoundException;
import com.cotalk.domain.exception.InvalidFriendRequestException;
import com.cotalk.domain.port.inbound.AcceptFriendRequestUseCase;
import com.cotalk.domain.port.outbound.FriendRepository;
import com.cotalk.domain.port.outbound.FriendRequestRepository;
import com.cotalk.infrastructure.id.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AcceptFriendRequestService implements AcceptFriendRequestUseCase {

    private final FriendRequestRepository friendRequestRepository;
    private final FriendRepository friendRepository;
    private final SnowflakeIdGenerator idGenerator;

    @Override
    public Long acceptFriendRequest(Long receiverId, Long requestId) {
        FriendRequest friendRequest = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new FriendNotFoundException("친구 요청을 찾을 수 없습니다."));

        if (!friendRequest.getReceiverId().equals(receiverId)) {
            throw new InvalidFriendRequestException("본인이 받은 친구 요청만 수락할 수 있습니다.");
        }

        if (!friendRequest.isPending()) {
            throw new InvalidFriendRequestException("이미 처리된 친구 요청입니다.");
        }

        friendRequest.accept();
        friendRequestRepository.save(friendRequest);

        Friend friend1 = Friend.builder()
                .id(idGenerator.nextId())
                .userId(friendRequest.getRequesterId())
                .friendId(friendRequest.getReceiverId())
                .status(Friend.FriendStatus.ACCEPTED)
                .build();

        Friend friend2 = Friend.builder()
                .id(idGenerator.nextId())
                .userId(friendRequest.getReceiverId())
                .friendId(friendRequest.getRequesterId())
                .status(Friend.FriendStatus.ACCEPTED)
                .build();

        friendRepository.save(friend1);
        friendRepository.save(friend2);

        return friend1.getId();
    }
}
