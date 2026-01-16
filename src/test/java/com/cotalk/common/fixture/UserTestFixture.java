package com.cotalk.common.fixture;

import com.cotalk.domain.entity.User;

import java.time.LocalDateTime;

/**
 * User 엔티티 테스트 픽스처
 * 테스트에서 반복적으로 사용되는 User 객체 생성 메서드를 제공합니다.
 */
public class UserTestFixture {

    private static final String DEFAULT_EMAIL = "test@example.com";
    private static final String DEFAULT_PASSWORD_HASH = "$2a$10$hashedPassword";
    private static final String DEFAULT_NICKNAME = "테스트유저";
    private static final String DEFAULT_AVATAR_URL = "https://example.com/avatar.png";

    /**
     * 기본값으로 User 객체를 생성합니다.
     */
    public static User createUser() {
        return createUser(1L);
    }

    /**
     * 지정된 ID로 User 객체를 생성합니다.
     */
    public static User createUser(Long userId) {
        return createUser(userId, DEFAULT_EMAIL, DEFAULT_NICKNAME);
    }

    /**
     * ID와 이메일, 닉네임을 지정하여 User 객체를 생성합니다.
     */
    public static User createUser(Long userId, String email, String nickname) {
        return User.builder()
                .id(userId)
                .email(email)
                .passwordHash(DEFAULT_PASSWORD_HASH)
                .nickname(nickname)
                .avatarUrl(DEFAULT_AVATAR_URL)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 프로필 정보를 포함한 User 객체를 생성합니다.
     */
    public static User createUserWithProfile(Long userId, String nickname, String avatarUrl) {
        return User.builder()
                .id(userId)
                .email(DEFAULT_EMAIL)
                .passwordHash(DEFAULT_PASSWORD_HASH)
                .nickname(nickname)
                .avatarUrl(avatarUrl)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 여러 User 객체를 생성합니다.
     */
    public static User[] createUsers(int count) {
        User[] users = new User[count];
        for (int i = 0; i < count; i++) {
            users[i] = createUser(
                    (long) (i + 1),
                    "user" + (i + 1) + "@example.com",
                    "유저" + (i + 1)
            );
        }
        return users;
    }

    /**
     * 빌더 스타일로 User 생성을 시작합니다.
     */
    public static UserBuilder builder() {
        return new UserBuilder();
    }

    public static class UserBuilder {
        private Long id = 1L;
        private String email = DEFAULT_EMAIL;
        private String passwordHash = DEFAULT_PASSWORD_HASH;
        private String nickname = DEFAULT_NICKNAME;
        private String avatarUrl = DEFAULT_AVATAR_URL;
        private LocalDateTime lastActiveAt = null;

        public UserBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public UserBuilder email(String email) {
            this.email = email;
            return this;
        }

        public UserBuilder passwordHash(String passwordHash) {
            this.passwordHash = passwordHash;
            return this;
        }

        public UserBuilder nickname(String nickname) {
            this.nickname = nickname;
            return this;
        }

        public UserBuilder avatarUrl(String avatarUrl) {
            this.avatarUrl = avatarUrl;
            return this;
        }

        public UserBuilder lastActiveAt(LocalDateTime lastActiveAt) {
            this.lastActiveAt = lastActiveAt;
            return this;
        }

        public User build() {
            return User.builder()
                    .id(id)
                    .email(email)
                    .passwordHash(passwordHash)
                    .nickname(nickname)
                    .avatarUrl(avatarUrl)
                    .lastActiveAt(lastActiveAt)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
        }
    }
}
