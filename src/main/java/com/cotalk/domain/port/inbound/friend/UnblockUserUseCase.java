package com.cotalk.domain.port.inbound.friend;

/**
 * 사용자 차단 해제 유스케이스.
 * 차단된 사용자의 차단을 해제한다.
 *
 * @author seunggu.lee
 */
public interface UnblockUserUseCase {

    /**
     * 사용자 차단을 해제한다.
     *
     * @param blockerId 차단을 해제하는 사용자 ID
     * @param blockedId 차단 해제 대상 사용자 ID
     */
    void unblockUser(Long blockerId, Long blockedId);
}
