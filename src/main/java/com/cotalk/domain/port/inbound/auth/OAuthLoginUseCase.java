package com.cotalk.domain.port.inbound.auth;

import com.cotalk.domain.entity.User;

/**
 * OAuth 소셜 로그인 유스케이스.
 * 카카오, 구글, 애플 등의 OAuth 제공자를 통한 로그인을 처리한다.
 *
 * @author seunggu.lee
 */
public interface OAuthLoginUseCase {

    /**
     * OAuth 제공자를 통해 로그인한다.
     * 신규 사용자인 경우 자동 회원가입 후 로그인한다.
     *
     * @param provider  OAuth 제공자 (KAKAO, GOOGLE, APPLE)
     * @param oauthId   OAuth 제공자가 제공한 사용자 고유 ID
     * @param email     사용자 이메일
     * @param nickname  사용자 닉네임
     * @param avatarUrl 프로필 이미지 URL (선택)
     * @return 로그인 결과 (JWT 토큰, 신규 사용자 여부, 사용자 ID)
     */
    OAuthLoginResult loginWithOAuth(
            User.OAuthProvider provider,
            String oauthId,
            String email,
            String nickname,
            String avatarUrl);

    /**
     * OAuth 로그인 결과.
     *
     * @param token     JWT 토큰
     * @param isNewUser 신규 사용자 여부
     * @param userId    사용자 ID
     */
    record OAuthLoginResult(String token, boolean isNewUser, Long userId) {}
}
