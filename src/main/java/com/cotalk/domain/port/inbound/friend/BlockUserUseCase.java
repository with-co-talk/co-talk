package com.cotalk.domain.port.inbound.friend;

/**
 * 사용자 차단 유스케이스.
 * 특정 사용자를 차단한다.
 *
 * @author seunggu.lee
 */
public interface BlockUserUseCase {

    /**
     * 사용자를 차단한다.
     *
     * @param blockerId 차단하는 사용자 ID
     * @param blockedId 차단당하는 사용자 ID
     */
    void blockUser(Long blockerId, Long blockedId);
}
