package com.cotalk.application.service.auth;

import com.cotalk.domain.entity.RefreshToken;
import com.cotalk.domain.exception.InvalidRefreshTokenException;
import com.cotalk.domain.port.outbound.IdGenerator;
import com.cotalk.domain.port.outbound.RefreshTokenRepository;
import com.cotalk.infrastructure.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * RefreshTokenService 테스트.
 *
 * @author seunggu.lee
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenService")
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private IdGenerator idGenerator;

    private RefreshTokenService refreshTokenService;

    private static final long REFRESH_TOKEN_EXPIRATION_DAYS = 7;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(
                refreshTokenRepository,
                jwtTokenProvider,
                idGenerator,
                REFRESH_TOKEN_EXPIRATION_DAYS
        );
    }

    @Nested
    @DisplayName("Refresh Token 생성")
    class CreateRefreshToken {

        @Test
        @DisplayName("사용자 ID로 Refresh Token을 생성할 수 있다")
        void should_CreateRefreshToken_when_ValidUserId() {
            // given
            Long userId = 100L;
            Long tokenId = 1L;
            given(idGenerator.nextId()).willReturn(tokenId);
            given(refreshTokenRepository.save(any(RefreshToken.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            String token = refreshTokenService.createRefreshToken(userId);

            // then
            assertThat(token).isNotNull();

            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenRepository).save(captor.capture());

            RefreshToken savedToken = captor.getValue();
            assertThat(savedToken.getUserId()).isEqualTo(userId);
            assertThat(savedToken.getExpiresAt()).isAfter(LocalDateTime.now());
        }

        @Test
        @DisplayName("기존 Refresh Token이 있으면 폐기하고 새로 생성한다")
        void should_RevokeExistingAndCreateNew_when_UserHasToken() {
            // given
            Long userId = 100L;
            Long tokenId = 1L;
            RefreshToken existingToken = RefreshToken.builder()
                    .id(99L)
                    .userId(userId)
                    .token("old-token")
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .build();

            given(refreshTokenRepository.findByUserIdAndRevokedFalse(userId))
                    .willReturn(Optional.of(existingToken));
            given(idGenerator.nextId()).willReturn(tokenId);
            given(refreshTokenRepository.save(any(RefreshToken.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            refreshTokenService.createRefreshToken(userId);

            // then
            assertThat(existingToken.isRevoked()).isTrue();
        }
    }

    @Nested
    @DisplayName("Access Token 갱신")
    class RefreshAccessToken {

        @Test
        @DisplayName("유효한 Refresh Token으로 새 Access Token을 발급받을 수 있다")
        void should_IssueNewAccessToken_when_ValidRefreshToken() {
            // given
            String refreshToken = "valid-refresh-token";
            Long userId = 100L;
            String newAccessToken = "new-access-token";

            RefreshToken storedToken = RefreshToken.builder()
                    .id(1L)
                    .userId(userId)
                    .token(refreshToken)
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .build();

            given(refreshTokenRepository.findByToken(refreshToken))
                    .willReturn(Optional.of(storedToken));
            given(jwtTokenProvider.generateToken(userId)).willReturn(newAccessToken);

            // when
            String result = refreshTokenService.refreshAccessToken(refreshToken);

            // then
            assertThat(result).isEqualTo(newAccessToken);
        }

        @Test
        @DisplayName("존재하지 않는 Refresh Token이면 예외가 발생한다")
        void should_ThrowException_when_TokenNotFound() {
            // given
            String invalidToken = "invalid-token";
            given(refreshTokenRepository.findByToken(invalidToken))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> refreshTokenService.refreshAccessToken(invalidToken))
                    .isInstanceOf(InvalidRefreshTokenException.class);
        }

        @Test
        @DisplayName("만료된 Refresh Token이면 예외가 발생한다")
        void should_ThrowException_when_TokenExpired() {
            // given
            String expiredToken = "expired-token";
            RefreshToken storedToken = RefreshToken.builder()
                    .id(1L)
                    .userId(100L)
                    .token(expiredToken)
                    .expiresAt(LocalDateTime.now().minusHours(1))
                    .build();

            given(refreshTokenRepository.findByToken(expiredToken))
                    .willReturn(Optional.of(storedToken));

            // when & then
            assertThatThrownBy(() -> refreshTokenService.refreshAccessToken(expiredToken))
                    .isInstanceOf(InvalidRefreshTokenException.class);
        }

        @Test
        @DisplayName("폐기된 Refresh Token이면 예외가 발생한다")
        void should_ThrowException_when_TokenRevoked() {
            // given
            String revokedToken = "revoked-token";
            RefreshToken storedToken = RefreshToken.builder()
                    .id(1L)
                    .userId(100L)
                    .token(revokedToken)
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .build();
            storedToken.revoke();

            given(refreshTokenRepository.findByToken(revokedToken))
                    .willReturn(Optional.of(storedToken));

            // when & then
            assertThatThrownBy(() -> refreshTokenService.refreshAccessToken(revokedToken))
                    .isInstanceOf(InvalidRefreshTokenException.class);
        }
    }

    @Nested
    @DisplayName("Refresh Token 폐기")
    class RevokeRefreshToken {

        @Test
        @DisplayName("사용자의 모든 Refresh Token을 폐기할 수 있다")
        void should_RevokeAllTokens_when_ValidUserId() {
            // given
            Long userId = 100L;

            // when
            refreshTokenService.revokeAllTokensByUserId(userId);

            // then
            verify(refreshTokenRepository).revokeAllByUserId(userId);
        }

        @Test
        @DisplayName("특정 Refresh Token을 폐기할 수 있다")
        void should_RevokeToken_when_ValidToken() {
            // given
            String token = "valid-token";
            RefreshToken refreshToken = RefreshToken.builder()
                    .id(1L)
                    .userId(100L)
                    .token(token)
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .build();

            given(refreshTokenRepository.findByToken(token))
                    .willReturn(Optional.of(refreshToken));

            // when
            refreshTokenService.revokeToken(token);

            // then
            assertThat(refreshToken.isRevoked()).isTrue();
        }
    }
}
