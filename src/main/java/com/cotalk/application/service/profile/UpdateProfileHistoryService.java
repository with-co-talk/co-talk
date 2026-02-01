package com.cotalk.application.service.profile;

import com.cotalk.domain.entity.ProfileHistory;
import com.cotalk.domain.exception.DomainException;
import com.cotalk.domain.port.inbound.profile.UpdateProfileHistoryUseCase;
import com.cotalk.domain.port.outbound.ProfileHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 프로필 이력 수정 서비스.
 *
 * @author seunggu.lee
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UpdateProfileHistoryService implements UpdateProfileHistoryUseCase {

    private final ProfileHistoryRepository profileHistoryRepository;

    @Override
    public void updatePrivacy(Long historyId, Long userId, boolean isPrivate) {
        ProfileHistory history = profileHistoryRepository.findById(historyId)
                .orElseThrow(() -> new DomainException("프로필 이력을 찾을 수 없습니다."));

        if (!history.getUserId().equals(userId)) {
            throw new DomainException("본인의 프로필 이력만 수정할 수 있습니다.");
        }

        history.updatePrivacy(isPrivate);
        profileHistoryRepository.save(history);
    }
}
