package com.cotalk.application.service.profile;

import com.cotalk.domain.entity.ProfileHistory;
import com.cotalk.domain.entity.ProfileHistoryType;
import com.cotalk.domain.port.inbound.profile.GetProfileHistoryUseCase;
import com.cotalk.domain.port.outbound.ProfileHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 프로필 이력 조회 서비스.
 *
 * @author seunggu.lee
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetProfileHistoryService implements GetProfileHistoryUseCase {

    private final ProfileHistoryRepository profileHistoryRepository;

    @Override
    public List<ProfileHistory> getProfileHistory(Long userId, ProfileHistoryType type, Long viewerId) {
        List<ProfileHistory> histories;

        if (type != null) {
            histories = profileHistoryRepository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, type);
        } else {
            histories = profileHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId);
        }

        // 본인이 아닌 경우 비공개 이력 제외
        if (!userId.equals(viewerId)) {
            histories = histories.stream()
                    .filter(h -> !h.isPrivate())
                    .toList();
        }

        return histories;
    }
}
