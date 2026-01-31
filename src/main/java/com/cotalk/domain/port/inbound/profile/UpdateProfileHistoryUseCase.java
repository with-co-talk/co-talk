package com.cotalk.domain.port.inbound.profile;

/**
 * 프로필 이력 수정 유스케이스.
 * 프로필 이력의 나만보기 설정을 변경한다.
 *
 * @author seunggu.lee
 */
public interface UpdateProfileHistoryUseCase {

    /**
     * 프로필 이력의 나만보기 설정을 변경한다.
     *
     * @param historyId 프로필 이력 ID
     * @param userId 요청 사용자 ID (본인 확인용)
     * @param isPrivate 나만보기 여부
     */
    void updatePrivacy(Long historyId, Long userId, boolean isPrivate);
}
