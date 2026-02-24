package com.cotalk.domain.entity;

import com.cotalk.common.fixture.NotificationSettingTestFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NotificationSetting 엔티티 테스트.
 *
 * @author seunggu.lee
 */
@DisplayName("NotificationSetting")
class NotificationSettingTest {

    @Nested
    @DisplayName("기본값 설정 시")
    class DefaultValues {

        @Test
        @DisplayName("기본적으로 모든 알림이 활성화된다")
        void should_enableAllNotifications_when_default() {
            // given & when
            NotificationSetting setting = NotificationSettingTestFixture.createDefaultSetting();

            // then
            assertThat(setting.isMessageNotification()).isTrue();
            assertThat(setting.isFriendRequestNotification()).isTrue();
            assertThat(setting.isGroupInviteNotification()).isTrue();
            assertThat(setting.isSoundEnabled()).isTrue();
            assertThat(setting.isVibrationEnabled()).isTrue();
        }

        @Test
        @DisplayName("기본적으로 방해금지 모드가 비활성화된다")
        void should_disableDoNotDisturb_when_default() {
            // given & when
            NotificationSetting setting = NotificationSettingTestFixture.createDefaultSetting();

            // then
            assertThat(setting.isDoNotDisturbEnabled()).isFalse();
            assertThat(setting.getDoNotDisturbStart()).isNull();
            assertThat(setting.getDoNotDisturbEnd()).isNull();
        }
    }

    @Nested
    @DisplayName("메시지 알림 설정 변경 시")
    class UpdateMessageNotification {

        @Test
        @DisplayName("메시지 알림을 비활성화한다")
        void should_disableMessageNotification_when_calledWithFalse() {
            // given
            NotificationSetting setting = NotificationSettingTestFixture.builder()
                    .messageNotification(true)
                    .build();

            // when
            setting.updateMessageNotification(false);

            // then
            assertThat(setting.isMessageNotification()).isFalse();
        }

        @Test
        @DisplayName("메시지 알림을 활성화한다")
        void should_enableMessageNotification_when_calledWithTrue() {
            // given
            NotificationSetting setting = NotificationSettingTestFixture.builder()
                    .messageNotification(false)
                    .build();

            // when
            setting.updateMessageNotification(true);

            // then
            assertThat(setting.isMessageNotification()).isTrue();
        }
    }

    @Nested
    @DisplayName("친구 요청 알림 설정 변경 시")
    class UpdateFriendRequestNotification {

        @Test
        @DisplayName("친구 요청 알림을 비활성화한다")
        void should_disableFriendRequestNotification_when_calledWithFalse() {
            // given
            NotificationSetting setting = NotificationSettingTestFixture.builder()
                    .friendRequestNotification(true)
                    .build();

            // when
            setting.updateFriendRequestNotification(false);

            // then
            assertThat(setting.isFriendRequestNotification()).isFalse();
        }
    }

    @Nested
    @DisplayName("그룹 초대 알림 설정 변경 시")
    class UpdateGroupInviteNotification {

        @Test
        @DisplayName("그룹 초대 알림을 비활성화한다")
        void should_disableGroupInviteNotification_when_calledWithFalse() {
            // given
            NotificationSetting setting = NotificationSettingTestFixture.builder()
                    .groupInviteNotification(true)
                    .build();

            // when
            setting.updateGroupInviteNotification(false);

            // then
            assertThat(setting.isGroupInviteNotification()).isFalse();
        }
    }

    @Nested
    @DisplayName("소리 설정 변경 시")
    class UpdateSoundEnabled {

        @Test
        @DisplayName("알림 소리를 비활성화한다")
        void should_disableSound_when_calledWithFalse() {
            // given
            NotificationSetting setting = NotificationSettingTestFixture.builder()
                    .soundEnabled(true)
                    .build();

            // when
            setting.updateSoundEnabled(false);

            // then
            assertThat(setting.isSoundEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("진동 설정 변경 시")
    class UpdateVibrationEnabled {

        @Test
        @DisplayName("알림 진동을 비활성화한다")
        void should_disableVibration_when_calledWithFalse() {
            // given
            NotificationSetting setting = NotificationSettingTestFixture.builder()
                    .vibrationEnabled(true)
                    .build();

            // when
            setting.updateVibrationEnabled(false);

            // then
            assertThat(setting.isVibrationEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("방해 금지 모드 설정 시")
    class UpdateDoNotDisturb {

        @Test
        @DisplayName("방해 금지 모드를 활성화한다")
        void should_enableDoNotDisturb_when_calledWithTrue() {
            // given
            NotificationSetting setting = NotificationSettingTestFixture.createDefaultSetting();

            // when
            setting.updateDoNotDisturb(true, "22:00", "07:00");

            // then
            assertThat(setting.isDoNotDisturbEnabled()).isTrue();
            assertThat(setting.getDoNotDisturbStart()).isEqualTo("22:00");
            assertThat(setting.getDoNotDisturbEnd()).isEqualTo("07:00");
        }

        @Test
        @DisplayName("방해 금지 모드를 비활성화한다")
        void should_disableDoNotDisturb_when_calledWithFalse() {
            // given
            NotificationSetting setting = NotificationSettingTestFixture.createWithDoNotDisturb(1L, 100L, "22:00", "07:00");

            // when
            setting.updateDoNotDisturb(false, null, null);

            // then
            assertThat(setting.isDoNotDisturbEnabled()).isFalse();
            assertThat(setting.getDoNotDisturbStart()).isNull();
            assertThat(setting.getDoNotDisturbEnd()).isNull();
        }

        @Test
        @DisplayName("방해 금지 시간을 변경한다")
        void should_updateDoNotDisturbTime_when_newTimeProvided() {
            // given
            NotificationSetting setting = NotificationSettingTestFixture.createWithDoNotDisturb(1L, 100L, "22:00", "07:00");

            // when
            setting.updateDoNotDisturb(true, "23:00", "06:00");

            // then
            assertThat(setting.isDoNotDisturbEnabled()).isTrue();
            assertThat(setting.getDoNotDisturbStart()).isEqualTo("23:00");
            assertThat(setting.getDoNotDisturbEnd()).isEqualTo("06:00");
        }
    }
}
