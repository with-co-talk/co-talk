package com.cotalk.adapter.outbound.persistence.refreshtoken;

import com.cotalk.domain.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Refresh Token JPA 리포지토리.
 *
 * @author seunggu.lee
 */
public interface RefreshTokenJpaRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * 토큰 값으로 Refresh Token을 조회한다.
     *
     * @param token 토큰 값
     * @return Refresh Token
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * 사용자 ID로 폐기되지 않은 Refresh Token을 조회한다.
     *
     * @param userId 사용자 ID
     * @return Refresh Token
     */
    Optional<RefreshToken> findByUserIdAndRevokedFalse(Long userId);

    /**
     * 사용자의 모든 Refresh Token을 폐기한다.
     *
     * @param userId 사용자 ID
     */
    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.userId = :userId AND r.revoked = false")
    void revokeAllByUserId(@Param("userId") Long userId);

    /**
     * 만료된 토큰을 삭제한다.
     *
     * @param now 현재 시간
     * @return 삭제된 토큰 수
     */
    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiresAt < :now")
    int deleteByExpiresAtBefore(@Param("now") LocalDateTime now);
}
