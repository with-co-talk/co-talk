package com.cotalk.domain.port.inbound.notification;

import com.cotalk.domain.entity.NotificationSetting;

/**
 * 알림 설정 수정 유스케이스.
 * 사용자의 알림 설정을 수정한다.
 *
 * @author seunggu.lee
 */
public interface UpdateNotificationSettingUseCase {

    /**
     * 사용자의 알림 설정을 수정한다.
     *
     * @param userId 사용자 ID
     * @param messageNotification 메시지 알림 활성화 여부
     * @param friendRequestNotification 친구 요청 알림 활성화 여부
     * @param groupInviteNotification 그룹 초대 알림 활성화 여부
     * @param soundEnabled 소리 활성화 여부
     * @param vibrationEnabled 진동 활성화 여부
     * @param doNotDisturbEnabled 방해금지 모드 활성화 여부
     * @param doNotDisturbStart 방해금지 시작 시간
     * @param doNotDisturbEnd 방해금지 종료 시간
     * @return 수정된 알림 설정
     */
    NotificationSetting updateNotificationSetting(
            Long userId,
            Boolean messageNotification,
            Boolean friendRequestNotification,
            Boolean groupInviteNotification,
            Boolean soundEnabled,
            Boolean vibrationEnabled,
            Boolean doNotDisturbEnabled,
            String doNotDisturbStart,
            String doNotDisturbEnd
    );
}
