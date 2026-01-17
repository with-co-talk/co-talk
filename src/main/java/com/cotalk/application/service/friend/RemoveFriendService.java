package com.cotalk.application.service.friend;

import com.cotalk.domain.entity.Friend;
import com.cotalk.domain.exception.DomainException;
import com.cotalk.domain.port.inbound.friend.RemoveFriendUseCase;
import com.cotalk.domain.port.outbound.FriendRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 친구 삭제 유스케이스 구현체.
 * 기존 친구 관계를 양방향으로 삭제한다.
 *
 * @author seunggu.lee
 */
@Service
@RequiredArgsConstructor
@Transactional
public class RemoveFriendService implements RemoveFriendUseCase {

    private final FriendRepository friendRepository;

    /**
     * 친구 관계를 삭제한다.
     * 양방향 친구 관계(사용자-친구, 친구-사용자)를 모두 삭제한다.
     *
     * @param userId   친구를 삭제하는 사용자 ID
     * @param friendId 삭제할 친구의 사용자 ID
     * @throws DomainException 친구 관계를 찾을 수 없는 경우
     */
    @Override
    public void removeFriend(Long userId, Long friendId) {
        Friend friend = friendRepository.findByUserIdAndFriendId(userId, friendId)
                .orElseThrow(() -> new DomainException("친구 관계를 찾을 수 없습니다."));

        friendRepository.delete(friend);

        // 양방향 친구 관계 삭제
        friendRepository.findByUserIdAndFriendId(friendId, userId)
                .ifPresent(friendRepository::delete);
    }
}
