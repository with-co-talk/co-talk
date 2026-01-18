package com.cotalk.adapter.inbound.rest.dto.auth;

/**
 * 인증 토큰 응답 DTO.
 * 로그인 시 Access Token과 Refresh Token을 함께 반환한다.
 *
 * @param accessToken  JWT Access Token
 * @param refreshToken Refresh Token
 * @param tokenType    토큰 타입 (Bearer)
 * @param expiresIn    Access Token 만료 시간 (초)
 * @author seunggu.lee
 */
public record AuthTokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn
) {

    /**
     * AuthTokenResponse를 생성한다.
     *
     * @param accessToken  JWT Access Token
     * @param refreshToken Refresh Token
     * @param expiresIn    Access Token 만료 시간 (초)
     * @return AuthTokenResponse 인스턴스
     */
    public static AuthTokenResponse of(String accessToken, String refreshToken, long expiresIn) {
        return new AuthTokenResponse(accessToken, refreshToken, "Bearer", expiresIn);
    }
}
