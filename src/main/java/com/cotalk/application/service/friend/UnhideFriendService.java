package com.cotalk.application.service.friend;

import com.cotalk.domain.entity.HiddenFriend;
import com.cotalk.domain.exception.HiddenFriendNotFoundException;
import com.cotalk.domain.port.inbound.friend.UnhideFriendUseCase;
import com.cotalk.domain.port.outbound.HiddenFriendRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 친구 숨김 해제 유스케이스 구현체.
 * 기존에 숨긴 친구의 숨김을 해제한다.
 *
 * @author seunggu.lee
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UnhideFriendService implements UnhideFriendUseCase {

    private final HiddenFriendRepository hiddenFriendRepository;

    /**
     * 친구 숨김을 해제한다.
     * 기존 숨김 관계를 삭제한다.
     *
     * @param userId 숨김 해제를 수행하는 사용자 ID
     * @param friendId 숨김 해제할 친구 ID
     * @throws HiddenFriendNotFoundException 숨김 관계를 찾을 수 없는 경우
     */
    @Override
    public void unhideFriend(Long userId, Long friendId) {
        HiddenFriend hiddenFriend = hiddenFriendRepository.findByUserIdAndFriendId(userId, friendId)
                .orElseThrow(() -> new HiddenFriendNotFoundException("숨김 관계를 찾을 수 없습니다"));

        hiddenFriendRepository.delete(hiddenFriend);
    }
}
