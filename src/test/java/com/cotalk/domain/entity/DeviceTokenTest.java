package com.cotalk.domain.entity;

import com.cotalk.common.fixture.DeviceTokenTestFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeviceTokenTest {

    @Nested
    @DisplayName("DeviceToken 생성")
    class Creation {

        @Test
        @DisplayName("유효한 정보로 DeviceToken 생성 성공")
        void should_createDeviceToken_when_validData() {
            // given
            Long id = 1L;
            Long userId = 100L;
            String token = "fcm-token-12345";
            DeviceToken.DeviceType deviceType = DeviceToken.DeviceType.ANDROID;

            // when
            DeviceToken deviceToken = DeviceTokenTestFixture.createDeviceToken(id, userId, token, deviceType);

            // then
            assertThat(deviceToken.getId()).isEqualTo(id);
            assertThat(deviceToken.getUserId()).isEqualTo(userId);
            assertThat(deviceToken.getToken()).isEqualTo(token);
            assertThat(deviceToken.getDeviceType()).isEqualTo(DeviceToken.DeviceType.ANDROID);
            assertThat(deviceToken.isActive()).isTrue();
        }

        @Test
        @DisplayName("iOS 디바이스 타입으로 생성")
        void should_createDeviceToken_when_iosDevice() {
            // given & when
            DeviceToken deviceToken = DeviceTokenTestFixture.createIosDeviceToken(1L, 100L);

            // then
            assertThat(deviceToken.getDeviceType()).isEqualTo(DeviceToken.DeviceType.IOS);
        }
    }

    @Nested
    @DisplayName("토큰 비활성화")
    class Deactivate {

        @Test
        @DisplayName("토큰을 비활성화하면 active가 false로 변경")
        void should_setActiveFalse_when_deactivate() {
            // given
            DeviceToken deviceToken = DeviceTokenTestFixture.createDeviceToken();

            // when
            deviceToken.deactivate();

            // then
            assertThat(deviceToken.isActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("토큰 업데이트")
    class UpdateToken {

        @Test
        @DisplayName("새로운 토큰으로 업데이트")
        void should_updateToken_when_newToken() {
            // given
            DeviceToken deviceToken = DeviceTokenTestFixture.createInactiveDeviceToken(1L, 100L);

            // when
            deviceToken.updateToken("new-token");

            // then
            assertThat(deviceToken.getToken()).isEqualTo("new-token");
            assertThat(deviceToken.isActive()).isTrue();
        }
    }
}
