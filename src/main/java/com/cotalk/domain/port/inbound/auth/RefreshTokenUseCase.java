package com.cotalk.domain.port.inbound.auth;

/**
 * Refresh Token 유스케이스.
 * Refresh Token의 생성, 갱신, 폐기 기능을 정의한다.
 *
 * @author seunggu.lee
 */
public interface RefreshTokenUseCase {

    /**
     * 사용자 ID로 새로운 Refresh Token을 생성한다.
     * 기존에 유효한 Refresh Token이 있으면 폐기하고 새로 생성한다.
     *
     * @param userId 사용자 ID
     * @return 생성된 Refresh Token 값
     */
    String createRefreshToken(Long userId);

    /**
     * Refresh Token으로 새로운 Access Token을 발급받는다.
     *
     * @param refreshToken Refresh Token 값
     * @return 새로 발급된 Access Token
     * @throws com.cotalk.domain.exception.InvalidRefreshTokenException 토큰이 유효하지 않은 경우
     */
    String refreshAccessToken(String refreshToken);

    /**
     * 특정 Refresh Token을 폐기한다.
     *
     * @param refreshToken 폐기할 Refresh Token 값
     */
    void revokeToken(String refreshToken);

    /**
     * 사용자의 모든 Refresh Token을 폐기한다.
     * 로그아웃 시 호출된다.
     *
     * @param userId 사용자 ID
     */
    void revokeAllTokensByUserId(Long userId);
}
