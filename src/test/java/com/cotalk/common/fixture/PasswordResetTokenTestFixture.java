package com.cotalk.common.fixture;

import com.cotalk.domain.entity.PasswordResetToken;
import com.cotalk.domain.model.Email;

import java.time.LocalDateTime;

/**
 * PasswordResetToken 엔티티 테스트 픽스처.
 * 테스트에서 반복적으로 사용되는 PasswordResetToken 객체 생성 메서드를 제공한다.
 *
 * @author seunggu.lee
 */
public class PasswordResetTokenTestFixture {

    private static final Long DEFAULT_USER_ID = 1L;
    private static final String DEFAULT_EMAIL = "test@example.com";
    private static final int DEFAULT_EXPIRATION_MINUTES = 30;
    private static final LocalDateTime DEFAULT_NOW = LocalDateTime.of(2026, 1, 1, 12, 0);

    /**
     * 기본값으로 유효한 PasswordResetToken을 생성한다.
     * {@link PasswordResetToken#create(Long, Email, int, LocalDateTime)} 팩토리 메서드를 사용한다.
     *
     * @return 유효한 PasswordResetToken 엔티티
     */
    public static PasswordResetToken createToken() {
        return createToken(DEFAULT_USER_ID, DEFAULT_EMAIL);
    }

    /**
     * 지정된 사용자 ID와 이메일로 유효한 PasswordResetToken을 생성한다.
     *
     * @param userId 사용자 ID
     * @param email  이메일 주소
     * @return 유효한 PasswordResetToken 엔티티
     */
    public static PasswordResetToken createToken(Long userId, String email) {
        return PasswordResetToken.create(userId, new Email(email), DEFAULT_EXPIRATION_MINUTES, DEFAULT_NOW);
    }

    /**
     * 6자리 인증 코드가 포함된 PasswordResetToken을 생성한다.
     *
     * @param userId 사용자 ID
     * @param email  이메일 주소
     * @return 인증 코드가 포함된 PasswordResetToken 엔티티
     */
    public static PasswordResetToken createTokenWithCode(Long userId, String email) {
        return PasswordResetToken.createWithCode(userId, new Email(email), DEFAULT_EXPIRATION_MINUTES, DEFAULT_NOW);
    }

    /**
     * 만료된 PasswordResetToken을 생성한다.
     *
     * @param userId 사용자 ID
     * @param email  이메일 주소
     * @return 만료된 PasswordResetToken 엔티티
     */
    public static PasswordResetToken createExpiredToken(Long userId, String email) {
        return PasswordResetToken.builder()
                .token("expired-token")
                .userId(userId)
                .email(new Email(email))
                .expiresAt(DEFAULT_NOW.minusHours(1))
                .build();
    }

    /**
     * 이미 사용된 PasswordResetToken을 생성한다.
     *
     * @param userId 사용자 ID
     * @param email  이메일 주소
     * @return 사용 완료된 PasswordResetToken 엔티티
     */
    public static PasswordResetToken createUsedToken(Long userId, String email) {
        PasswordResetToken token = PasswordResetToken.create(
                userId, new Email(email), DEFAULT_EXPIRATION_MINUTES, DEFAULT_NOW
        );
        token.markAsUsed(DEFAULT_NOW);
        return token;
    }

    /**
     * 빌더 스타일로 PasswordResetToken 생성을 시작한다.
     *
     * @return PasswordResetTokenBuilder 인스턴스
     */
    public static PasswordResetTokenBuilder builder() {
        return new PasswordResetTokenBuilder();
    }

    /**
     * PasswordResetToken 테스트 빌더.
     */
    public static class PasswordResetTokenBuilder {
        private Long id = null;
        private String token = "test-reset-token";
        private Long userId = DEFAULT_USER_ID;
        private String email = DEFAULT_EMAIL;
        private LocalDateTime expiresAt = DEFAULT_NOW.plusMinutes(DEFAULT_EXPIRATION_MINUTES);
        private LocalDateTime usedAt = null;
        private String verificationCode = null;

        /**
         * ID를 설정한다.
         *
         * @param id 토큰 ID
         * @return 빌더
         */
        public PasswordResetTokenBuilder id(Long id) {
            this.id = id;
            return this;
        }

        /**
         * 토큰 값을 설정한다.
         *
         * @param token 토큰 값
         * @return 빌더
         */
        public PasswordResetTokenBuilder token(String token) {
            this.token = token;
            return this;
        }

        /**
         * 사용자 ID를 설정한다.
         *
         * @param userId 사용자 ID
         * @return 빌더
         */
        public PasswordResetTokenBuilder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        /**
         * 이메일 주소를 설정한다.
         *
         * @param email 이메일 주소
         * @return 빌더
         */
        public PasswordResetTokenBuilder email(String email) {
            this.email = email;
            return this;
        }

        /**
         * 만료 시간을 설정한다.
         *
         * @param expiresAt 만료 시간
         * @return 빌더
         */
        public PasswordResetTokenBuilder expiresAt(LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        /**
         * 사용 시간을 설정한다.
         *
         * @param usedAt 사용 시간
         * @return 빌더
         */
        public PasswordResetTokenBuilder usedAt(LocalDateTime usedAt) {
            this.usedAt = usedAt;
            return this;
        }

        /**
         * 인증 코드를 설정한다.
         *
         * @param verificationCode 인증 코드
         * @return 빌더
         */
        public PasswordResetTokenBuilder verificationCode(String verificationCode) {
            this.verificationCode = verificationCode;
            return this;
        }

        /**
         * PasswordResetToken 객체를 생성한다.
         *
         * @return 생성된 PasswordResetToken 엔티티
         */
        public PasswordResetToken build() {
            return PasswordResetToken.builder()
                    .id(id)
                    .token(token)
                    .userId(userId)
                    .email(new Email(email))
                    .expiresAt(expiresAt)
                    .usedAt(usedAt)
                    .verificationCode(verificationCode)
                    .build();
        }
    }
}
