package com.cotalk.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PasswordResetToken 엔티티")
class PasswordResetTokenTest {

    @Nested
    @DisplayName("create 메서드")
    class Create {

        @Test
        @DisplayName("사용자 ID, 이메일, 만료 시간으로 토큰을 생성할 수 있다")
        void should_createToken_when_validInputsProvided() {
            // given
            Long userId = 1L;
            String email = "test@example.com";
            int expirationMinutes = 30;

            // when
            PasswordResetToken token = PasswordResetToken.create(userId, email, expirationMinutes);

            // then
            assertThat(token.getUserId()).isEqualTo(userId);
            assertThat(token.getEmail()).isEqualTo(email);
            assertThat(token.getToken()).isNotNull();
            assertThat(token.getToken()).isNotBlank();
            assertThat(token.getExpiresAt()).isAfter(LocalDateTime.now());
            assertThat(token.getUsedAt()).isNull();
        }

        @Test
        @DisplayName("생성된 토큰은 UUID 형식이다")
        void should_generateUuidToken_when_created() {
            // given
            Long userId = 1L;
            String email = "test@example.com";
            int expirationMinutes = 30;

            // when
            PasswordResetToken token = PasswordResetToken.create(userId, email, expirationMinutes);

            // then
            assertThat(token.getToken()).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        }

        @Test
        @DisplayName("생성될 때마다 다른 토큰이 생성된다")
        void should_generateUniqueToken_when_calledMultipleTimes() {
            // given
            Long userId = 1L;
            String email = "test@example.com";
            int expirationMinutes = 30;

            // when
            PasswordResetToken token1 = PasswordResetToken.create(userId, email, expirationMinutes);
            PasswordResetToken token2 = PasswordResetToken.create(userId, email, expirationMinutes);

            // then
            assertThat(token1.getToken()).isNotEqualTo(token2.getToken());
        }

        @Test
        @DisplayName("만료 시간이 올바르게 설정된다")
        void should_setExpirationTime_correctly() {
            // given
            Long userId = 1L;
            String email = "test@example.com";
            int expirationMinutes = 60;
            LocalDateTime beforeCreate = LocalDateTime.now();

            // when
            PasswordResetToken token = PasswordResetToken.create(userId, email, expirationMinutes);
            LocalDateTime afterCreate = LocalDateTime.now();

            // then
            assertThat(token.getExpiresAt())
                    .isAfterOrEqualTo(beforeCreate.plusMinutes(expirationMinutes))
                    .isBeforeOrEqualTo(afterCreate.plusMinutes(expirationMinutes));
        }
    }

    @Nested
    @DisplayName("isExpired 메서드")
    class IsExpired {

        @Test
        @DisplayName("만료 시간이 지나면 true를 반환한다")
        void should_returnTrue_when_expired() {
            // given
            PasswordResetToken token = PasswordResetToken.builder()
                    .token("test-token")
                    .userId(1L)
                    .email("test@example.com")
                    .expiresAt(LocalDateTime.now().minusMinutes(1))
                    .build();

            // when
            boolean result = token.isExpired();

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("만료 시간이 지나지 않으면 false를 반환한다")
        void should_returnFalse_when_notExpired() {
            // given
            PasswordResetToken token = PasswordResetToken.builder()
                    .token("test-token")
                    .userId(1L)
                    .email("test@example.com")
                    .expiresAt(LocalDateTime.now().plusMinutes(30))
                    .build();

            // when
            boolean result = token.isExpired();

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("isUsed 메서드")
    class IsUsed {

        @Test
        @DisplayName("사용된 적이 있으면 true를 반환한다")
        void should_returnTrue_when_used() {
            // given
            PasswordResetToken token = PasswordResetToken.builder()
                    .token("test-token")
                    .userId(1L)
                    .email("test@example.com")
                    .expiresAt(LocalDateTime.now().plusMinutes(30))
                    .usedAt(LocalDateTime.now())
                    .build();

            // when
            boolean result = token.isUsed();

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("사용된 적이 없으면 false를 반환한다")
        void should_returnFalse_when_notUsed() {
            // given
            PasswordResetToken token = PasswordResetToken.builder()
                    .token("test-token")
                    .userId(1L)
                    .email("test@example.com")
                    .expiresAt(LocalDateTime.now().plusMinutes(30))
                    .usedAt(null)
                    .build();

            // when
            boolean result = token.isUsed();

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("isValid 메서드")
    class IsValid {

        @Test
        @DisplayName("만료되지 않고 사용되지 않으면 true를 반환한다")
        void should_returnTrue_when_notExpiredAndNotUsed() {
            // given
            PasswordResetToken token = PasswordResetToken.builder()
                    .token("test-token")
                    .userId(1L)
                    .email("test@example.com")
                    .expiresAt(LocalDateTime.now().plusMinutes(30))
                    .usedAt(null)
                    .build();

            // when
            boolean result = token.isValid();

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("만료되면 false를 반환한다")
        void should_returnFalse_when_expired() {
            // given
            PasswordResetToken token = PasswordResetToken.builder()
                    .token("test-token")
                    .userId(1L)
                    .email("test@example.com")
                    .expiresAt(LocalDateTime.now().minusMinutes(1))
                    .usedAt(null)
                    .build();

            // when
            boolean result = token.isValid();

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("사용되었으면 false를 반환한다")
        void should_returnFalse_when_used() {
            // given
            PasswordResetToken token = PasswordResetToken.builder()
                    .token("test-token")
                    .userId(1L)
                    .email("test@example.com")
                    .expiresAt(LocalDateTime.now().plusMinutes(30))
                    .usedAt(LocalDateTime.now())
                    .build();

            // when
            boolean result = token.isValid();

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("만료되고 사용되었으면 false를 반환한다")
        void should_returnFalse_when_expiredAndUsed() {
            // given
            PasswordResetToken token = PasswordResetToken.builder()
                    .token("test-token")
                    .userId(1L)
                    .email("test@example.com")
                    .expiresAt(LocalDateTime.now().minusMinutes(1))
                    .usedAt(LocalDateTime.now().minusMinutes(2))
                    .build();

            // when
            boolean result = token.isValid();

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("markAsUsed 메서드")
    class MarkAsUsed {

        @Test
        @DisplayName("토큰을 사용됨으로 표시한다")
        void should_setUsedAt_when_markAsUsed() {
            // given
            PasswordResetToken token = PasswordResetToken.builder()
                    .token("test-token")
                    .userId(1L)
                    .email("test@example.com")
                    .expiresAt(LocalDateTime.now().plusMinutes(30))
                    .usedAt(null)
                    .build();

            assertThat(token.isUsed()).isFalse();

            // when
            token.markAsUsed();

            // then
            assertThat(token.isUsed()).isTrue();
            assertThat(token.getUsedAt()).isNotNull();
            assertThat(token.getUsedAt()).isBeforeOrEqualTo(LocalDateTime.now());
        }

        @Test
        @DisplayName("사용됨으로 표시 후 유효하지 않게 된다")
        void should_becomeInvalid_when_markedAsUsed() {
            // given
            PasswordResetToken token = PasswordResetToken.builder()
                    .token("test-token")
                    .userId(1L)
                    .email("test@example.com")
                    .expiresAt(LocalDateTime.now().plusMinutes(30))
                    .usedAt(null)
                    .build();

            assertThat(token.isValid()).isTrue();

            // when
            token.markAsUsed();

            // then
            assertThat(token.isValid()).isFalse();
        }
    }
}
