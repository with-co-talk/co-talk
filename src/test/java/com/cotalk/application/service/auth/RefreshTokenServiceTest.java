package com.cotalk.application.service.auth;

import com.cotalk.common.fixture.RefreshTokenTestFixture;
import com.cotalk.domain.entity.RefreshToken;
import com.cotalk.domain.exception.InvalidRefreshTokenException;
import com.cotalk.domain.port.inbound.auth.RefreshTokenUseCase.RefreshResult;
import com.cotalk.domain.port.outbound.AuthTokenPort;
import com.cotalk.domain.port.outbound.IdGenerator;
import com.cotalk.domain.port.outbound.RefreshTokenRepository;
import com.cotalk.domain.port.outbound.TimeProvider;
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
import static org.mockito.Mockito.never;
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
    private AuthTokenPort authTokenPort;

    @Mock
    private IdGenerator idGenerator;

    @Mock
    private TimeProvider timeProvider;

    private RefreshTokenService refreshTokenService;

    private static final long REFRESH_TOKEN_EXPIRATION_DAYS = 30;
    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 1, 1, 12, 0);

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(
                refreshTokenRepository,
                authTokenPort,
                idGenerator,
                timeProvider,
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
            given(timeProvider.now()).willReturn(FIXED_NOW);

            // when
            String token = refreshTokenService.createRefreshToken(userId);

            // then
            assertThat(token).isNotNull();

            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenRepository).save(captor.capture());

            RefreshToken savedToken = captor.getValue();
            assertThat(savedToken.getUserId()).isEqualTo(userId);
            assertThat(savedToken.getExpiresAt()).isEqualTo(FIXED_NOW.plusDays(REFRESH_TOKEN_EXPIRATION_DAYS));
        }

        @Test
        @DisplayName("기존 Refresh Token이 있으면 폐기하고 새로 생성한다")
        void should_RevokeExistingAndCreateNew_when_UserHasToken() {
            // given
            Long userId = 100L;
            Long tokenId = 1L;
            RefreshToken existingToken = RefreshTokenTestFixture.createRefreshToken(99L, userId, "old-token");

            given(refreshTokenRepository.findByUserIdAndRevokedFalse(userId))
                    .willReturn(Optional.of(existingToken));
            given(idGenerator.nextId()).willReturn(tokenId);
            given(refreshTokenRepository.save(any(RefreshToken.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));
            given(timeProvider.now()).willReturn(FIXED_NOW);

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
        @DisplayName("유효한 Refresh Token으로 새 Access Token과 Refresh Token을 발급받을 수 있다 (Token Rotation)")
        void should_IssueNewAccessTokenAndRefreshToken_when_ValidRefreshToken() {
            // given
            String refreshToken = "valid-refresh-token";
            Long userId = 100L;
            String newAccessToken = "new-access-token";

            RefreshToken storedToken = RefreshTokenTestFixture.createRefreshToken(1L, userId, refreshToken);

            given(refreshTokenRepository.findByToken(refreshToken))
                    .willReturn(Optional.of(storedToken));
            given(authTokenPort.generateAccessToken(userId)).willReturn(newAccessToken);
            given(idGenerator.nextId()).willReturn(2L);
            given(refreshTokenRepository.save(any(RefreshToken.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));
            given(timeProvider.now()).willReturn(FIXED_NOW);

            // when
            RefreshResult result = refreshTokenService.refreshAccessToken(refreshToken);

            // then
            assertThat(result.accessToken()).isEqualTo(newAccessToken);
            assertThat(result.refreshToken()).isNotNull();
            assertThat(storedToken.isRevoked()).isTrue();
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
            RefreshToken storedToken = RefreshTokenTestFixture.builder()
                    .id(1L)
                    .userId(100L)
                    .token(expiredToken)
                    .expiresAt(FIXED_NOW.minusHours(1))
                    .build();

            given(refreshTokenRepository.findByToken(expiredToken))
                    .willReturn(Optional.of(storedToken));
            given(timeProvider.now()).willReturn(FIXED_NOW);

            // when & then
            assertThatThrownBy(() -> refreshTokenService.refreshAccessToken(expiredToken))
                    .isInstanceOf(InvalidRefreshTokenException.class);
        }

        @Test
        @DisplayName("폐기된 Refresh Token이면 예외가 발생한다")
        void should_ThrowException_when_TokenRevoked() {
            // given
            String revokedToken = "revoked-token";
            RefreshToken storedToken = RefreshTokenTestFixture.builder()
                    .id(1L)
                    .userId(100L)
                    .token(revokedToken)
                    .revoked(true)
                    .build();

            given(refreshTokenRepository.findByToken(revokedToken))
                    .willReturn(Optional.of(storedToken));
            given(timeProvider.now()).willReturn(FIXED_NOW);

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
            RefreshToken refreshToken = RefreshTokenTestFixture.createRefreshToken(1L, 100L, token);

            given(refreshTokenRepository.findByToken(token))
                    .willReturn(Optional.of(refreshToken));
            given(refreshTokenRepository.save(any(RefreshToken.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            refreshTokenService.revokeToken(token);

            // then
            assertThat(refreshToken.isRevoked()).isTrue();
            verify(refreshTokenRepository).save(refreshToken);
        }

        @Test
        @DisplayName("존재하지 않는 토큰을 폐기하려고 하면 아무 작업도 하지 않는다")
        void should_DoNothing_when_TokenNotFound() {
            // given
            String token = "nonexistent-token";
            given(refreshTokenRepository.findByToken(token))
                    .willReturn(Optional.empty());

            // when
            refreshTokenService.revokeToken(token);

            // then - 예외가 발생하지 않고, save가 호출되지 않음
            verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
        }
    }
}
