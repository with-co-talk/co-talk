package com.cotalk.application.service.profile;

import com.cotalk.domain.entity.Friend;
import com.cotalk.domain.entity.ProfileHistory;
import com.cotalk.domain.entity.ProfileHistoryType;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.DomainException;
import com.cotalk.domain.port.inbound.profile.DeleteProfileHistoryUseCase;
import com.cotalk.domain.port.outbound.FriendRepository;
import com.cotalk.domain.port.outbound.ProfileHistoryRepository;
import com.cotalk.domain.port.outbound.UserEventBroker;
import com.cotalk.domain.port.outbound.UserEventBroker.ProfileUpdateEvent;
import com.cotalk.domain.port.outbound.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 프로필 이력 삭제 서비스.
 * 프로필 이력을 삭제하고 친구들에게 실시간으로 알린다.
 *
 * @author seunggu.lee
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DeleteProfileHistoryService implements DeleteProfileHistoryUseCase {

    private final ProfileHistoryRepository profileHistoryRepository;
    private final UserRepository userRepository;
    private final FriendRepository friendRepository;
    private final UserEventBroker userEventBroker;

    @Override
    public void deleteProfileHistory(Long historyId, Long userId) {
        ProfileHistory history = profileHistoryRepository.findById(historyId)
                .orElseThrow(() -> new DomainException("프로필 이력을 찾을 수 없습니다."));

        if (!history.getUserId().equals(userId)) {
            throw new DomainException("본인의 프로필 이력만 삭제할 수 있습니다.");
        }

        boolean wasCurrent = history.isCurrent();
        ProfileHistoryType type = history.getType();

        profileHistoryRepository.delete(history);

        // 현재 프로필이었다면 이전 이력으로 전환
        if (wasCurrent) {
            List<ProfileHistory> remaining = profileHistoryRepository
                    .findByUserIdAndTypeOrderByCreatedAtDesc(userId, type);

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new DomainException("사용자를 찾을 수 없습니다."));

            if (!remaining.isEmpty()) {
                ProfileHistory newCurrent = remaining.get(0);
                newCurrent.setCurrent(true);
                profileHistoryRepository.save(newCurrent);
                updateUserProfile(user, type, newCurrent);
            } else {
                clearUserProfile(user, type);
            }
            userRepository.save(user);

            // 친구들에게 프로필 변경 알림
            publishProfileUpdateToFriends(user);
        }
    }

    /**
     * 친구들에게 프로필 업데이트 이벤트를 브로드캐스트한다.
     *
     * @param user 프로필이 변경된 사용자
     */
    private void publishProfileUpdateToFriends(User user) {
        List<Friend> friends = friendRepository.findAcceptedFriendsByUserId(user.getId());

        if (friends.isEmpty()) {
            log.debug("No friends to notify for profile update: userId={}", user.getId());
            return;
        }

        String eventId = "profile-update:" + user.getId() + ":" + System.currentTimeMillis();
        ProfileUpdateEvent event = new ProfileUpdateEvent(
                1,
                eventId,
                user.getId(),
                user.getAvatarUrl(),
                user.getBackgroundUrl(),
                user.getStatusMessage(),
                LocalDateTime.now()
        );

        for (Friend friend : friends) {
            userEventBroker.publishProfileUpdate(friend.getFriendId(), event);
        }

        log.info("Profile update event published to {} friends: userId={}", friends.size(), user.getId());
    }

    private void updateUserProfile(User user, ProfileHistoryType type, ProfileHistory history) {
        switch (type) {
            case AVATAR -> user.updateAvatarUrl(history.getUrl());
            case BACKGROUND -> user.updateBackgroundUrl(history.getUrl());
            case STATUS_MESSAGE -> user.updateStatusMessage(history.getContent());
        }
    }

    private void clearUserProfile(User user, ProfileHistoryType type) {
        switch (type) {
            case AVATAR -> user.updateAvatarUrl(null);
            case BACKGROUND -> user.updateBackgroundUrl(null);
            case STATUS_MESSAGE -> user.updateStatusMessage(null);
        }
    }
}
