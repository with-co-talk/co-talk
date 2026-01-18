package com.cotalk.domain.port.outbound;

import com.cotalk.domain.entity.RefreshToken;

import java.util.Optional;

/**
 * Refresh Token 저장소 포트.
 * Refresh Token의 영속성 관리를 위한 인터페이스를 정의한다.
 *
 * @author seunggu.lee
 */
public interface RefreshTokenRepository {

    /**
     * Refresh Token을 저장한다.
     *
     * @param refreshToken 저장할 Refresh Token
     * @return 저장된 Refresh Token
     */
    RefreshToken save(RefreshToken refreshToken);

    /**
     * 토큰 값으로 Refresh Token을 조회한다.
     *
     * @param token 토큰 값
     * @return Refresh Token (존재하지 않으면 empty)
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * 사용자 ID로 폐기되지 않은 Refresh Token을 조회한다.
     *
     * @param userId 사용자 ID
     * @return Refresh Token (존재하지 않으면 empty)
     */
    Optional<RefreshToken> findByUserIdAndRevokedFalse(Long userId);

    /**
     * 사용자의 모든 Refresh Token을 폐기한다.
     *
     * @param userId 사용자 ID
     */
    void revokeAllByUserId(Long userId);

    /**
     * 만료된 모든 Refresh Token을 삭제한다.
     * 스케줄러에서 주기적으로 호출하여 정리한다.
     *
     * @return 삭제된 토큰 수
     */
    int deleteExpiredTokens();
}
