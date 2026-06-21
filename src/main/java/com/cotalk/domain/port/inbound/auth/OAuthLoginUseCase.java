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
     * 제공자 토큰을 서버에서 검증한 뒤 OAuth 로그인을 처리한다.
     *
     * <p>식별 정보(oauthId/email/nickname/avatar)는 클라이언트 입력이 아니라
     * 검증된 제공자 토큰에서만 도출한다. 신규 사용자인 경우 자동 회원가입 후 로그인한다.</p>
     *
     * @param provider      OAuth 제공자 (KAKAO, GOOGLE, APPLE)
     * @param providerToken 제공자 토큰 (카카오: access token, 구글/애플: id_token)
     * @return 로그인 결과 (JWT 토큰, 신규 사용자 여부, 사용자 ID)
     */
    OAuthLoginResult loginWithOAuth(User.OAuthProvider provider, String providerToken);

    /**
     * OAuth 로그인 결과.
     *
     * @param token     JWT 토큰
     * @param isNewUser 신규 사용자 여부
     * @param userId    사용자 ID
     */
    record OAuthLoginResult(String token, boolean isNewUser, Long userId) {}
}
