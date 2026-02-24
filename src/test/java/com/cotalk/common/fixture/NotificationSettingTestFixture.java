package com.cotalk.common.fixture;

import com.cotalk.domain.entity.NotificationSetting;

/**
 * NotificationSetting 엔티티 테스트 픽스처.
 * 테스트에서 반복적으로 사용되는 NotificationSetting 객체 생성 메서드를 제공한다.
 *
 * @author seunggu.lee
 */
public class NotificationSettingTestFixture {

    private static final Long DEFAULT_ID = 1L;
    private static final Long DEFAULT_USER_ID = 100L;

    /**
     * 기본값(모든 알림 활성화)으로 NotificationSetting 객체를 생성한다.
     *
     * @return 기본 알림 설정 엔티티
     */
    public static NotificationSetting createDefaultSetting() {
        return createDefaultSetting(DEFAULT_ID, DEFAULT_USER_ID);
    }

    /**
     * 지정된 ID와 사용자 ID로 기본 NotificationSetting 객체를 생성한다.
     *
     * @param id     설정 ID
     * @param userId 사용자 ID
     * @return 기본 알림 설정 엔티티
     */
    public static NotificationSetting createDefaultSetting(Long id, Long userId) {
        return NotificationSetting.builder()
                .id(id)
                .userId(userId)
                .build();
    }

    /**
     * 방해 금지 모드가 활성화된 NotificationSetting 객체를 생성한다.
     *
     * @param id     설정 ID
     * @param userId 사용자 ID
     * @param start  방해 금지 시작 시간 (HH:mm 형식)
     * @param end    방해 금지 종료 시간 (HH:mm 형식)
     * @return 방해 금지 모드가 활성화된 알림 설정 엔티티
     */
    public static NotificationSetting createWithDoNotDisturb(Long id, Long userId, String start, String end) {
        return NotificationSetting.builder()
                .id(id)
                .userId(userId)
                .doNotDisturbEnabled(true)
                .doNotDisturbStart(start)
                .doNotDisturbEnd(end)
                .build();
    }

    /**
     * 모든 알림이 비활성화된 NotificationSetting 객체를 생성한다.
     *
     * @param id     설정 ID
     * @param userId 사용자 ID
     * @return 모든 알림이 비활성화된 설정 엔티티
     */
    public static NotificationSetting createAllDisabled(Long id, Long userId) {
        return NotificationSetting.builder()
                .id(id)
                .userId(userId)
                .messageNotification(false)
                .friendRequestNotification(false)
                .groupInviteNotification(false)
                .soundEnabled(false)
                .vibrationEnabled(false)
                .build();
    }

    /**
     * 빌더 스타일로 NotificationSetting 생성을 시작한다.
     *
     * @return NotificationSettingBuilder 인스턴스
     */
    public static NotificationSettingBuilder builder() {
        return new NotificationSettingBuilder();
    }

    /**
     * NotificationSetting 테스트 빌더.
     */
    public static class NotificationSettingBuilder {
        private Long id = DEFAULT_ID;
        private Long userId = DEFAULT_USER_ID;
        private boolean messageNotification = true;
        private boolean friendRequestNotification = true;
        private boolean groupInviteNotification = true;
        private String notificationPreviewMode = "NAME_AND_MESSAGE";
        private boolean soundEnabled = true;
        private boolean vibrationEnabled = true;
        private boolean doNotDisturbEnabled = false;
        private String doNotDisturbStart = null;
        private String doNotDisturbEnd = null;

        /**
         * 설정 ID를 설정한다.
         *
         * @param id 설정 ID
         * @return 빌더
         */
        public NotificationSettingBuilder id(Long id) {
            this.id = id;
            return this;
        }

