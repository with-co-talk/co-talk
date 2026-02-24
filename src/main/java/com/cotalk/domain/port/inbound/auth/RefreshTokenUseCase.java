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
     * 토큰 갱신 결과.
     * Access Token과 새로 발급된 Refresh Token을 함께 반환한다.
     *
     * @param accessToken 새로 발급된 Access Token
     * @param refreshToken 새로 발급된 Refresh Token
     */
    record RefreshResult(String accessToken, String refreshToken) {}

    /**
     * Refresh Token으로 새로운 Access Token과 Refresh Token을 발급받는다.
     * 기존 Refresh Token은 폐기되고 새로운 Refresh Token이 발급된다 (Token Rotation).
     *
     * @param refreshToken Refresh Token 값
     * @return 새로 발급된 Access Token과 Refresh Token
     * @throws com.cotalk.domain.exception.InvalidRefreshTokenException 토큰이 유효하지 않은 경우
     */
    RefreshResult refreshAccessToken(String refreshToken);

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
