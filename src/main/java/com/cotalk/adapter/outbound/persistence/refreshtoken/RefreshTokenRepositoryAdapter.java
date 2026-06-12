package com.cotalk.adapter.outbound.persistence.refreshtoken;

import com.cotalk.domain.entity.RefreshToken;
import com.cotalk.domain.port.outbound.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Refresh Token 저장소 어댑터.
 * RefreshTokenRepository 포트의 구현체로, JPA를 통해 영속성을 관리한다.
 *
 * @author seunggu.lee
 */
@Repository
@RequiredArgsConstructor
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpaRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        return jpaRepository.save(refreshToken);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return jpaRepository.findByToken(token);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<RefreshToken> findByUserIdAndRevokedFalse(Long userId) {
        return jpaRepository.findByUserIdAndRevokedFalse(userId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void revokeAllByUserId(Long userId) {
        jpaRepository.revokeAllByUserId(userId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteByUserId(Long userId) {
        jpaRepository.deleteByUserId(userId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int deleteExpiredTokens() {
        return jpaRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }
}
