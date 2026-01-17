package com.cotalk.application.service.friend;

import com.cotalk.domain.entity.FriendRequest;
import com.cotalk.domain.exception.InvalidFriendRequestException;
import com.cotalk.domain.exception.UserNotFoundException;
import com.cotalk.domain.port.inbound.friend.SendFriendRequestUseCase;
import com.cotalk.domain.port.outbound.FriendRepository;
import com.cotalk.domain.port.outbound.FriendRequestRepository;
import com.cotalk.domain.port.outbound.UserRepository;
import com.cotalk.infrastructure.id.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 친구 요청 전송 유스케이스 구현체.
 * 다른 사용자에게 친구 요청을 보낸다.
 *
 * @author seunggu.lee
 */
@Service
@RequiredArgsConstructor
@Transactional
public class SendFriendRequestService implements SendFriendRequestUseCase {

    private final FriendRequestRepository friendRequestRepository;
    private final FriendRepository friendRepository;
    private final UserRepository userRepository;
    private final SnowflakeIdGenerator idGenerator;

    /**
     * 친구 요청을 전송한다.
     * 자기 자신에게 요청 불가, 이미 친구인 경우 불가, 중복 요청 불가 조건을 검증한다.
     *
     * @param requesterId 요청을 보내는 사용자 ID
     * @param receiverId  요청을 받는 사용자 ID
     * @return 생성된 친구 요청 ID
     * @throws InvalidFriendRequestException 자기 자신에게 요청하거나, 이미 친구이거나, 중복 요청인 경우
     * @throws UserNotFoundException         수신자를 찾을 수 없는 경우
     */
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
