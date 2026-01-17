package com.cotalk.adapter.inbound.rest.dto.auth;

/**
 * OAuth 로그인 응답 DTO.
 *
 * @param token     JWT 토큰
 * @param tokenType 토큰 타입 (Bearer)
 * @param isNewUser 신규 사용자 여부
 * @param userId    사용자 ID
 * @author seunggu.lee
 */
public record OAuthLoginResponse(
        String token,
        String tokenType,
        boolean isNewUser,
        Long userId
) {

    /**
     * OAuthLoginResponse를 생성한다.
     *
     * @param token     JWT 토큰
     * @param isNewUser 신규 사용자 여부
     * @param userId    사용자 ID
     * @return OAuthLoginResponse 인스턴스
     */
    public static OAuthLoginResponse of(String token, boolean isNewUser, Long userId) {
        return new OAuthLoginResponse(token, "Bearer", isNewUser, userId);
    }
}
