package com.cotalk.common.fixture;

import com.cotalk.domain.entity.EmailVerificationToken;
import com.cotalk.domain.model.Email;

import java.time.LocalDateTime;

/**
 * EmailVerificationToken 엔티티 테스트 픽스처.
 * 테스트에서 반복적으로 사용되는 EmailVerificationToken 객체 생성 메서드를 제공한다.
 *
 * @author seunggu.lee
 */
public class EmailVerificationTokenTestFixture {

    private static final Long DEFAULT_USER_ID = 1L;
    private static final String DEFAULT_EMAIL = "test@example.com";
    private static final int DEFAULT_EXPIRATION_MINUTES = 30;
    private static final LocalDateTime DEFAULT_NOW = LocalDateTime.of(2026, 1, 1, 12, 0);

    /**
     * 기본값으로 유효한 EmailVerificationToken을 생성한다.
     * {@link EmailVerificationToken#create(Long, Email, int, LocalDateTime)} 팩토리 메서드를 사용한다.
     *
     * @return 유효한 EmailVerificationToken 엔티티
     */
    public static EmailVerificationToken createToken() {
        return createToken(DEFAULT_USER_ID, DEFAULT_EMAIL);
    }

    /**
     * 지정된 사용자 ID와 이메일로 유효한 EmailVerificationToken을 생성한다.
     *
     * @param userId 사용자 ID
     * @param email  이메일 주소
     * @return 유효한 EmailVerificationToken 엔티티
     */
    public static EmailVerificationToken createToken(Long userId, String email) {
        return EmailVerificationToken.create(userId, new Email(email), DEFAULT_EXPIRATION_MINUTES, DEFAULT_NOW);
    }

    /**
     * 만료된 EmailVerificationToken을 생성한다.
     *
     * @param userId 사용자 ID
     * @param email  이메일 주소
     * @return 만료된 EmailVerificationToken 엔티티
     */
    public static EmailVerificationToken createExpiredToken(Long userId, String email) {
        return EmailVerificationToken.builder()
                .token("expired-token")
                .userId(userId)
                .email(new Email(email))
                .expiresAt(DEFAULT_NOW.minusHours(1))
                .build();
    }

    /**
     * 이미 인증된 EmailVerificationToken을 생성한다.
     *
     * @param userId 사용자 ID
     * @param email  이메일 주소
     * @return 인증 완료된 EmailVerificationToken 엔티티
     */
    public static EmailVerificationToken createVerifiedToken(Long userId, String email) {
        EmailVerificationToken token = EmailVerificationToken.create(
                userId, new Email(email), DEFAULT_EXPIRATION_MINUTES, DEFAULT_NOW
        );
        token.markAsVerified(DEFAULT_NOW);
        return token;
    }

    /**
     * 빌더 스타일로 EmailVerificationToken 생성을 시작한다.
     *
     * @return EmailVerificationTokenBuilder 인스턴스
     */
    public static EmailVerificationTokenBuilder builder() {
        return new EmailVerificationTokenBuilder();
    }

    /**
     * EmailVerificationToken 테스트 빌더.
     */
    public static class EmailVerificationTokenBuilder {
        private Long id = null;
        private String token = "test-verification-token";
        private Long userId = DEFAULT_USER_ID;
        private String email = DEFAULT_EMAIL;
        private LocalDateTime expiresAt = DEFAULT_NOW.plusMinutes(DEFAULT_EXPIRATION_MINUTES);
        private LocalDateTime verifiedAt = null;

        /**
         * ID를 설정한다.
         *
         * @param id 토큰 ID
         * @return 빌더
         */
        public EmailVerificationTokenBuilder id(Long id) {
            this.id = id;
            return this;
        }

        /**
         * 토큰 값을 설정한다.
         *
         * @param token 토큰 값
         * @return 빌더
         */
        public EmailVerificationTokenBuilder token(String token) {
            this.token = token;
            return this;
        }

        /**
         * 사용자 ID를 설정한다.
         *
         * @param userId 사용자 ID
         * @return 빌더
         */
        public EmailVerificationTokenBuilder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        /**
         * 이메일 주소를 설정한다.
         *
         * @param email 이메일 주소
         * @return 빌더
         */
        public EmailVerificationTokenBuilder email(String email) {
            this.email = email;
            return this;
        }

        /**
         * 만료 시간을 설정한다.
         *
         * @param expiresAt 만료 시간
         * @return 빌더
         */
        public EmailVerificationTokenBuilder expiresAt(LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        /**
         * 인증 완료 시간을 설정한다.
         *
         * @param verifiedAt 인증 완료 시간
         * @return 빌더
         */
        public EmailVerificationTokenBuilder verifiedAt(LocalDateTime verifiedAt) {
            this.verifiedAt = verifiedAt;
            return this;
        }

        /**
         * EmailVerificationToken 객체를 생성한다.
         *
         * @return 생성된 EmailVerificationToken 엔티티
         */
        public EmailVerificationToken build() {
            return EmailVerificationToken.builder()
                    .id(id)
                    .token(token)
                    .userId(userId)
                    .email(new Email(email))
                    .expiresAt(expiresAt)
                    .verifiedAt(verifiedAt)
                    .build();
        }
    }
}
