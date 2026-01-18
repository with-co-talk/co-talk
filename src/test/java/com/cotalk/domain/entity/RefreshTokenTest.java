package com.cotalk.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * RefreshToken 엔티티 테스트.
 *
 * @author seunggu.lee
 */
@DisplayName("RefreshToken")
class RefreshTokenTest {

    @Nested
    @DisplayName("생성")
    class Creation {

        @Test
        @DisplayName("유효한 정보로 RefreshToken을 생성할 수 있다")
        void should_CreateRefreshToken_when_ValidData() {
            // given
            Long id = 1L;
            Long userId = 100L;
            String token = "refresh-token-value";
            LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);

            // when
            RefreshToken refreshToken = RefreshToken.builder()
                    .id(id)
                    .userId(userId)
                    .token(token)
                    .expiresAt(expiresAt)
                    .build();

            // then
            assertThat(refreshToken.getId()).isEqualTo(id);
            assertThat(refreshToken.getUserId()).isEqualTo(userId);
            assertThat(refreshToken.getToken()).isEqualTo(token);
            assertThat(refreshToken.getExpiresAt()).isEqualTo(expiresAt);
            assertThat(refreshToken.isRevoked()).isFalse();
        }
    }

    @Nested
    @DisplayName("만료 확인")
    class ExpirationCheck {

        @Test
        @DisplayName("만료 시간이 지나면 isExpired가 true를 반환한다")
        void should_ReturnTrue_when_TokenExpired() {
            // given
            RefreshToken refreshToken = RefreshToken.builder()
                    .id(1L)
                    .userId(100L)
                    .token("token")
                    .expiresAt(LocalDateTime.now().minusHours(1))
                    .build();

            // when & then
            assertThat(refreshToken.isExpired()).isTrue();
        }

        @Test
        @DisplayName("만료 시간이 지나지 않으면 isExpired가 false를 반환한다")
        void should_ReturnFalse_when_TokenNotExpired() {
            // given
            RefreshToken refreshToken = RefreshToken.builder()
                    .id(1L)
                    .userId(100L)
                    .token("token")
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .build();

            // when & then
            assertThat(refreshToken.isExpired()).isFalse();
        }
    }

    @Nested
    @DisplayName("폐기")
    class Revocation {

        @Test
        @DisplayName("토큰을 폐기하면 isRevoked가 true가 된다")
        void should_SetRevokedTrue_when_Revoke() {
            // given
            RefreshToken refreshToken = RefreshToken.builder()
                    .id(1L)
                    .userId(100L)
                    .token("token")
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .build();

            // when
            refreshToken.revoke();

            // then
            assertThat(refreshToken.isRevoked()).isTrue();
        }
    }

    @Nested
    @DisplayName("유효성 확인")
    class ValidityCheck {

        @Test
        @DisplayName("만료되지 않고 폐기되지 않은 토큰은 유효하다")
        void should_ReturnTrue_when_TokenValid() {
            // given
            RefreshToken refreshToken = RefreshToken.builder()
                    .id(1L)
                    .userId(100L)
                    .token("token")
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .build();

            // when & then
            assertThat(refreshToken.isValid()).isTrue();
        }

        @Test
        @DisplayName("만료된 토큰은 유효하지 않다")
        void should_ReturnFalse_when_TokenExpired() {
            // given
            RefreshToken refreshToken = RefreshToken.builder()
                    .id(1L)
                    .userId(100L)
                    .token("token")
                    .expiresAt(LocalDateTime.now().minusHours(1))
                    .build();

            // when & then
            assertThat(refreshToken.isValid()).isFalse();
        }

        @Test
        @DisplayName("폐기된 토큰은 유효하지 않다")
        void should_ReturnFalse_when_TokenRevoked() {
            // given
            RefreshToken refreshToken = RefreshToken.builder()
                    .id(1L)
                    .userId(100L)
                    .token("token")
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .build();
            refreshToken.revoke();

            // when & then
            assertThat(refreshToken.isValid()).isFalse();
        }
    }
}
