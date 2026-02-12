package com.cotalk.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

/**
 * 알림 설정 엔티티.
 * 사용자의 푸시 알림 설정 정보를 나타낸다.
 *
 * @author seunggu.lee
 */
@Entity
@Table(name = "notification_settings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class NotificationSetting extends BaseEntity {

    @Id
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "message_notification", nullable = false)
    @Builder.Default
    private boolean messageNotification = true;

    @Column(name = "friend_request_notification", nullable = false)
    @Builder.Default
    private boolean friendRequestNotification = true;

    @Column(name = "group_invite_notification", nullable = false)
    @Builder.Default
    private boolean groupInviteNotification = true;

    @Column(name = "notification_preview_mode", nullable = false, length = 20)
    @Builder.Default
    private String notificationPreviewMode = "NAME_AND_MESSAGE";

    @Column(name = "sound_enabled", nullable = false)
    @Builder.Default
    private boolean soundEnabled = true;

    @Column(name = "vibration_enabled", nullable = false)
    @Builder.Default
    private boolean vibrationEnabled = true;

    @Column(name = "do_not_disturb_enabled", nullable = false)
    @Builder.Default
    private boolean doNotDisturbEnabled = false;

    @Column(name = "do_not_disturb_start")
    private String doNotDisturbStart;

    @Column(name = "do_not_disturb_end")
    private String doNotDisturbEnd;

    /**
     * 메시지 알림 설정을 변경한다.
     *
     * @param enabled 활성화 여부
     */
    public void updateMessageNotification(boolean enabled) {
        this.messageNotification = enabled;
    }

    /**
     * 친구 요청 알림 설정을 변경한다.
     *
     * @param enabled 활성화 여부
     */
    public void updateFriendRequestNotification(boolean enabled) {
        this.friendRequestNotification = enabled;
    }

    /**
     * 그룹 초대 알림 설정을 변경한다.
     *
     * @param enabled 활성화 여부
     */
    public void updateGroupInviteNotification(boolean enabled) {
        this.groupInviteNotification = enabled;
    }

    /**
     * 알림 미리보기 모드를 변경한다.
     *
     * @param mode 미리보기 모드 (NAME_AND_MESSAGE, NAME_ONLY, NOTHING)
     */
    public void updateNotificationPreviewMode(String mode) {
        this.notificationPreviewMode = mode;
    }

    /**
     * 알림 소리 설정을 변경한다.
     *
     * @param enabled 활성화 여부
     */
    public void updateSoundEnabled(boolean enabled) {
        this.soundEnabled = enabled;
    }

    /**
     * 알림 진동 설정을 변경한다.
     *
     * @param enabled 활성화 여부
     */
    public void updateVibrationEnabled(boolean enabled) {
        this.vibrationEnabled = enabled;
    }

    /**
     * 방해 금지 모드 설정을 변경한다.
     *
     * @param enabled 활성화 여부
     * @param start 시작 시간 (HH:mm 형식)
     * @param end 종료 시간 (HH:mm 형식)
     */
    public void updateDoNotDisturb(boolean enabled, String start, String end) {
        this.doNotDisturbEnabled = enabled;
        this.doNotDisturbStart = start;
        this.doNotDisturbEnd = end;
    }

    /**
     * 현재 시각이 방해 금지 시간대에 해당하는지 판단한다.
     * 방해 금지 모드가 비활성화되어 있거나 시작/종료 시간이 설정되지 않은 경우 {@code false}를 반환한다.
     * 자정을 넘어가는 시간대(예: 23:00 ~ 07:00)도 올바르게 처리한다.
     *
     * @param now 현재 시각
     * @return 방해 금지 시간대인 경우 {@code true}
     */
    public boolean isInDoNotDisturbTime(LocalTime now) {
        if (!doNotDisturbEnabled || doNotDisturbStart == null || doNotDisturbEnd == null) {
            return false;
        }

        LocalTime start = LocalTime.parse(doNotDisturbStart);
        LocalTime end = LocalTime.parse(doNotDisturbEnd);

        if (start.isBefore(end)) {
            // 같은 날 범위 (예: 09:00 ~ 18:00)
            return !now.isBefore(start) && now.isBefore(end);
        } else {
            // 자정을 넘어가는 범위 (예: 23:00 ~ 07:00)
            return !now.isBefore(start) || now.isBefore(end);
        }
    }
}