        /**
         * 사용자 ID를 설정한다.
         *
         * @param userId 사용자 ID
         * @return 빌더
         */
        public NotificationSettingBuilder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        /**
         * 메시지 알림 활성화 여부를 설정한다.
         *
         * @param messageNotification 메시지 알림 활성화 여부
         * @return 빌더
         */
        public NotificationSettingBuilder messageNotification(boolean messageNotification) {
            this.messageNotification = messageNotification;
            return this;
        }

        /**
         * 친구 요청 알림 활성화 여부를 설정한다.
         *
         * @param friendRequestNotification 친구 요청 알림 활성화 여부
         * @return 빌더
         */
        public NotificationSettingBuilder friendRequestNotification(boolean friendRequestNotification) {
            this.friendRequestNotification = friendRequestNotification;
            return this;
        }

        /**
         * 그룹 초대 알림 활성화 여부를 설정한다.
         *
         * @param groupInviteNotification 그룹 초대 알림 활성화 여부
         * @return 빌더
         */
        public NotificationSettingBuilder groupInviteNotification(boolean groupInviteNotification) {
            this.groupInviteNotification = groupInviteNotification;
            return this;
        }

        /**
         * 알림 미리보기 모드를 설정한다.
         *
         * @param notificationPreviewMode 미리보기 모드
         * @return 빌더
         */
        public NotificationSettingBuilder notificationPreviewMode(String notificationPreviewMode) {
            this.notificationPreviewMode = notificationPreviewMode;
            return this;
        }

        /**
         * 알림 소리 활성화 여부를 설정한다.
         *
         * @param soundEnabled 소리 활성화 여부
         * @return 빌더
         */
        public NotificationSettingBuilder soundEnabled(boolean soundEnabled) {
            this.soundEnabled = soundEnabled;
            return this;
        }

        /**
         * 알림 진동 활성화 여부를 설정한다.
         *
         * @param vibrationEnabled 진동 활성화 여부
         * @return 빌더
         */
        public NotificationSettingBuilder vibrationEnabled(boolean vibrationEnabled) {
            this.vibrationEnabled = vibrationEnabled;
            return this;
        }

        /**
         * 방해 금지 모드 활성화 여부를 설정한다.
         *
         * @param doNotDisturbEnabled 방해 금지 모드 활성화 여부
         * @return 빌더
         */
        public NotificationSettingBuilder doNotDisturbEnabled(boolean doNotDisturbEnabled) {
            this.doNotDisturbEnabled = doNotDisturbEnabled;
            return this;
        }

        /**
         * 방해 금지 시작 시간을 설정한다.
         *
         * @param doNotDisturbStart 방해 금지 시작 시간 (HH:mm 형식)
         * @return 빌더
         */
        public NotificationSettingBuilder doNotDisturbStart(String doNotDisturbStart) {
            this.doNotDisturbStart = doNotDisturbStart;
            return this;
        }

        /**
         * 방해 금지 종료 시간을 설정한다.
         *
         * @param doNotDisturbEnd 방해 금지 종료 시간 (HH:mm 형식)
         * @return 빌더
         */
        public NotificationSettingBuilder doNotDisturbEnd(String doNotDisturbEnd) {
            this.doNotDisturbEnd = doNotDisturbEnd;
            return this;
        }

        /**
         * NotificationSetting 객체를 생성한다.
         *
         * @return 생성된 NotificationSetting 엔티티
         */
        public NotificationSetting build() {
            return NotificationSetting.builder()
                    .id(id)
                    .userId(userId)
                    .messageNotification(messageNotification)
                    .friendRequestNotification(friendRequestNotification)
                    .groupInviteNotification(groupInviteNotification)
                    .notificationPreviewMode(notificationPreviewMode)
                    .soundEnabled(soundEnabled)
                    .vibrationEnabled(vibrationEnabled)
                    .doNotDisturbEnabled(doNotDisturbEnabled)
                    .doNotDisturbStart(doNotDisturbStart)
                    .doNotDisturbEnd(doNotDisturbEnd)
                    .build();
        }
    }
}
