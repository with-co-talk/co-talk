package com.cotalk.application.service.friend;

import com.cotalk.adapter.inbound.rest.dto.friend.HiddenFriendDto;
import com.cotalk.domain.entity.HiddenFriend;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.port.inbound.friend.GetHiddenFriendsUseCase;
import com.cotalk.domain.port.outbound.HiddenFriendRepository;
import com.cotalk.domain.port.outbound.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 숨긴 친구 목록 조회 유스케이스 구현체.
 * 사용자가 숨긴 친구 목록을 조회한다.
 *
 * @author seunggu.lee
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetHiddenFriendsService implements GetHiddenFriendsUseCase {

    private final HiddenFriendRepository hiddenFriendRepository;
    private final UserRepository userRepository;

    /**
     * 숨긴 친구 목록을 조회한다.
     * 숨김 관계를 조회하고 해당 사용자 정보를 DTO로 변환하여 반환한다.
     *
     * @param userId 숨긴 친구 목록을 조회할 사용자 ID
     * @return 숨긴 친구 정보 목록
     */
    @Override
    public List<HiddenFriendDto> getHiddenFriends(Long userId) {
        List<HiddenFriend> hiddenFriends = hiddenFriendRepository.findByUserId(userId);

        return hiddenFriends.stream()
                .map(hiddenFriend -> userRepository.findById(hiddenFriend.getFriendId())
                        .map(friend -> HiddenFriendDto.builder()
                                .id(hiddenFriend.getId())
                                .friendId(friend.getId())
                                .nickname(friend.getNickname())
                                .profileImageUrl(friend.getAvatarUrl())
                                .hiddenAt(hiddenFriend.getCreatedAt())
                                .build())
                        .orElse(null))
                .filter(dto -> dto != null)
                .toList();
    }
}
