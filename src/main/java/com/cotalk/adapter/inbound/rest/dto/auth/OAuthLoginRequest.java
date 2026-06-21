package com.cotalk.adapter.inbound.rest.dto.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * OAuth 로그인 요청 DTO.
 *
 * <p>보안: 클라이언트는 더 이상 oauthId/email/nickname 등 식별 정보를 직접 보내지 않는다.
 * 제공자 토큰만 보내며, 서버가 이를 검증하여 신뢰 가능한 식별 정보를 도출한다.</p>
 *
 * @param provider OAuth 제공자 (KAKAO, GOOGLE, APPLE)
 * @param token    제공자 토큰 (카카오: access token, 구글/애플: id_token)
 * @author seunggu.lee
 */
public record OAuthLoginRequest(
        @NotBlank(message = "OAuth 제공자는 필수입니다.")
        String provider,

        @NotBlank(message = "OAuth 토큰은 필수입니다.")
        String token
) {
    /**
     * OAuthLoginRequest 인스턴스를 생성합니다.
     *
     * @param provider OAuth 제공자 (KAKAO, GOOGLE, APPLE)
     * @param token    제공자 토큰 (카카오: access token, 구글/애플: id_token)
     * @return OAuthLoginRequest 인스턴스
     */
    public static OAuthLoginRequest of(String provider, String token) {
        return new OAuthLoginRequest(provider, token);
    }
}
