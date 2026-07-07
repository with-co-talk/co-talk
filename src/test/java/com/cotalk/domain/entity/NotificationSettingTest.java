package com.cotalk.domain.entity;

import com.cotalk.common.fixture.NotificationSettingTestFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

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

    @Nested
    @DisplayName("방해 금지 시간대 판별 시")
    class IsInDoNotDisturbTime {

        @Test
        @DisplayName("자정을 넘는 범위에서 시간대 내이면 true를 반환한다")
        void should_returnTrue_when_nowWithinOvernightRange() {
            // given
            NotificationSetting setting = NotificationSettingTestFixture.createWithDoNotDisturb(1L, 100L, "23:00", "07:00");

            // when & then
            assertThat(setting.isInDoNotDisturbTime(LocalTime.of(0, 30))).isTrue();
            assertThat(setting.isInDoNotDisturbTime(LocalTime.of(12, 0))).isFalse();
        }

        @Test
        @DisplayName("저장된 시간 형식이 손상돼도 예외 대신 false를 반환한다")
        void should_returnFalse_when_storedTimeFormatCorrupted() {
            // 검증 도입 이전에 저장된 잘못된 형식이 푸시 발송(특히 벌크 필터)
            // 경로 전체를 중단시키지 않아야 한다. 설정 미비 = 허용과 동일하게 취급.
            NotificationSetting corrupted = NotificationSettingTestFixture.createWithDoNotDisturb(1L, 100L, "25:99", "07:00");

            assertThat(corrupted.isInDoNotDisturbTime(LocalTime.of(0, 30))).isFalse();
        }

        @Test
        @DisplayName("콜론 없는 손상 형식도 예외 대신 false를 반환한다")
        void should_returnFalse_when_storedTimeMissingColon() {
            NotificationSetting corrupted = NotificationSettingTestFixture.createWithDoNotDisturb(1L, 100L, "2300", "0700");

            assertThat(corrupted.isInDoNotDisturbTime(LocalTime.of(23, 30))).isFalse();
        }
    }
}
