package com.cotalk.application.service.profile;

import com.cotalk.domain.entity.ProfileHistory;
import com.cotalk.domain.entity.ProfileHistoryType;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.DomainException;
import com.cotalk.domain.port.inbound.profile.DeleteProfileHistoryUseCase;
import com.cotalk.domain.port.outbound.ProfileHistoryRepository;
import com.cotalk.domain.port.outbound.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 프로필 이력 삭제 서비스.
 *
 * @author seunggu.lee
 */
@Service
@RequiredArgsConstructor
@Transactional
public class DeleteProfileHistoryService implements DeleteProfileHistoryUseCase {

    private final ProfileHistoryRepository profileHistoryRepository;
    private final UserRepository userRepository;

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
        }
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
