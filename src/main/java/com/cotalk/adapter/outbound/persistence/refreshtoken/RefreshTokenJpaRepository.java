package com.cotalk.adapter.outbound.persistence.refreshtoken;

import com.cotalk.adapter.outbound.persistence.entity.RefreshTokenJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Refresh Token JPA 리포지토리.
 * persistence 계층 전용이며, 도메인 반환은 Adapter에서 매핑한다.
 *
 * @author seunggu.lee
 */
public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenJpaEntity, Long> {

    /**
     * 토큰 값으로 Refresh Token을 조회한다.
     *
     * @param token 토큰 값
     * @return Refresh Token
     */
    Optional<RefreshTokenJpaEntity> findByToken(String token);

    /**
     * 사용자 ID로 폐기되지 않은 Refresh Token을 조회한다.
     *
     * @param userId 사용자 ID
     * @return Refresh Token
     */
    Optional<RefreshTokenJpaEntity> findByUserIdAndRevokedFalse(Long userId);

    /**
     * 사용자의 모든 Refresh Token을 폐기한다.
     *
     * @param userId 사용자 ID
     */
    @Modifying
    @Query("UPDATE RefreshTokenJpaEntity r SET r.revoked = true WHERE r.userId = :userId AND r.revoked = false")
    void revokeAllByUserId(@Param("userId") Long userId);

    /**
     * 사용자의 모든 Refresh Token을 물리적으로 삭제한다.
     *
     * <p>회원 탈퇴 시 {@code refresh_tokens.user_id}에 걸린
     * {@code fk_refresh_tokens_user}(cascade 없음) 제약 위반을 방지하기 위해
     * 사용자 삭제 이전에 토큰 행을 실제로 제거한다.</p>
     *
     * @param userId 사용자 ID
     * @return 삭제된 토큰 수
     */
    @Modifying
    @Query("DELETE FROM RefreshTokenJpaEntity r WHERE r.userId = :userId")
    int deleteByUserId(@Param("userId") Long userId);

    /**
     * 만료된 토큰을 삭제한다.
     *
     * @param now 현재 시간
     * @return 삭제된 토큰 수
     */
    @Modifying
    @Query("DELETE FROM RefreshTokenJpaEntity r WHERE r.expiresAt < :now")
    int deleteByExpiresAtBefore(@Param("now") LocalDateTime now);
}
