package com.cotalk.adapter.outbound.persistence.refreshtoken;

import com.cotalk.adapter.outbound.persistence.entity.RefreshTokenJpaEntity;
import com.cotalk.adapter.outbound.persistence.mapper.RefreshTokenMapper;
import com.cotalk.domain.entity.RefreshToken;
import com.cotalk.domain.port.outbound.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Refresh Token 저장소 어댑터.
 * RefreshTokenRepository 포트의 구현체로, JPA 엔티티와 도메인 간 매핑을 수행한다.
 *
 * @author seunggu.lee
 */
@Repository
@RequiredArgsConstructor
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpaRepository;
    private final RefreshTokenMapper mapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        RefreshTokenJpaEntity saved = jpaRepository.save(mapper.toJpa(refreshToken));
        return mapper.toDomain(saved);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return jpaRepository.findByToken(token).map(mapper::toDomain);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<RefreshToken> findByUserIdAndRevokedFalse(Long userId) {
        return jpaRepository.findByUserIdAndRevokedFalse(userId).map(mapper::toDomain);
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
