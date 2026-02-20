package com.cotalk.common.fixture;

import com.cotalk.domain.entity.RefreshToken;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * RefreshToken 엔티티 테스트 픽스처.
 * 테스트에서 반복적으로 사용되는 RefreshToken 객체 생성 메서드를 제공한다.
 *
 * @author seunggu.lee
 */
public class RefreshTokenTestFixture {

    private static final Long DEFAULT_ID = 1L;
    private static final Long DEFAULT_USER_ID = 100L;
    private static final String DEFAULT_TOKEN = "test-refresh-token-value";

    /**
     * 기본값으로 유효한 RefreshToken 객체를 생성한다.
     * 만료 시간은 현재로부터 7일 뒤로 설정된다.
     *
     * @return 유효한 RefreshToken 엔티티
     */
    public static RefreshToken createRefreshToken() {
        return createRefreshToken(DEFAULT_ID, DEFAULT_USER_ID);
    }

    /**
     * 지정된 ID와 사용자 ID로 유효한 RefreshToken 객체를 생성한다.
     *
     * @param id     토큰 ID
     * @param userId 사용자 ID
     * @return 유효한 RefreshToken 엔티티
     */
    public static RefreshToken createRefreshToken(Long id, Long userId) {
        return RefreshToken.builder()
                .id(id)
                .userId(userId)
                .token(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
    }

    /**
     * 지정된 토큰 값으로 RefreshToken 객체를 생성한다.
     *
     * @param id     토큰 ID
     * @param userId 사용자 ID
     * @param token  토큰 값
     * @return 유효한 RefreshToken 엔티티
     */
    public static RefreshToken createRefreshToken(Long id, Long userId, String token) {
        return RefreshToken.builder()
                .id(id)
                .userId(userId)
                .token(token)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
    }

    /**
     * 만료된 RefreshToken 객체를 생성한다.
     *
     * @param id     토큰 ID
     * @param userId 사용자 ID
     * @return 만료된 RefreshToken 엔티티
     */
    public static RefreshToken createExpiredRefreshToken(Long id, Long userId) {
        return RefreshToken.builder()
                .id(id)
                .userId(userId)
                .token(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().minusHours(1))
                .build();
    }

    /**
     * 폐기된 RefreshToken 객체를 생성한다.
     *
     * @param id     토큰 ID
     * @param userId 사용자 ID
     * @return 폐기된 RefreshToken 엔티티
     */
    public static RefreshToken createRevokedRefreshToken(Long id, Long userId) {
        RefreshToken token = RefreshToken.builder()
                .id(id)
                .userId(userId)
                .token(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        token.revoke();
        return token;
    }

    /**
     * 빌더 스타일로 RefreshToken 생성을 시작한다.
     *
     * @return RefreshTokenBuilder 인스턴스
     */
    public static RefreshTokenBuilder builder() {
        return new RefreshTokenBuilder();
    }

    /**
     * RefreshToken 테스트 빌더.
     */
    public static class RefreshTokenBuilder {
        private Long id = DEFAULT_ID;
        private Long userId = DEFAULT_USER_ID;
        private String token = DEFAULT_TOKEN;
        private LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);
        private boolean revoked = false;

        /**
         * 토큰 ID를 설정한다.
         *
         * @param id 토큰 ID
         * @return 빌더
         */
        public RefreshTokenBuilder id(Long id) {
            this.id = id;
            return this;
        }

        /**
         * 사용자 ID를 설정한다.
         *
         * @param userId 사용자 ID
         * @return 빌더
         */
        public RefreshTokenBuilder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        /**
         * 토큰 값을 설정한다.
         *
         * @param token 토큰 값
         * @return 빌더
         */
        public RefreshTokenBuilder token(String token) {
            this.token = token;
            return this;
        }

        /**
         * 만료 시간을 설정한다.
         *
         * @param expiresAt 만료 시간
         * @return 빌더
         */
        public RefreshTokenBuilder expiresAt(LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        /**
         * 폐기 여부를 설정한다.
         *
         * @param revoked 폐기 여부
         * @return 빌더
         */
        public RefreshTokenBuilder revoked(boolean revoked) {
            this.revoked = revoked;
            return this;
        }

        /**
         * RefreshToken 객체를 생성한다.
         *
         * @return 생성된 RefreshToken 엔티티
         */
        public RefreshToken build() {
            RefreshToken refreshToken = RefreshToken.builder()
                    .id(id)
                    .userId(userId)
                    .token(token)
                    .expiresAt(expiresAt)
                    .revoked(revoked)
                    .build();
            return refreshToken;
        }
    }
}
