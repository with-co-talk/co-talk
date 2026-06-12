package com.cotalk.adapter.outbound.persistence.auth;

import com.cotalk.domain.entity.PasswordResetToken;
import com.cotalk.domain.model.Email;
import com.cotalk.domain.port.outbound.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 비밀번호 재설정 토큰 영속성 어댑터.
 * JPA를 통해 비밀번호 재설정 토큰 데이터를 저장하고 조회한다.
 *
 * @author seunggu.lee
 */
@Repository
@RequiredArgsConstructor
public class PasswordResetTokenRepositoryAdapter implements PasswordResetTokenRepository {

    private final PasswordResetTokenJpaRepository jpaRepository;

    /**
     * 비밀번호 재설정 토큰을 저장한다.
     *
     * @param token 저장할 비밀번호 재설정 토큰 엔티티
     * @return 저장된 비밀번호 재설정 토큰 엔티티
     */
    @Override
    public PasswordResetToken save(PasswordResetToken token) {
        return jpaRepository.save(token);
    }

    /**
     * 토큰 값으로 비밀번호 재설정 토큰을 조회한다.
     *
     * @param token 토큰 값
     * @return 비밀번호 재설정 토큰 (Optional)
     */
    @Override
    public Optional<PasswordResetToken> findByToken(String token) {
        return jpaRepository.findByToken(token);
    }

    /**
     * 이메일로 가장 최근에 발급된 미사용 비밀번호 재설정 토큰을 조회한다.
     *
     * @param email 이메일 주소
     * @return 비밀번호 재설정 토큰 (Optional)
     */
    @Override
    public Optional<PasswordResetToken> findLatestActiveByEmail(String email) {
        return jpaRepository.findLatestActiveByEmail(new Email(email));
    }

    /**
     * 사용자 ID로 비밀번호 재설정 토큰을 삭제한다.
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
