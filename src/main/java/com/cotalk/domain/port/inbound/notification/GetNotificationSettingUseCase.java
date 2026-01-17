package com.cotalk.domain.port.inbound.notification;

import com.cotalk.domain.entity.NotificationSetting;

/**
 * 알림 설정 조회 유스케이스.
 * 사용자의 알림 설정을 조회한다.
 *
 * @author seunggu.lee
 */
public interface GetNotificationSettingUseCase {

    /**
     * 사용자의 알림 설정을 조회한다.
     *
     * @param userId 사용자 ID
     * @return 알림 설정
     */
    NotificationSetting getNotificationSetting(Long userId);
}
