package com.cotalk.application.service.friend;

import com.cotalk.domain.entity.HiddenFriend;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.model.HiddenFriendInfo;
import com.cotalk.domain.port.inbound.friend.GetHiddenFriendsUseCase;
import com.cotalk.domain.port.outbound.HiddenFriendRepository;
import com.cotalk.domain.port.outbound.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
     * 숨김 관계를 조회하고 해당 사용자 정보를 도메인 모델로 변환하여 반환한다.
     * <p>
     * N+1 쿼리 문제를 방지하기 위해 findAllById를 사용하여 배치 조회한다.
     * </p>
     *
     * @param userId 숨긴 친구 목록을 조회할 사용자 ID
     * @return 숨긴 친구 정보 목록
     */
    @Override
    public List<HiddenFriendInfo> getHiddenFriends(Long userId) {
        List<HiddenFriend> hiddenFriends = hiddenFriendRepository.findByUserId(userId);

        if (hiddenFriends.isEmpty()) {
            return List.of();
        }

        // N+1 방지: 모든 친구 ID를 수집하여 배치 조회
        List<Long> friendIds = hiddenFriends.stream()
                .map(HiddenFriend::getFriendId)
                .toList();

        // 배치 조회
        List<User> friends = userRepository.findAllById(friendIds);

        // ID를 키로 하는 Map 생성 (빠른 조회를 위해)
        Map<Long, User> friendMap = friends.stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        // 도메인 모델 변환 (탈퇴한 사용자는 자동으로 필터링됨)
        return hiddenFriends.stream()
                .map(hiddenFriend -> {
                    User friend = friendMap.get(hiddenFriend.getFriendId());
                    if (friend == null) {
                        return null; // 탈퇴한 사용자
                    }
                    return new HiddenFriendInfo(
                            hiddenFriend.getId(),
                            friend.getId(),
                            friend.getNickname(),
                            friend.getAvatarUrl(),
                            hiddenFriend.getCreatedAt()
                    );
                })
                .filter(info -> info != null)
                .toList();
    }
}
