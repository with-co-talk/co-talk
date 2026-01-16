package com.cotalk.application.service.friend;

import com.cotalk.domain.entity.FriendRequest;
import com.cotalk.domain.port.inbound.friend.GetFriendRequestListUseCase;
import com.cotalk.domain.port.outbound.persistence.FriendRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 친구 요청 목록 조회 Use Case 구현체.
 *
 * @author Co-Talk Team
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetFriendRequestListService implements GetFriendRequestListUseCase {

    private final FriendRequestRepository friendRequestRepository;

    @Override
    public List<FriendRequest> getFriendRequestList(UUID userId) {
        return friendRequestRepository.findPendingByReceiverId(userId);
    }
}
