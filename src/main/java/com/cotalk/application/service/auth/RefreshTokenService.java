package com.cotalk.application.service.auth;

import com.cotalk.domain.entity.RefreshToken;
import com.cotalk.domain.exception.InvalidRefreshTokenException;
import com.cotalk.domain.port.inbound.auth.RefreshTokenUseCase;
import com.cotalk.domain.port.outbound.AuthTokenPort;
import com.cotalk.domain.port.outbound.IdGenerator;
import com.cotalk.domain.port.outbound.RefreshTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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
    private final long refreshTokenExpirationDays;

    /**
     * RefreshTokenService 생성자.
     *
     * @param refreshTokenRepository Refresh Token 저장소
     * @param authTokenPort Access 토큰 발급 포트
     * @param idGenerator ID 생성기
     * @param refreshTokenExpirationDays 리프레시 토큰 만료 일수
     */
    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            AuthTokenPort authTokenPort,
            IdGenerator idGenerator,
            long refreshTokenExpirationDays) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.authTokenPort = authTokenPort;
        this.idGenerator = idGenerator;
        this.refreshTokenExpirationDays = refreshTokenExpirationDays;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String createRefreshToken(Long userId) {
        // 기존 토큰이 있으면 폐기
        refreshTokenRepository.findByUserIdAndRevokedFalse(userId)
                .ifPresent(token -> {
                    token.revoke();
                    refreshTokenRepository.save(token);
                });

        // 새 토큰 생성
        String tokenValue = generateTokenValue();
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(refreshTokenExpirationDays);

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
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public String refreshAccessToken(String refreshToken) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 Refresh Token 사용 시도");
                    return new InvalidRefreshTokenException();
                });

        if (!storedToken.isValid()) {
            log.warn("유효하지 않은 Refresh Token 사용 시도 - expired: {}, revoked: {}",
                    storedToken.isExpired(), storedToken.isRevoked());
            throw new InvalidRefreshTokenException();
        }

        String newAccessToken = authTokenPort.generateAccessToken(storedToken.getUserId());
        log.debug("Access Token 갱신 완료");

        return newAccessToken;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void revokeToken(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken)
                .ifPresent(token -> {
                    token.revoke();
                    refreshTokenRepository.save(token);
                    log.debug("Refresh Token 폐기 완료");
                });
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
     * 고유한 토큰 값을 생성한다.
     *
     * @return 생성된 토큰 값
     */
    private String generateTokenValue() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
