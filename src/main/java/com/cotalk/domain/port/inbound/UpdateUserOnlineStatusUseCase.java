package com.cotalk.domain.port.inbound;

import com.cotalk.domain.entity.User.OnlineStatus;

/**
 * 사용자 온라인 상태 업데이트 유즈케이스
 */
public interface UpdateUserOnlineStatusUseCase {

    /**
     * 사용자 온라인 상태 업데이트
     *
     * @param userId 사용자 ID
     * @param status 온라인 상태
     */
    void updateOnlineStatus(Long userId, OnlineStatus status);

    /**
     * 사용자를 온라인으로 설정
     *
     * @param userId 사용자 ID
     */
    void setOnline(Long userId);

    /**
     * 사용자를 오프라인으로 설정
     *
     * @param userId 사용자 ID
     */
    void setOffline(Long userId);

    /**
     * 사용자 마지막 접속 시간 업데이트
     *
     * @param userId 사용자 ID
     */
    void updateLastActiveAt(Long userId);
}
