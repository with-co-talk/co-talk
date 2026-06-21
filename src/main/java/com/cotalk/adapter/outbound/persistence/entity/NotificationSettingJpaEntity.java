package com.cotalk.adapter.outbound.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 알림 설정 JPA 엔티티.
 * persistence 계층 전용이며, 도메인 NotificationSetting과 매핑된다.
 *
 * @author seunggu.lee
 */
@Entity
@Table(name = "notification_settings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class NotificationSettingJpaEntity extends BaseJpaEntity {

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
}
