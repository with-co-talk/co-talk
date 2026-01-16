package com.cotalk.application.service.friend;

import com.cotalk.domain.entity.ChatRoom;
import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.entity.Friend;
import com.cotalk.domain.entity.FriendRequest;
import com.cotalk.domain.exception.DomainException;
import com.cotalk.domain.port.inbound.chat.CreateChatRoomUseCase;
import com.cotalk.domain.port.inbound.friend.AcceptFriendRequestUseCase;
import com.cotalk.domain.port.outbound.persistence.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.persistence.FriendRepository;
import com.cotalk.domain.port.outbound.persistence.FriendRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 친구 요청 수락 Use Case 구현체.
 *
 * @author Co-Talk Team
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AcceptFriendRequestService implements AcceptFriendRequestUseCase {

    private final FriendRequestRepository friendRequestRepository;
    private final FriendRepository friendRepository;
    private final CreateChatRoomUseCase createChatRoomUseCase;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    @Override
    public UUID acceptFriendRequest(UUID receiverId, UUID requestId) {
        // 친구 요청 조회
        FriendRequest friendRequest = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new DomainException("친구 요청을 찾을 수 없습니다."));

        // 요청 수신자가 맞는지 확인
        if (!friendRequest.getReceiverId().equals(receiverId)) {
            throw new DomainException("이 요청을 수락할 권한이 없습니다.");
        }

        // 이미 처리된 요청인지 확인
        if (!friendRequest.isPending()) {
            throw new DomainException("이미 처리된 요청입니다.");
        }

        // 친구 요청 수락
        friendRequest.accept();
        friendRequestRepository.save(friendRequest);

        // 친구 관계 생성 (양방향)
        Friend friend1 = Friend.builder()
                .userId(friendRequest.getRequesterId())
                .friendId(friendRequest.getReceiverId())
                .status(Friend.FriendStatus.ACCEPTED)
                .build();

        Friend friend2 = Friend.builder()
                .userId(friendRequest.getReceiverId())
                .friendId(friendRequest.getRequesterId())
                .status(Friend.FriendStatus.ACCEPTED)
                .build();

        friendRepository.save(friend1);
        friendRepository.save(friend2);

        // 채팅방 생성
        UUID chatRoomId = createChatRoomUseCase.createChatRoom(
                friendRequest.getRequesterId(),
                friendRequest.getReceiverId()
        );

        return chatRoomId;
    }
}
