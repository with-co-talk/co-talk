package com.cotalk.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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
public class NotificationSetting {

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

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 엔티티 생성 시 호출되는 콜백 메서드.
     * 생성 시간과 수정 시간을 현재 시간으로 설정한다.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    /**
     * 엔티티 수정 시 호출되는 콜백 메서드.
     * 수정 시간을 현재 시간으로 갱신한다.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

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
}
