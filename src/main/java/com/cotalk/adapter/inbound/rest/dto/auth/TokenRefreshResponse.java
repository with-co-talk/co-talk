package com.cotalk.adapter.inbound.rest.dto.auth;

/**
 * 토큰 갱신 응답 DTO.
 *
 * @param accessToken  새로 발급된 Access Token
 * @param refreshToken Refresh Token (기존 토큰 유지)
 * @param tokenType    토큰 타입 (Bearer)
 * @author seunggu.lee
 */
public record TokenRefreshResponse(
        String accessToken,
        String refreshToken,
        String tokenType
) {

    /**
     * TokenRefreshResponse를 생성한다.
     *
     * @param accessToken  새로 발급된 Access Token
     * @param refreshToken Refresh Token
     * @return TokenRefreshResponse 인스턴스
     */
    public static TokenRefreshResponse of(String accessToken, String refreshToken) {
        return new TokenRefreshResponse(accessToken, refreshToken, "Bearer");
    }
}
