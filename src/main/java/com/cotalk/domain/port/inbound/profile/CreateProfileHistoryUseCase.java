package com.cotalk.domain.port.inbound.profile;

import com.cotalk.domain.entity.ProfileHistory;
import com.cotalk.domain.entity.ProfileHistoryType;

/**
 * 프로필 이력 생성 유스케이스.
 * 새로운 프로필 이력을 생성한다.
 *
 * @author seunggu.lee
 */
public interface CreateProfileHistoryUseCase {

    /**
     * 새로운 프로필 이력을 생성한다.
     *
     * @param userId 사용자 ID
     * @param type 프로필 이력 유형
     * @param url 이미지 URL (AVATAR, BACKGROUND인 경우)
     * @param content 내용 (STATUS_MESSAGE인 경우)
     * @param isPrivate 나만보기 여부
     * @param setCurrent 현재 프로필로 설정할지 여부
     * @return 생성된 프로필 이력
     */
    ProfileHistory createProfileHistory(Long userId, ProfileHistoryType type,
                                        String url, String content,
                                        boolean isPrivate, boolean setCurrent);
}
