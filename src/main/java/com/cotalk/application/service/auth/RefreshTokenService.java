package com.cotalk.application.service.auth;

import com.cotalk.domain.entity.RefreshToken;
import com.cotalk.domain.exception.InvalidRefreshTokenException;
import com.cotalk.domain.port.inbound.auth.RefreshTokenUseCase;
import com.cotalk.domain.port.outbound.AuthTokenPort;
import com.cotalk.domain.port.outbound.IdGenerator;
import com.cotalk.domain.port.outbound.RefreshTokenRepository;
import com.cotalk.domain.port.outbound.TimeProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Refresh Token 서비스.
 * Refresh Token의 생성, 갱신, 폐기 로직을 구현한다.
 *
 * @author seunggu.lee
 */
@Slf4j
@Transactional
public class RefreshTokenService implements RefreshTokenUseCase {

    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthTokenPort authTokenPort;
    private final IdGenerator idGenerator;
    private final TimeProvider timeProvider;
    private final long refreshTokenExpirationDays;

    /**
     * RefreshTokenService 생성자.
     *
     * @param refreshTokenRepository Refresh Token 저장소
     * @param authTokenPort Access 토큰 발급 포트
     * @param idGenerator ID 생성기
     * @param timeProvider 시간 제공자
     * @param refreshTokenExpirationDays 리프레시 토큰 만료 일수
     */
    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            AuthTokenPort authTokenPort,
            IdGenerator idGenerator,
            TimeProvider timeProvider,
            long refreshTokenExpirationDays) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.authTokenPort = authTokenPort;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
        this.refreshTokenExpirationDays = refreshTokenExpirationDays;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String createRefreshToken(Long userId) {
        refreshTokenRepository.findByUserIdAndRevokedFalse(userId)
                .ifPresent(this::revokeAndSave);
        return issueNewToken(userId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RefreshResult refreshAccessToken(String refreshToken) {
        RefreshToken storedToken = findAndValidateToken(refreshToken);

        revokeAndSave(storedToken);
        String newAccessToken = authTokenPort.generateAccessToken(storedToken.getUserId());
        String newRefreshToken = issueNewToken(storedToken.getUserId());

        log.debug("Access Token 및 Refresh Token 갱신 완료 (Token Rotation)");
        return new RefreshResult(newAccessToken, newRefreshToken);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void revokeToken(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken)
                .ifPresent(this::revokeAndSave);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void revokeAllTokensByUserId(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
        log.debug("사용자의 모든 Refresh Token 폐기 완료");
    }

    /**
     * Refresh Token을 조회하고 유효성을 검증한다.
     *
     * @param refreshToken 토큰 값
     * @return 유효한 Refresh Token 엔티티
     * @throws InvalidRefreshTokenException 토큰이 존재하지 않거나 유효하지 않은 경우
     */
    private RefreshToken findAndValidateToken(String refreshToken) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 Refresh Token 사용 시도");
                    return new InvalidRefreshTokenException();
                });

        if (!storedToken.isValid(timeProvider.now())) {
            log.warn("유효하지 않은 Refresh Token 사용 시도 - expired: {}, revoked: {}",
                    storedToken.isExpired(timeProvider.now()), storedToken.isRevoked());
            throw new InvalidRefreshTokenException();
        }

        return storedToken;
    }

    /**
     * 토큰을 폐기하고 저장한다.
     *
     * @param token 폐기할 Refresh Token
     */
    private void revokeAndSave(RefreshToken token) {
        token.revoke();
        refreshTokenRepository.save(token);
        log.debug("Refresh Token 폐기 완료");
    }

    /**
     * 새로운 Refresh Token을 생성하여 저장한다.
     *
     * @param userId 사용자 ID
     * @return 생성된 토큰 값
     */
    private String issueNewToken(Long userId) {
        String tokenValue = generateTokenValue();
        LocalDateTime expiresAt = timeProvider.now().plusDays(refreshTokenExpirationDays);

        RefreshToken refreshToken = RefreshToken.builder()
                .id(idGenerator.nextId())
                .userId(userId)
                .token(tokenValue)
                .expiresAt(expiresAt)
                .build();

        refreshTokenRepository.save(refreshToken);
        log.debug("Refresh Token 생성 완료");
        return tokenValue;
    }

    /**
     * 고유한 토큰 값을 생성한다.
     *
     * @return 생성된 토큰 값
     */
    private String generateTokenValue() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
