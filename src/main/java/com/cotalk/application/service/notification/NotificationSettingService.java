package com.cotalk.application.service.notification;

import com.cotalk.domain.entity.NotificationSetting;
import com.cotalk.domain.port.inbound.notification.GetNotificationSettingUseCase;
import com.cotalk.domain.port.inbound.notification.UpdateNotificationSettingUseCase;
import com.cotalk.domain.port.outbound.IdGenerator;
import com.cotalk.domain.port.outbound.NotificationSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 알림 설정 조회 및 수정 유스케이스 구현체.
 * 사용자별 알림 설정을 관리한다.
 *
 * @author seunggu.lee
 */
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationSettingService implements GetNotificationSettingUseCase, UpdateNotificationSettingUseCase {

    private final NotificationSettingRepository notificationSettingRepository;
    private final IdGenerator idGenerator;

    /**
     * 사용자의 알림 설정을 조회한다.
     * 설정이 없는 경우 기본 설정을 생성하여 반환한다.
     *
     * @param userId 알림 설정을 조회할 사용자 ID
     * @return 알림 설정 정보
     */
    @Override
    @Transactional(readOnly = true)
    public NotificationSetting getNotificationSetting(Long userId) {
        return notificationSettingRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultSetting(userId));
    }

    /**
     * 사용자의 알림 설정을 수정한다.
     * null이 아닌 값만 업데이트하며, 설정이 없는 경우 기본 설정을 생성 후 수정한다.
     *
     * @param userId                    알림 설정을 수정할 사용자 ID
     * @param messageNotification       메시지 알림 활성화 여부
     * @param friendRequestNotification 친구 요청 알림 활성화 여부
     * @param groupInviteNotification   그룹 초대 알림 활성화 여부
     * @param soundEnabled              알림 소리 활성화 여부
     * @param vibrationEnabled          알림 진동 활성화 여부
     * @param doNotDisturbEnabled       방해 금지 모드 활성화 여부
     * @param doNotDisturbStart         방해 금지 시작 시간
     * @param doNotDisturbEnd           방해 금지 종료 시간
     * @return 수정된 알림 설정 정보
     */
    @Override
    public NotificationSetting updateNotificationSetting(
            Long userId,
            Boolean messageNotification,
            Boolean friendRequestNotification,
            Boolean groupInviteNotification,
            Boolean soundEnabled,
            Boolean vibrationEnabled,
            Boolean doNotDisturbEnabled,
            String doNotDisturbStart,
            String doNotDisturbEnd) {

        NotificationSetting setting = notificationSettingRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultSetting(userId));

        if (messageNotification != null) {
            setting.updateMessageNotification(messageNotification);
        }
        if (friendRequestNotification != null) {
            setting.updateFriendRequestNotification(friendRequestNotification);
        }
        if (groupInviteNotification != null) {
            setting.updateGroupInviteNotification(groupInviteNotification);
        }
        if (soundEnabled != null) {
            setting.updateSoundEnabled(soundEnabled);
        }
        if (vibrationEnabled != null) {
            setting.updateVibrationEnabled(vibrationEnabled);
        }
        if (doNotDisturbEnabled != null) {
            setting.updateDoNotDisturb(doNotDisturbEnabled, doNotDisturbStart, doNotDisturbEnd);
        }

        return notificationSettingRepository.save(setting);
    }

    /**
     * 기본 알림 설정을 생성한다.
     *
     * @param userId 사용자 ID
     * @return 생성된 기본 알림 설정
     */
    private NotificationSetting createDefaultSetting(Long userId) {
        NotificationSetting defaultSetting = NotificationSetting.builder()
                .id(idGenerator.nextId())
                .userId(userId)
                .build();
        return notificationSettingRepository.save(defaultSetting);
    }
}
