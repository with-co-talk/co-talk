package com.cotalk.application.service.user;

import com.cotalk.domain.entity.Friend;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.entity.User.OnlineStatus;
import com.cotalk.domain.exception.UserNotFoundException;
import com.cotalk.domain.port.inbound.user.UpdateUserOnlineStatusUseCase;
import com.cotalk.domain.port.outbound.FriendRepository;
import com.cotalk.domain.port.outbound.TimeProvider;
import com.cotalk.domain.port.outbound.UserEventBroker;
import com.cotalk.domain.port.outbound.UserEventBroker.OnlineStatusEvent;
import com.cotalk.domain.port.outbound.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 사용자 온라인 상태 업데이트 유스케이스 구현체.
 * 사용자의 온라인/오프라인/자리비움 상태와 마지막 활동 시간을 관리한다.
 *
 * @author seunggu.lee
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UpdateUserOnlineStatusService implements UpdateUserOnlineStatusUseCase {

    private final UserRepository userRepository;
    private final FriendRepository friendRepository;
    private final UserEventBroker userEventBroker;
    private final TimeProvider timeProvider;

    /**
     * 사용자의 온라인 상태를 업데이트한다.
     *
     * @param userId 사용자 ID
     * @param status 변경할 온라인 상태 (ONLINE, OFFLINE, AWAY)
     * @throws UserNotFoundException 사용자를 찾을 수 없는 경우
     */
    @Override
    public void updateOnlineStatus(Long userId, OnlineStatus status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        var now = timeProvider.now();
        switch (status) {
            case ONLINE -> user.goOnline(now);
            case OFFLINE -> user.goOffline(now);
            case AWAY -> user.goAway(now);
        }

        userRepository.save(user);
        log.info("User online status updated: userId={}, status={}", userId, status);

        // 모든 친구들에게 온라인 상태 변경 알림
        broadcastOnlineStatusToFriends(user);
    }

    /**
     * 모든 친구들에게 온라인 상태 변경을 브로드캐스트한다.
     *
     * @param user 상태가 변경된 사용자
     */
    private void broadcastOnlineStatusToFriends(User user) {
        // 사용자의 모든 친구 조회
        List<Friend> friends = friendRepository.findAcceptedFriendsByUserId(user.getId());

        // 중복 알림 방지를 위한 Set
        Set<Long> notifiedUserIds = new HashSet<>();

        for (Friend friend : friends) {
            Long friendUserId = friend.getFriendId();

            // 이미 알림을 보낸 사용자는 제외
            if (notifiedUserIds.contains(friendUserId)) {
                continue;
            }

            OnlineStatusEvent event = new OnlineStatusEvent(
                    1,
                    UUID.randomUUID().toString(),
                    user.getId(),
                    user.isOnline(),
                    user.getLastActiveAt()
            );
            userEventBroker.publishOnlineStatus(friendUserId, event);
            notifiedUserIds.add(friendUserId);

            log.debug("Broadcast online status to friend userId={}: targetUserId={}, isOnline={}",
                    friendUserId, user.getId(), user.isOnline());
        }

        log.info("Broadcasted online status to {} friends for userId={}", notifiedUserIds.size(), user.getId());
    }

    /**
     * 사용자를 온라인 상태로 설정한다.
     *
     * @param userId 사용자 ID
     * @throws UserNotFoundException 사용자를 찾을 수 없는 경우
     */
    @Override
    public void setOnline(Long userId) {
        updateOnlineStatus(userId, OnlineStatus.ONLINE);
        log.info("User set online: userId={}", userId);
    }

    /**
     * 사용자를 오프라인 상태로 설정한다.
     * 사용자가 존재하지 않는 경우 (탈퇴, 삭제 등) 조용히 무시한다.
     *
     * @param userId 사용자 ID
     */
    @Override
    public void setOffline(Long userId) {
        try {
            updateOnlineStatus(userId, OnlineStatus.OFFLINE);
            log.info("User set offline: userId={}", userId);
        } catch (UserNotFoundException e) {
            // WebSocket disconnect 시 이미 삭제된 사용자일 수 있음 - 조용히 무시
            log.debug("Ignoring setOffline for non-existent user: userId={}", userId);
        }
    }

    /**
     * 사용자의 마지막 활동 시간을 현재 시간으로 업데이트한다.
     *
     * @param userId 사용자 ID
     * @throws UserNotFoundException 사용자를 찾을 수 없는 경우
     */
    @Override
    public void updateLastActiveAt(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        user.updateLastActiveAt(timeProvider.now());
        userRepository.save(user);
        log.debug("User last active time updated: userId={}", userId);
    }
}
