package com.cotalk.domain.port.inbound.profile;

import com.cotalk.domain.entity.ProfileHistory;
import com.cotalk.domain.entity.ProfileHistoryType;

import java.util.List;

/**
 * 프로필 이력 조회 유스케이스.
 * 사용자의 프로필 이력을 조회한다.
 *
 * @author seunggu.lee
 */
public interface GetProfileHistoryUseCase {

    /**
     * 사용자의 프로필 이력을 조회한다.
     *
     * @param userId 사용자 ID
     * @param type 프로필 이력 유형 (null이면 전체 조회)
     * @param viewerId 조회하는 사용자 ID (본인이 아니면 비공개 이력 제외)
     * @return 프로필 이력 목록
     */
    List<ProfileHistory> getProfileHistory(Long userId, ProfileHistoryType type, Long viewerId);
}
