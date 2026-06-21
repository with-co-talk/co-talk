package com.cotalk.adapter.outbound.persistence.mapper;

import com.cotalk.adapter.outbound.persistence.entity.NotificationSettingJpaEntity;
import com.cotalk.domain.entity.NotificationSetting;
import org.springframework.stereotype.Component;

/**
 * NotificationSetting 도메인과 NotificationSettingJpaEntity 간 매핑.
 *
 * @author seunggu.lee
 */
@Component
public class NotificationSettingMapper {

    /**
     * JPA 엔티티를 도메인 엔티티로 변환한다.
     *
     * @param jpa JPA 엔티티
     * @return 도메인 엔티티, jpa가 null이면 null
     */
    public NotificationSetting toDomain(NotificationSettingJpaEntity jpa) {
        if (jpa == null) {
            return null;
        }
        return NotificationSetting.builder()
                .id(jpa.getId())
                .userId(jpa.getUserId())
                .messageNotification(jpa.isMessageNotification())
                .friendRequestNotification(jpa.isFriendRequestNotification())
                .groupInviteNotification(jpa.isGroupInviteNotification())
                .notificationPreviewMode(jpa.getNotificationPreviewMode())
                .soundEnabled(jpa.isSoundEnabled())
                .vibrationEnabled(jpa.isVibrationEnabled())
                .doNotDisturbEnabled(jpa.isDoNotDisturbEnabled())
                .doNotDisturbStart(jpa.getDoNotDisturbStart())
                .doNotDisturbEnd(jpa.getDoNotDisturbEnd())
                .createdAt(jpa.getCreatedAt())
                .updatedAt(jpa.getUpdatedAt())
                .build();
    }

    /**
     * 도메인 엔티티를 JPA 엔티티로 변환한다.
     *
     * @param domain 도메인 엔티티
     * @return JPA 엔티티, domain이 null이면 null
     */
    public NotificationSettingJpaEntity toJpa(NotificationSetting domain) {
        if (domain == null) {
            return null;
        }
        NotificationSettingJpaEntity jpa = NotificationSettingJpaEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .messageNotification(domain.isMessageNotification())
                .friendRequestNotification(domain.isFriendRequestNotification())
                .groupInviteNotification(domain.isGroupInviteNotification())
                .notificationPreviewMode(domain.getNotificationPreviewMode())
                .soundEnabled(domain.isSoundEnabled())
                .vibrationEnabled(domain.isVibrationEnabled())
                .doNotDisturbEnabled(domain.isDoNotDisturbEnabled())
                .doNotDisturbStart(domain.getDoNotDisturbStart())
                .doNotDisturbEnd(domain.getDoNotDisturbEnd())
                .build();
        if (domain.getCreatedAt() != null) {
            jpa.setCreatedAt(domain.getCreatedAt());
        }
        if (domain.getUpdatedAt() != null) {
            jpa.setUpdatedAt(domain.getUpdatedAt());
        }
        return jpa;
    }
}
