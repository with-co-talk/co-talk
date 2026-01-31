package com.cotalk.application.service.profile;

import com.cotalk.domain.entity.ProfileHistory;
import com.cotalk.domain.entity.ProfileHistoryType;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.DomainException;
import com.cotalk.domain.port.inbound.profile.CreateProfileHistoryUseCase;
import com.cotalk.domain.port.outbound.ProfileHistoryRepository;
import com.cotalk.domain.port.outbound.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 프로필 이력 생성 서비스.
 *
 * @author seunggu.lee
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CreateProfileHistoryService implements CreateProfileHistoryUseCase {

    private final ProfileHistoryRepository profileHistoryRepository;
    private final UserRepository userRepository;

    @Override
    public ProfileHistory createProfileHistory(Long userId, ProfileHistoryType type,
                                               String url, String content,
                                               boolean isPrivate, boolean setCurrent) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new DomainException("사용자를 찾을 수 없습니다."));

        // 현재 프로필로 설정하는 경우 기존 현재 프로필 해제
        if (setCurrent) {
            profileHistoryRepository.findByUserIdAndTypeAndIsCurrentTrue(userId, type)
                    .ifPresent(existing -> existing.setCurrent(false));
        }

        ProfileHistory history = ProfileHistory.builder()
                .userId(userId)
                .type(type)
                .url(url)
                .content(content)
                .isPrivate(isPrivate)
                .isCurrent(setCurrent)
                .build();

        ProfileHistory saved = profileHistoryRepository.save(history);

        // User 엔티티도 업데이트
        if (setCurrent) {
            updateUserProfile(user, type, url, content);
            userRepository.save(user);
        }

        return saved;
    }

    private void updateUserProfile(User user, ProfileHistoryType type, String url, String content) {
        switch (type) {
            case AVATAR -> user.updateAvatarUrl(url);
            case BACKGROUND -> user.updateBackgroundUrl(url);
            case STATUS_MESSAGE -> user.updateStatusMessage(content);
        }
    }
}
