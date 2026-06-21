package com.cotalk.adapter.outbound.persistence.auth;

import com.cotalk.adapter.outbound.persistence.entity.EmailVerificationTokenJpaEntity;
import com.cotalk.adapter.outbound.persistence.mapper.EmailVerificationTokenMapper;
import com.cotalk.domain.entity.EmailVerificationToken;
import com.cotalk.domain.port.outbound.EmailVerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 이메일 인증 토큰 영속성 어댑터.
 * JPA를 통해 이메일 인증 토큰 데이터를 저장하고 조회한다.
 *
 * @author seunggu.lee
 */
@Repository
@RequiredArgsConstructor
public class EmailVerificationTokenRepositoryAdapter implements EmailVerificationTokenRepository {

    private final EmailVerificationTokenJpaRepository jpaRepository;
    private final EmailVerificationTokenMapper mapper;

    /**
     * 이메일 인증 토큰을 저장한다.
     *
     * @param token 저장할 이메일 인증 토큰 엔티티
     * @return 저장된 이메일 인증 토큰 엔티티
     */
    @Override
    public EmailVerificationToken save(EmailVerificationToken token) {
        EmailVerificationTokenJpaEntity saved = jpaRepository.save(mapper.toJpa(token));
        return mapper.toDomain(saved);
    }

    /**
     * 토큰 값으로 이메일 인증 토큰을 조회한다.
     *
     * @param token 토큰 값
     * @return 이메일 인증 토큰 (Optional)
     */
    @Override
    public Optional<EmailVerificationToken> findByToken(String token) {
        return jpaRepository.findByToken(token).map(mapper::toDomain);
    }

    /**
     * 사용자 ID로 가장 최근 이메일 인증 토큰을 조회한다.
     *
     * @param userId 사용자 ID
     * @return 가장 최근 이메일 인증 토큰 (Optional)
     */
    @Override
    public Optional<EmailVerificationToken> findLatestByUserId(Long userId) {
        return jpaRepository.findLatestByUserId(userId).map(mapper::toDomain);
    }

    /**
     * 사용자 ID로 이메일 인증 토큰을 삭제한다.
     *
     * @param userId 사용자 ID
     */
    @Override
    public void deleteByUserId(Long userId) {
        jpaRepository.deleteByUserId(userId);
    }

    /**
     * 만료된 토큰들을 일괄 삭제한다.
     */
    @Override
    public void deleteExpiredTokens() {
        jpaRepository.deleteExpiredTokens(LocalDateTime.now());
    }
}
