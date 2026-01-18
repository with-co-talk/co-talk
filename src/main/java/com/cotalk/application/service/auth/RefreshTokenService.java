package com.cotalk.application.service.auth;

import com.cotalk.domain.entity.RefreshToken;
import com.cotalk.domain.exception.InvalidRefreshTokenException;
import com.cotalk.domain.port.inbound.auth.RefreshTokenUseCase;
import com.cotalk.domain.port.outbound.IdGenerator;
import com.cotalk.domain.port.outbound.RefreshTokenRepository;
import com.cotalk.infrastructure.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
@Service
@Transactional
public class RefreshTokenService implements RefreshTokenUseCase {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final IdGenerator idGenerator;
    private final long refreshTokenExpirationDays;

    /**
     * RefreshTokenService 생성자.
     *
     * @param refreshTokenRepository Refresh Token 저장소
     * @param jwtTokenProvider JWT 토큰 제공자
     * @param idGenerator ID 생성기
     * @param refreshTokenExpirationDays Refresh Token 만료 일수
     */
    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            JwtTokenProvider jwtTokenProvider,
            IdGenerator idGenerator,
            @Value("${jwt.refresh-token.expiration-days:7}") long refreshTokenExpirationDays) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtTokenProvider = jwtTokenProvider;
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
                .ifPresent(RefreshToken::revoke);

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
        log.info("Refresh Token 생성 완료 - userId: {}", userId);

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
            log.warn("유효하지 않은 Refresh Token 사용 시도 - userId: {}, expired: {}, revoked: {}",
                    storedToken.getUserId(), storedToken.isExpired(), storedToken.isRevoked());
            throw new InvalidRefreshTokenException();
        }

        String newAccessToken = jwtTokenProvider.generateToken(storedToken.getUserId());
        log.debug("Access Token 갱신 완료 - userId: {}", storedToken.getUserId());

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
                    log.info("Refresh Token 폐기 완료 - userId: {}", token.getUserId());
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void revokeAllTokensByUserId(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
        log.info("사용자의 모든 Refresh Token 폐기 완료 - userId: {}", userId);
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
