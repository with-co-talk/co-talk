package com.cotalk.domain.port.outbound;

import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.OAuthVerificationException;
import com.cotalk.domain.model.VerifiedOAuthIdentity;

/**
 * OAuth 제공자(카카오/구글/애플)가 발급한 토큰을 서버에서 검증하여
 * 신뢰 가능한 사용자 식별 정보를 도출하는 아웃바운드 포트.
 *
 * <p>이 포트의 존재 이유: 클라이언트가 보낸 {@code oauthId}/{@code email} 등을 그대로 신뢰하면
 * 공격자가 피해자의 {@code oauthId}만 알아도 세션 토큰을 발급받는 계정 탈취가 가능하다.
 * 따라서 인증 식별 정보는 반드시 제공자 토큰을 서버가 직접 검증해 도출해야 한다.</p>
 *
 * <p>구현 어댑터는 {@code infrastructure} 계층에 위치하며, 카카오는 userinfo API 호출,
 * 구글/애플은 id_token JWT 서명을 제공자 JWKS로 검증하는 방식으로 식별 정보를 얻는다.</p>
 *
 * @author seunggu.lee
 */
public interface OAuthIdentityVerifier {

    /**
     * 제공자 토큰을 검증하여 신뢰 가능한 사용자 식별 정보를 반환한다.
     *
     * @param provider      OAuth 제공자 (KAKAO, GOOGLE, APPLE)
     * @param providerToken 제공자 토큰 (카카오: access token, 구글/애플: id_token)
     * @return 검증된 사용자 식별 정보 (제공자 토큰에서 도출, 클라이언트 입력 아님)
     * @throws OAuthVerificationException 토큰이 유효하지 않거나(만료/서명/aud/iss 불일치),
     *                                    네트워크 오류 등으로 검증에 실패한 경우
     */
    VerifiedOAuthIdentity verify(User.OAuthProvider provider, String providerToken);
}
