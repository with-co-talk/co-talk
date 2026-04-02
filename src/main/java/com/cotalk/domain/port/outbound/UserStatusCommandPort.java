package com.cotalk.domain.port.outbound;

/**
 * 사용자 상태 변경을 다른 도메인 모듈에서 요청할 때 사용하는 아웃바운드 포트.
 *
 * @author seunggu.lee
 */
public interface UserStatusCommandPort {

    /**
     * 사용자를 온라인 상태로 변경한다.
     *
     * @param userId 사용자 ID
     */
    void setOnline(Long userId);

    /**
     * 사용자를 오프라인 상태로 변경한다.
     *
     * @param userId 사용자 ID
     */
    void setOffline(Long userId);

    /**
     * 사용자의 마지막 활동 시각을 갱신한다.
     *
     * @param userId 사용자 ID
     */
    void updateLastActiveAt(Long userId);
}
