package com.cotalk.adapter.outbound.persistence.auth;

import com.cotalk.adapter.outbound.persistence.entity.EmailVerificationTokenJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 이메일 인증 토큰 JPA 리포지토리.
 * persistence 계층 전용이며, 도메인 반환은 Adapter에서 매핑한다.
 *
 * @author seunggu.lee
 */
public interface EmailVerificationTokenJpaRepository extends JpaRepository<EmailVerificationTokenJpaEntity, Long> {

    /**
     * 토큰 값으로 이메일 인증 토큰을 조회한다.
     *
     * @param token 토큰 값
     * @return 이메일 인증 토큰 (Optional)
     */
    Optional<EmailVerificationTokenJpaEntity> findByToken(String token);

    /**
     * 사용자 ID로 가장 최근 이메일 인증 토큰을 조회한다.
     *
     * @param userId 사용자 ID
     * @return 가장 최근 이메일 인증 토큰 (Optional)
     */
    @Query("SELECT t FROM EmailVerificationTokenJpaEntity t WHERE t.userId = :userId ORDER BY t.createdAt DESC LIMIT 1")
    Optional<EmailVerificationTokenJpaEntity> findLatestByUserId(@Param("userId") Long userId);

    /**
     * 사용자 ID로 이메일 인증 토큰을 삭제한다.
     *
     * @param userId 사용자 ID
     */
    void deleteByUserId(Long userId);

    /**
     * 만료된 토큰들을 일괄 삭제한다.
     *
     * @param now 현재 시각
     */
    @Modifying
    @Query("DELETE FROM EmailVerificationTokenJpaEntity t WHERE t.expiresAt < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);
}
