package com.cotalk.adapter.inbound.rest.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * OAuth 로그인 요청 DTO.
 *
 * @param provider  OAuth 제공자 (KAKAO, GOOGLE, APPLE)
 * @param oauthId   OAuth 제공자에서 발급한 사용자 고유 ID
 * @param email     이메일 주소
 * @param nickname  닉네임
 * @param avatarUrl 프로필 이미지 URL (선택)
 * @author seunggu.lee
 */
public record OAuthLoginRequest(
        @NotBlank(message = "OAuth 제공자는 필수입니다.")
        String provider,

        @NotBlank(message = "OAuth ID는 필수입니다.")
        String oauthId,

        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        String email,

        @NotBlank(message = "닉네임은 필수입니다.")
        String nickname,

        String avatarUrl
) {
    /**
     * OAuthLoginRequest 인스턴스를 생성합니다.
     *
     * @param provider  OAuth 제공자 (KAKAO, GOOGLE, APPLE)
     * @param oauthId   OAuth 제공자에서 발급한 사용자 고유 ID
     * @param email     이메일 주소
     * @param nickname  닉네임
     * @param avatarUrl 프로필 이미지 URL (선택)
     * @return OAuthLoginRequest 인스턴스
     */
    public static OAuthLoginRequest of(
            String provider,
            String oauthId,
            String email,
            String nickname,
            String avatarUrl) {
        return new OAuthLoginRequest(provider, oauthId, email, nickname, avatarUrl);
    }
}
