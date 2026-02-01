package com.cotalk.application.service.profile;

import com.cotalk.domain.entity.ProfileHistory;
import com.cotalk.domain.entity.ProfileHistoryType;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.DomainException;
import com.cotalk.domain.port.inbound.profile.SetCurrentProfileUseCase;
import com.cotalk.domain.port.outbound.ProfileHistoryRepository;
import com.cotalk.domain.port.outbound.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 현재 프로필 설정 서비스.
 *
 * @author seunggu.lee
 */
@Service
@RequiredArgsConstructor
@Transactional
public class SetCurrentProfileService implements SetCurrentProfileUseCase {

    private final ProfileHistoryRepository profileHistoryRepository;
    private final UserRepository userRepository;

    @Override
    public void setCurrentProfile(Long historyId, Long userId) {
        ProfileHistory history = profileHistoryRepository.findById(historyId)
                .orElseThrow(() -> new DomainException("프로필 이력을 찾을 수 없습니다."));

        if (!history.getUserId().equals(userId)) {
            throw new DomainException("본인의 프로필 이력만 설정할 수 있습니다.");
        }

        ProfileHistoryType type = history.getType();

        // 기존 현재 프로필 해제
        profileHistoryRepository.findByUserIdAndTypeAndIsCurrentTrue(userId, type)
                .ifPresent(existing -> {
                    existing.setCurrent(false);
                    profileHistoryRepository.save(existing);
                });

        // 새 프로필을 현재로 설정
        history.setCurrent(true);
        profileHistoryRepository.save(history);

        // User 엔티티 업데이트
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new DomainException("사용자를 찾을 수 없습니다."));

        switch (type) {
            case AVATAR -> user.updateAvatarUrl(history.getUrl());
            case BACKGROUND -> user.updateBackgroundUrl(history.getUrl());
            case STATUS_MESSAGE -> user.updateStatusMessage(history.getContent());
        }
        userRepository.save(user);
    }
}
