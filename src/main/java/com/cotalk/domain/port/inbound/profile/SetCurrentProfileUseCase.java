package com.cotalk.domain.port.inbound.profile;

/**
 * 현재 프로필 설정 유스케이스.
 * 특정 프로필 이력을 현재 프로필로 설정한다.
 *
 * @author seunggu.lee
 */
public interface SetCurrentProfileUseCase {

    /**
     * 특정 프로필 이력을 현재 프로필로 설정한다.
     *
     * @param historyId 프로필 이력 ID
     * @param userId 요청 사용자 ID (본인 확인용)
     */
    void setCurrentProfile(Long historyId, Long userId);
}
