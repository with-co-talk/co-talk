package com.cotalk.domain.port.inbound.profile;

/**
 * 프로필 이력 삭제 유스케이스.
 * 프로필 이력을 삭제한다.
 *
 * @author seunggu.lee
 */
public interface DeleteProfileHistoryUseCase {

    /**
     * 프로필 이력을 삭제한다.
     *
     * @param historyId 프로필 이력 ID
     * @param userId 요청 사용자 ID (본인 확인용)
     */
    void deleteProfileHistory(Long historyId, Long userId);
}
