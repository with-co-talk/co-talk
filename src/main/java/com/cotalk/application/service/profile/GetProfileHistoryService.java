package com.cotalk.application.service.profile;

import com.cotalk.domain.entity.ProfileHistory;
import com.cotalk.domain.entity.ProfileHistoryType;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.port.inbound.profile.GetProfileHistoryUseCase;
import com.cotalk.domain.port.outbound.ProfileHistoryRepository;
import com.cotalk.domain.port.outbound.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 프로필 이력 조회 서비스.
 * 이력이 없지만 User에 현재 프로필 값이 있는 경우 자동으로 이력을 생성한다.
 *
 * @author seunggu.lee
 */
@Service
@RequiredArgsConstructor
@Transactional
public class GetProfileHistoryService implements GetProfileHistoryUseCase {

    private final ProfileHistoryRepository profileHistoryRepository;
    private final UserRepository userRepository;

    @Override
    public List<ProfileHistory> getProfileHistory(Long userId, ProfileHistoryType type, Long viewerId) {
        List<ProfileHistory> histories;

        if (type != null) {
            histories = new ArrayList<>(
                    profileHistoryRepository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, type));

            // 본인이 조회할 때만: 이력이 없고 User에 해당 값이 있으면 자동으로 이력 생성
            if (histories.isEmpty() && userId.equals(viewerId)) {
                ProfileHistory created = tryCreateHistoryFromUser(userId, type);
                if (created != null) {
                    histories.add(created);
                }
            }

            // 본인이 조회할 때: User의 현재 값과 isCurrent history의 값이 다르면 새 이력 생성
            if (!histories.isEmpty() && userId.equals(viewerId)) {
                histories = syncCurrentHistoryWithUser(histories, userId, type);
            }
        } else {
            histories = new ArrayList<>(
                    profileHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId));
        }

        // 본인이 아닌 경우 비공개 이력 제외
        if (!userId.equals(viewerId)) {
            histories = histories.stream()
                    .filter(h -> !h.isPrivate())
                    .toList();
        }

        return histories;
    }

    /**
     * User 엔티티의 현재 프로필 값으로 이력을 자동 생성한다.
     * 기존에 프로필 이력 없이 프로필 사진, 배경화면, 상태메시지가 설정된 경우를 위한 마이그레이션 로직.
     *
     * @param userId 사용자 ID
     * @param type   프로필 이력 유형
     * @return 생성된 이력, 해당 값이 없으면 null
     */
    private ProfileHistory tryCreateHistoryFromUser(Long userId, ProfileHistoryType type) {
        return userRepository.findById(userId)
                .map(user -> createHistoryIfValueExists(user, type))
                .orElse(null);
    }

    /**
     * User의 현재 값이 있으면 이력을 생성한다.
     *
     * @param user 사용자 엔티티
     * @param type 프로필 이력 유형
     * @return 생성된 이력, 값이 없으면 null
     */
    private ProfileHistory createHistoryIfValueExists(User user, ProfileHistoryType type) {
        String url = null;
        String content = null;

        switch (type) {
            case AVATAR -> url = user.getAvatarUrl();
            case BACKGROUND -> url = user.getBackgroundUrl();
            case STATUS_MESSAGE -> content = user.getStatusMessage();
        }

        // 값이 없으면 null 반환
        if ((url == null || url.isBlank()) && (content == null || content.isBlank())) {
            return null;
        }

        ProfileHistory history = ProfileHistory.builder()
                .userId(user.getId())
                .type(type)
                .url(url)
                .content(content)
                .isPrivate(false)
                .isCurrent(true)
                .build();

        return profileHistoryRepository.save(history);
    }

    /**
     * User의 현재 프로필 값과 isCurrent history의 값을 비교하여 동기화한다.
     * 값이 다르면 새 이력을 생성하고 기존 current를 해제한다.
     *
     * @param histories 기존 이력 목록
     * @param userId    사용자 ID
     * @param type      프로필 이력 유형
     * @return 동기화된 이력 목록
     */
    private List<ProfileHistory> syncCurrentHistoryWithUser(
            List<ProfileHistory> histories, Long userId, ProfileHistoryType type) {

        return userRepository.findById(userId)
                .map(user -> {
                    String userValue = getUserValueByType(user, type);

                    // User에 값이 없으면 동기화 불필요
                    if (userValue == null || userValue.isBlank()) {
                        return histories;
                    }

                    // isCurrent인 이력 찾기
                    ProfileHistory currentHistory = histories.stream()
                            .filter(ProfileHistory::isCurrent)
                            .findFirst()
                            .orElse(null);

                    // isCurrent 이력이 없거나 값이 다르면 새 이력 생성
                    String historyValue = getHistoryValue(currentHistory, type);
                    if (!userValue.equals(historyValue)) {
                        // 기존 current 해제
                        if (currentHistory != null) {
                            currentHistory.setCurrent(false);
                            profileHistoryRepository.save(currentHistory);
                        }

                        // 새 이력 생성
                        ProfileHistory newHistory = createHistoryFromUserValue(user, type, userValue);

                        // 목록 업데이트: 새 이력을 맨 앞에 추가
                        List<ProfileHistory> updatedHistories = new ArrayList<>();
                        updatedHistories.add(newHistory);
                        // 기존 이력들 추가 (currentHistory는 이미 setCurrent(false)로 변경됨)
                        updatedHistories.addAll(histories);
                        return updatedHistories;
                    }

                    return histories;
                })
                .orElse(histories);
    }

    /**
     * User 엔티티에서 타입에 맞는 값을 가져온다.
     */
    private String getUserValueByType(User user, ProfileHistoryType type) {
        return switch (type) {
            case AVATAR -> user.getAvatarUrl();
            case BACKGROUND -> user.getBackgroundUrl();
            case STATUS_MESSAGE -> user.getStatusMessage();
        };
    }

    /**
     * ProfileHistory에서 타입에 맞는 값을 가져온다.
     */
    private String getHistoryValue(ProfileHistory history, ProfileHistoryType type) {
        if (history == null) {
            return null;
        }
        return switch (type) {
            case AVATAR, BACKGROUND -> history.getUrl();
            case STATUS_MESSAGE -> history.getContent();
        };
    }

    /**
     * User의 현재 값으로 새 이력을 생성한다.
     */
    private ProfileHistory createHistoryFromUserValue(User user, ProfileHistoryType type, String value) {
        String url = null;
        String content = null;

        switch (type) {
            case AVATAR, BACKGROUND -> url = value;
            case STATUS_MESSAGE -> content = value;
        }

        ProfileHistory history = ProfileHistory.builder()
                .userId(user.getId())
                .type(type)
                .url(url)
                .content(content)
                .isPrivate(false)
                .isCurrent(true)
                .build();

        return profileHistoryRepository.save(history);
    }
}
