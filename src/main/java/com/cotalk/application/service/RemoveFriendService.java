package com.cotalk.application.service;

import com.cotalk.domain.entity.Friend;
import com.cotalk.domain.exception.DomainException;
import com.cotalk.domain.port.inbound.RemoveFriendUseCase;
import com.cotalk.domain.port.outbound.FriendRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RemoveFriendService implements RemoveFriendUseCase {

    private final FriendRepository friendRepository;

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
