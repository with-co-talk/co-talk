package com.cotalk.application.service.friend;

import com.cotalk.domain.entity.Friend;
import com.cotalk.domain.exception.FriendNotFoundException;
import com.cotalk.domain.port.inbound.friend.DeleteFriendUseCase;
import com.cotalk.domain.port.outbound.persistence.FriendRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 친구 삭제 Use Case 구현체.
 *
 * @author Co-Talk Team
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Transactional
public class DeleteFriendService implements DeleteFriendUseCase {

    private final FriendRepository friendRepository;

    @Override
    public void deleteFriend(UUID userId, UUID friendId) {
        // 친구 관계 조회
        Friend friend = friendRepository.findByUserIdAndFriendId(userId, friendId)
                .orElseThrow(FriendNotFoundException::new);

        // 양방향 친구 관계 삭제
        friendRepository.delete(friend);

        // 반대 방향 친구 관계도 삭제
        friendRepository.findByUserIdAndFriendId(friendId, userId)
                .ifPresent(friendRepository::delete);
    }
}
