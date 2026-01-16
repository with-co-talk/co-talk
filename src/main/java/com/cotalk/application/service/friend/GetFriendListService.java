package com.cotalk.application.service.friend;

import com.cotalk.domain.entity.Friend;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.port.inbound.friend.GetFriendListUseCase;
import com.cotalk.domain.port.outbound.persistence.FriendRepository;
import com.cotalk.domain.port.outbound.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 친구 목록 조회 Use Case 구현체.
 *
 * @author Co-Talk Team
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetFriendListService implements GetFriendListUseCase {

    private final FriendRepository friendRepository;
    private final UserRepository userRepository;

    @Override
    public List<User> getFriendList(UUID userId) {
        // 수락된 친구 관계만 조회
        List<Friend> friends = friendRepository.findAcceptedFriendsByUserId(userId);

        // 친구 ID 목록 추출
        List<UUID> friendIds = friends.stream()
                .map(Friend::getFriendId)
                .collect(Collectors.toList());

        // 친구 사용자 정보 조회
        return friendIds.stream()
                .map(friendId -> userRepository.findById(friendId))
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .collect(Collectors.toList());
    }
}
