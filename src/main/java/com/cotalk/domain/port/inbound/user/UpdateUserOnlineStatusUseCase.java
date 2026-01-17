package com.cotalk.domain.port.inbound.user;

import com.cotalk.domain.entity.User.OnlineStatus;

/**
 * 사용자 온라인 상태 업데이트 유스케이스.
 * 사용자의 온라인 상태와 마지막 접속 시간을 관리한다.
 *
 * @author seunggu.lee
 */
public interface UpdateUserOnlineStatusUseCase {

    /**
     * 사용자 온라인 상태를 업데이트한다.
     *
     * @param userId 사용자 ID
     * @param status 온라인 상태
     */
    void updateOnlineStatus(Long userId, OnlineStatus status);

    /**
     * 사용자를 온라인으로 설정한다.
     *
     * @param userId 사용자 ID
     */
    void setOnline(Long userId);

    /**
     * 사용자를 오프라인으로 설정한다.
     *
     * @param userId 사용자 ID
     */
    void setOffline(Long userId);

    /**
     * 사용자 마지막 접속 시간을 업데이트한다.
     *
     * @param userId 사용자 ID
     */
    void updateLastActiveAt(Long userId);
}
