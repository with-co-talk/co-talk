package com.cotalk.adapter.inbound.rest.dto.notification;

/**
 * 알림 설정 업데이트 요청 DTO.
 *
 * @param messageNotification        메시지 알림 활성화 여부
 * @param friendRequestNotification  친구 요청 알림 활성화 여부
 * @param groupInviteNotification    그룹 초대 알림 활성화 여부
 * @param notificationPreviewMode    알림 미리보기 모드 (NAME_AND_MESSAGE, NAME_ONLY, NOTHING)
 * @param soundEnabled               소리 활성화 여부
 * @param vibrationEnabled           진동 활성화 여부
 * @param doNotDisturbEnabled        방해 금지 모드 활성화 여부
 * @param doNotDisturbStart          방해 금지 시작 시간 (HH:mm 형식)
 * @param doNotDisturbEnd            방해 금지 종료 시간 (HH:mm 형식)
 * @author seunggu.lee
 */
public record UpdateNotificationSettingRequest(
        Boolean messageNotification,
        Boolean friendRequestNotification,
        Boolean groupInviteNotification,
        String notificationPreviewMode,
        Boolean soundEnabled,
        Boolean vibrationEnabled,
        Boolean doNotDisturbEnabled,
        String doNotDisturbStart,
        String doNotDisturbEnd
) {}
