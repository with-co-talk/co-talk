package com.cotalk.common.fixture;

import com.cotalk.domain.entity.ProfileHistory;
import com.cotalk.domain.entity.ProfileHistoryType;

/**
 * ProfileHistory 엔티티 테스트 픽스처.
 * 테스트에서 반복적으로 사용되는 ProfileHistory 객체 생성 메서드를 제공한다.
 *
 * @author seunggu.lee
 */
public class ProfileHistoryTestFixture {

    private static final Long DEFAULT_ID = 1L;
    private static final Long DEFAULT_USER_ID = 1L;
    private static final String DEFAULT_URL = "https://example.com/avatar.png";

    /**
     * 프로필 사진(AVATAR) 이력을 생성한다.
     *
     * @param id     이력 ID
     * @param userId 사용자 ID
     * @param url    이미지 URL
     * @return AVATAR 타입의 ProfileHistory 엔티티
     */
    public static ProfileHistory createAvatarHistory(Long id, Long userId, String url) {
        return ProfileHistory.builder()
                .id(id)
                .userId(userId)
                .type(ProfileHistoryType.AVATAR)
                .url(url)
                .build();
    }

    /**
     * 기본값으로 프로필 사진 이력을 생성한다.
     *
     * @return AVATAR 타입의 ProfileHistory 엔티티
     */
    public static ProfileHistory createAvatarHistory() {
        return createAvatarHistory(DEFAULT_ID, DEFAULT_USER_ID, DEFAULT_URL);
    }

    /**
     * 배경화면(BACKGROUND) 이력을 생성한다.
     *
     * @param id     이력 ID
     * @param userId 사용자 ID
     * @param url    배경화면 URL
     * @return BACKGROUND 타입의 ProfileHistory 엔티티
     */
    public static ProfileHistory createBackgroundHistory(Long id, Long userId, String url) {
        return ProfileHistory.builder()
                .id(id)
                .userId(userId)
                .type(ProfileHistoryType.BACKGROUND)
                .url(url)
                .build();
    }

    /**
     * 상태메시지(STATUS_MESSAGE) 이력을 생성한다.
     *
     * @param id      이력 ID
     * @param userId  사용자 ID
     * @param content 상태 메시지 내용
     * @return STATUS_MESSAGE 타입의 ProfileHistory 엔티티
     */
    public static ProfileHistory createStatusMessageHistory(Long id, Long userId, String content) {
        return ProfileHistory.builder()
                .id(id)
                .userId(userId)
                .type(ProfileHistoryType.STATUS_MESSAGE)
                .content(content)
                .build();
    }

    /**
     * 현재 사용 중인 프로필 이력을 생성한다.
     *
     * @param id     이력 ID
     * @param userId 사용자 ID
     * @param type   프로필 이력 유형
     * @param url    URL (STATUS_MESSAGE 타입에서는 null)
     * @return isCurrent=true인 ProfileHistory 엔티티
     */
    public static ProfileHistory createCurrentHistory(Long id, Long userId, ProfileHistoryType type, String url) {
        return ProfileHistory.builder()
                .id(id)
                .userId(userId)
                .type(type)
                .url(url)
                .isCurrent(true)
                .build();
    }

    /**
     * 빌더 스타일로 ProfileHistory 생성을 시작한다.
     *
     * @return ProfileHistoryBuilder 인스턴스
     */
    public static ProfileHistoryBuilder builder() {
        return new ProfileHistoryBuilder();
    }

    /**
     * ProfileHistory 테스트 빌더.
     */
    public static class ProfileHistoryBuilder {
        private Long id = DEFAULT_ID;
        private Long userId = DEFAULT_USER_ID;
        private ProfileHistoryType type = ProfileHistoryType.AVATAR;
        private String url = DEFAULT_URL;
        private String content = null;
        private boolean isPrivate = false;
        private boolean isCurrent = false;

        /**
         * 이력 ID를 설정한다.
         *
         * @param id 이력 ID
         * @return 빌더
         */
        public ProfileHistoryBuilder id(Long id) {
            this.id = id;
            return this;
        }

        /**
         * 사용자 ID를 설정한다.
         *
         * @param userId 사용자 ID
         * @return 빌더
         */
        public ProfileHistoryBuilder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        /**
         * 프로필 이력 유형을 설정한다.
         *
         * @param type 프로필 이력 유형
         * @return 빌더
         */
        public ProfileHistoryBuilder type(ProfileHistoryType type) {
            this.type = type;
            return this;
        }

        /**
         * URL을 설정한다.
         *
         * @param url URL
         * @return 빌더
         */
        public ProfileHistoryBuilder url(String url) {
            this.url = url;
            return this;
        }

        /**
         * 내용을 설정한다.
         *
         * @param content 내용
         * @return 빌더
         */
        public ProfileHistoryBuilder content(String content) {
            this.content = content;
            return this;
        }

        /**
         * 나만보기 여부를 설정한다.
         *
         * @param isPrivate 나만보기 여부
         * @return 빌더
         */
        public ProfileHistoryBuilder isPrivate(boolean isPrivate) {
            this.isPrivate = isPrivate;
            return this;
        }

        /**
         * 현재 사용 중 여부를 설정한다.
         *
         * @param isCurrent 현재 사용 중 여부
         * @return 빌더
         */
        public ProfileHistoryBuilder isCurrent(boolean isCurrent) {
            this.isCurrent = isCurrent;
            return this;
        }

        /**
         * ProfileHistory 객체를 생성한다.
         *
         * @return 생성된 ProfileHistory 엔티티
         */
        public ProfileHistory build() {
            return ProfileHistory.builder()
                    .id(id)
                    .userId(userId)
                    .type(type)
                    .url(url)
                    .content(content)
                    .isPrivate(isPrivate)
                    .isCurrent(isCurrent)
                    .build();
        }
    }
}
