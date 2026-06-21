package com.cotalk.infrastructure.oauth;

import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.OAuthVerificationException;
import com.cotalk.domain.model.VerifiedOAuthIdentity;

/**
 * 단일 OAuth 제공자에 대한 토큰 검증 전략.
 *
 * <p>{@link OAuthIdentityVerifierDispatcher}가 제공자별 구현을 {@link #provider()} 키로 라우팅한다.</p>
 *
 * @author seunggu.lee
 */
interface ProviderTokenVerifier {

    /**
     * 이 전략이 담당하는 OAuth 제공자.
     *
     * @return 담당 제공자
     */
    User.OAuthProvider provider();

    /**
     * 제공자 토큰을 검증하여 신뢰 가능한 식별 정보를 반환한다.
     *
     * @param providerToken 제공자 토큰 (카카오: access token, 구글/애플: id_token)
     * @return 검증된 식별 정보
     * @throws OAuthVerificationException 검증 실패 시
     */
    VerifiedOAuthIdentity verify(String providerToken);
}
