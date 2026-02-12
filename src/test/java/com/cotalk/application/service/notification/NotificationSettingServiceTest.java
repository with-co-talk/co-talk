package com.cotalk.application.service.notification;

import com.cotalk.domain.entity.NotificationSetting;
import com.cotalk.domain.port.outbound.NotificationSettingRepository;
import com.cotalk.infrastructure.id.SnowflakeIdGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationSettingServiceTest {

    @Mock
    private NotificationSettingRepository notificationSettingRepository;

    @Mock
    private SnowflakeIdGenerator idGenerator;

    @InjectMocks
    private NotificationSettingService notificationSettingService;

    @Test
    @DisplayName("알림 설정 조회 - 기존 설정이 있는 경우")
    void should_returnExistingSetting_when_settingExists() {
        // given
        Long userId = 100L;
        NotificationSetting existingSetting = NotificationSetting.builder()
                .id(1L)
                .userId(userId)
                .messageNotification(true)
                .friendRequestNotification(true)
                .build();

        given(notificationSettingRepository.findByUserId(userId))
                .willReturn(Optional.of(existingSetting));

        // when
        NotificationSetting result = notificationSettingService.getNotificationSetting(userId);

        // then
        assertThat(result).isEqualTo(existingSetting);
    }

    @Test
    @DisplayName("알림 설정 조회 - 기존 설정이 없는 경우 기본 설정 생성")
    void should_createDefaultSetting_when_settingNotExists() {
        // given
        Long userId = 100L;

        given(notificationSettingRepository.findByUserId(userId))
                .willReturn(Optional.empty());
        given(idGenerator.nextId()).willReturn(1L);
        given(notificationSettingRepository.save(any(NotificationSetting.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        NotificationSetting result = notificationSettingService.getNotificationSetting(userId);

        // then
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.isMessageNotification()).isTrue();
        assertThat(result.isFriendRequestNotification()).isTrue();
        assertThat(result.isGroupInviteNotification()).isTrue();
        assertThat(result.isSoundEnabled()).isTrue();
        assertThat(result.isVibrationEnabled()).isTrue();
        assertThat(result.isDoNotDisturbEnabled()).isFalse();
        verify(notificationSettingRepository).save(any(NotificationSetting.class));
    }

    @Test
    @DisplayName("알림 설정 업데이트")
    void should_updateSetting_when_validInput() {
        // given
        Long userId = 100L;
        NotificationSetting existingSetting = NotificationSetting.builder()
                .id(1L)
                .userId(userId)
                .messageNotification(true)
                .friendRequestNotification(true)
                .soundEnabled(true)
                .build();

        given(notificationSettingRepository.findByUserId(userId))
                .willReturn(Optional.of(existingSetting));
        given(notificationSettingRepository.save(any(NotificationSetting.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        NotificationSetting result = notificationSettingService.updateNotificationSetting(
                userId,
                false,  // messageNotification
                false,  // friendRequestNotification
                null,   // groupInviteNotification (null = 변경 안함)
                null,   // notificationPreviewMode (null = 변경 안함)
                false,  // soundEnabled
                null,   // vibrationEnabled
                true,   // doNotDisturbEnabled
                "22:00", // doNotDisturbStart
                "07:00"  // doNotDisturbEnd
        );

        // then
        assertThat(result.isMessageNotification()).isFalse();
        assertThat(result.isFriendRequestNotification()).isFalse();
        assertThat(result.isSoundEnabled()).isFalse();
        assertThat(result.isDoNotDisturbEnabled()).isTrue();
        assertThat(result.getDoNotDisturbStart()).isEqualTo("22:00");
        assertThat(result.getDoNotDisturbEnd()).isEqualTo("07:00");
    }

    @Test
    @DisplayName("알림 설정 업데이트 - 설정이 없는 경우 새로 생성 후 업데이트")
    void should_createAndUpdateSetting_when_settingNotExists() {
        // given
        Long userId = 100L;

        given(notificationSettingRepository.findByUserId(userId))
                .willReturn(Optional.empty());
        given(idGenerator.nextId()).willReturn(1L);
        given(notificationSettingRepository.save(any(NotificationSetting.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        NotificationSetting result = notificationSettingService.updateNotificationSetting(
                userId,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        // then
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.isMessageNotification()).isFalse();
    }

    @Test
    @DisplayName("모든 파라미터가 null인 경우 기존 설정 유지")
    void should_keepExistingSetting_when_allParametersNull() {
        // given
        Long userId = 100L;
        NotificationSetting existingSetting = NotificationSetting.builder()
                .id(1L)
                .userId(userId)
                .messageNotification(true)
                .friendRequestNotification(true)
                .soundEnabled(true)
                .build();

        given(notificationSettingRepository.findByUserId(userId))
                .willReturn(Optional.of(existingSetting));
        given(notificationSettingRepository.save(any(NotificationSetting.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        NotificationSetting result = notificationSettingService.updateNotificationSetting(
                userId,
                null, null, null, null, null, null, null, null, null
        );

        // then
        assertThat(result.isMessageNotification()).isTrue();
        assertThat(result.isFriendRequestNotification()).isTrue();
        assertThat(result.isSoundEnabled()).isTrue();
    }

    @Test
    @DisplayName("방해 금지 모드만 업데이트")
    void should_updateOnlyDoNotDisturb_when_otherParametersNull() {
        // given
        Long userId = 100L;
        NotificationSetting existingSetting = NotificationSetting.builder()
                .id(1L)
                .userId(userId)
                .messageNotification(true)
                .build();

        given(notificationSettingRepository.findByUserId(userId))
                .willReturn(Optional.of(existingSetting));
        given(notificationSettingRepository.save(any(NotificationSetting.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        NotificationSetting result = notificationSettingService.updateNotificationSetting(
                userId,
                null, null, null, null, null, null,
                true, "22:00", "07:00"
        );

        // then
        assertThat(result.isMessageNotification()).isTrue(); // 기존 값 유지
        assertThat(result.isDoNotDisturbEnabled()).isTrue();
        assertThat(result.getDoNotDisturbStart()).isEqualTo("22:00");
        assertThat(result.getDoNotDisturbEnd()).isEqualTo("07:00");
    }
}
