package com.cotalk.application.service.friend;

import com.cotalk.domain.entity.HiddenFriend;
import com.cotalk.domain.exception.FriendNotFoundException;
import com.cotalk.domain.exception.InvalidHiddenFriendException;
import com.cotalk.domain.port.inbound.friend.HideFriendUseCase;
import com.cotalk.domain.port.outbound.FriendRepository;
import com.cotalk.domain.port.outbound.HiddenFriendRepository;
import com.cotalk.domain.validator.UserValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 친구 숨김 유스케이스 구현체.
 * 특정 친구를 숨겨서 목록에서 보이지 않도록 한다.
 *
 * @author seunggu.lee
 */
@Service
@RequiredArgsConstructor
@Transactional
public class HideFriendService implements HideFriendUseCase {

    private final HiddenFriendRepository hiddenFriendRepository;
    private final FriendRepository friendRepository;
    private final UserValidator userValidator;

    /**
     * 친구를 숨긴다.
     * 자기 자신 숨김 불가, 친구 관계 존재 확인, 중복 숨김 불가 조건을 검증하고 숨김 관계를 생성한다.
     *
     * @param userId 숨김을 수행하는 사용자 ID
     * @param friendId 숨길 친구 ID
     * @throws InvalidHiddenFriendException 자기 자신을 숨기거나 이미 숨긴 친구인 경우
     * @throws FriendNotFoundException 친구 관계를 찾을 수 없는 경우
     * @throws UserNotFoundException 사용자를 찾을 수 없는 경우
     */
    @Override
    public void hideFriend(Long userId, Long friendId) {
        userValidator.validateNotSelfAction(userId, friendId, "친구 숨김");
        userValidator.validateUserExists(userId);
        userValidator.validateUserExists(friendId);

        // 친구 관계 존재 확인
        if (!friendRepository.existsByUserIdAndFriendId(userId, friendId)) {
            throw new FriendNotFoundException("친구 관계를 찾을 수 없습니다");
        }

        // 이미 숨긴 친구인지 확인
        if (hiddenFriendRepository.existsByUserIdAndFriendId(userId, friendId)) {
            throw new InvalidHiddenFriendException("이미 숨긴 친구입니다");
        }

        HiddenFriend hiddenFriend = HiddenFriend.builder()
                .userId(userId)
                .friendId(friendId)
                .build();

        hiddenFriendRepository.save(hiddenFriend);
    }
}
