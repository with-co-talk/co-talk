package com.cotalk.adapter.inbound.rest.dto.notification;

import com.cotalk.domain.entity.NotificationSetting;

/**
 * 알림 설정 응답 DTO.
 *
 * @param userId                             사용자 ID
 * @param messageNotification                메시지 알림 활성화 여부
 * @param friendRequestNotification          친구 요청 알림 활성화 여부
 * @param groupInviteNotification            그룹 초대 알림 활성화 여부
 * @param showMessageContentInNotification   푸시 알림에 메시지 내용 노출 여부
 * @param soundEnabled                       소리 활성화 여부
 * @param vibrationEnabled                   진동 활성화 여부
 * @param doNotDisturbEnabled                방해 금지 모드 활성화 여부
 * @param doNotDisturbStart                  방해 금지 시작 시간
 * @param doNotDisturbEnd                    방해 금지 종료 시간
 * @author seunggu.lee
 */
public record NotificationSettingResponse(
        Long userId,
        boolean messageNotification,
        boolean friendRequestNotification,
        boolean groupInviteNotification,
        boolean showMessageContentInNotification,
        boolean soundEnabled,
        boolean vibrationEnabled,
        boolean doNotDisturbEnabled,
        String doNotDisturbStart,
        String doNotDisturbEnd
) {
    /**
     * NotificationSetting 엔티티로부터 DTO를 생성한다.
     *
     * @param setting NotificationSetting 엔티티
     * @return NotificationSettingResponse 인스턴스
     */
    public static NotificationSettingResponse from(NotificationSetting setting) {
        return new NotificationSettingResponse(
                setting.getUserId(),
                setting.isMessageNotification(),
                setting.isFriendRequestNotification(),
                setting.isGroupInviteNotification(),
                setting.isShowMessageContentInNotification(),
                setting.isSoundEnabled(),
                setting.isVibrationEnabled(),
                setting.isDoNotDisturbEnabled(),
                setting.getDoNotDisturbStart(),
                setting.getDoNotDisturbEnd()
        );
    }
}
