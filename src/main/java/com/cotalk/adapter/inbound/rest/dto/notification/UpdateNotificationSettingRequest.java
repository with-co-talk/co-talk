package com.cotalk.adapter.inbound.rest.dto.notification;

import jakarta.validation.constraints.Pattern;

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
        @Pattern(
                regexp = "NAME_AND_MESSAGE|NAME_ONLY|NOTHING",
                message = "알림 미리보기 모드는 NAME_AND_MESSAGE, NAME_ONLY, NOTHING 중 하나여야 합니다"
        )
        String notificationPreviewMode,
        Boolean soundEnabled,
        Boolean vibrationEnabled,
        Boolean doNotDisturbEnabled,
        // 형식이 틀린 시간은 저장 후 푸시 발송 경로의 LocalTime.parse()에서
        // 터지므로 반드시 저장 시점에 거부해야 한다.
        @Pattern(
                regexp = "^([01]\\d|2[0-3]):[0-5]\\d$",
                message = "방해 금지 시작 시간은 HH:mm 형식이어야 합니다"
        )
        String doNotDisturbStart,
        @Pattern(
                regexp = "^([01]\\d|2[0-3]):[0-5]\\d$",
                message = "방해 금지 종료 시간은 HH:mm 형식이어야 합니다"
        )
        String doNotDisturbEnd
) {}
