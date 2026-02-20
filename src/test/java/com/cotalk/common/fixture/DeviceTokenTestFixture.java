package com.cotalk.common.fixture;

import com.cotalk.domain.entity.DeviceToken;

/**
 * DeviceToken 엔티티 테스트 픽스처.
 * 테스트에서 반복적으로 사용되는 DeviceToken 객체 생성 메서드를 제공한다.
 *
 * @author seunggu.lee
 */
public class DeviceTokenTestFixture {

    private static final Long DEFAULT_ID = 1L;
    private static final Long DEFAULT_USER_ID = 100L;
    private static final String DEFAULT_TOKEN = "fcm-token-12345";
    private static final DeviceToken.DeviceType DEFAULT_DEVICE_TYPE = DeviceToken.DeviceType.ANDROID;

    /**
     * 기본값(Android)으로 DeviceToken 객체를 생성한다.
     *
     * @return 활성 상태의 DeviceToken 엔티티
     */
    public static DeviceToken createDeviceToken() {
        return createDeviceToken(DEFAULT_ID, DEFAULT_USER_ID);
    }

    /**
     * 지정된 ID와 사용자 ID로 Android DeviceToken 객체를 생성한다.
     *
     * @param id     토큰 ID
     * @param userId 사용자 ID
     * @return 활성 상태의 DeviceToken 엔티티
     */
    public static DeviceToken createDeviceToken(Long id, Long userId) {
        return createDeviceToken(id, userId, DEFAULT_TOKEN, DEFAULT_DEVICE_TYPE);
    }

    /**
     * 모든 파라미터를 지정하여 DeviceToken 객체를 생성한다.
     *
     * @param id         토큰 ID
     * @param userId     사용자 ID
     * @param token      토큰 값
     * @param deviceType 디바이스 유형
     * @return 활성 상태의 DeviceToken 엔티티
     */
    public static DeviceToken createDeviceToken(Long id, Long userId, String token, DeviceToken.DeviceType deviceType) {
        return DeviceToken.builder()
                .id(id)
                .userId(userId)
                .token(token)
                .deviceType(deviceType)
                .build();
    }

    /**
     * iOS DeviceToken 객체를 생성한다.
     *
     * @param id     토큰 ID
     * @param userId 사용자 ID
     * @return iOS 디바이스의 활성 DeviceToken 엔티티
     */
    public static DeviceToken createIosDeviceToken(Long id, Long userId) {
        return DeviceToken.builder()
                .id(id)
                .userId(userId)
                .token("apns-token-" + id)
                .deviceType(DeviceToken.DeviceType.IOS)
                .build();
    }

    /**
     * Web DeviceToken 객체를 생성한다.
     *
     * @param id     토큰 ID
     * @param userId 사용자 ID
     * @return Web 디바이스의 활성 DeviceToken 엔티티
     */
    public static DeviceToken createWebDeviceToken(Long id, Long userId) {
        return DeviceToken.builder()
                .id(id)
                .userId(userId)
                .token("web-token-" + id)
                .deviceType(DeviceToken.DeviceType.WEB)
                .build();
    }

    /**
     * 비활성화된 DeviceToken 객체를 생성한다.
     *
     * @param id     토큰 ID
     * @param userId 사용자 ID
     * @return 비활성 상태의 DeviceToken 엔티티
     */
    public static DeviceToken createInactiveDeviceToken(Long id, Long userId) {
        DeviceToken deviceToken = DeviceToken.builder()
                .id(id)
                .userId(userId)
                .token("inactive-token-" + id)
                .deviceType(DEFAULT_DEVICE_TYPE)
                .build();
        deviceToken.deactivate();
        return deviceToken;
    }

    /**
     * 빌더 스타일로 DeviceToken 생성을 시작한다.
     *
     * @return DeviceTokenBuilder 인스턴스
     */
    public static DeviceTokenBuilder builder() {
        return new DeviceTokenBuilder();
    }

    /**
     * DeviceToken 테스트 빌더.
     */
    public static class DeviceTokenBuilder {
        private Long id = DEFAULT_ID;
        private Long userId = DEFAULT_USER_ID;
        private String token = DEFAULT_TOKEN;
        private DeviceToken.DeviceType deviceType = DEFAULT_DEVICE_TYPE;
        private boolean active = true;

        /**
         * 토큰 ID를 설정한다.
         *
         * @param id 토큰 ID
         * @return 빌더
         */
        public DeviceTokenBuilder id(Long id) {
            this.id = id;
            return this;
        }

        /**
         * 사용자 ID를 설정한다.
         *
         * @param userId 사용자 ID
         * @return 빌더
         */
        public DeviceTokenBuilder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        /**
         * 토큰 값을 설정한다.
         *
         * @param token 토큰 값
         * @return 빌더
         */
        public DeviceTokenBuilder token(String token) {
            this.token = token;
            return this;
        }

        /**
         * 디바이스 유형을 설정한다.
         *
         * @param deviceType 디바이스 유형
         * @return 빌더
         */
        public DeviceTokenBuilder deviceType(DeviceToken.DeviceType deviceType) {
            this.deviceType = deviceType;
            return this;
        }

        /**
         * 활성화 여부를 설정한다.
         *
         * @param active 활성화 여부
         * @return 빌더
         */
        public DeviceTokenBuilder active(boolean active) {
            this.active = active;
            return this;
        }

        /**
         * DeviceToken 객체를 생성한다.
         *
         * @return 생성된 DeviceToken 엔티티
         */
        public DeviceToken build() {
            DeviceToken deviceToken = DeviceToken.builder()
                    .id(id)
                    .userId(userId)
                    .token(token)
                    .deviceType(deviceType)
                    .active(active)
                    .build();
            return deviceToken;
        }
    }
}
