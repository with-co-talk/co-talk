package com.cotalk.infrastructure.oauth;

import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.OAuthVerificationException;
import com.cotalk.domain.model.VerifiedOAuthIdentity;
import com.cotalk.infrastructure.config.properties.OAuthProperties;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Set;

/**
 * 구글 id_token 검증 전략.
 *
 * <p>구글 JWKS(https://www.googleapis.com/oauth2/v3/certs)로 서명을 검증하고,
 * {@code iss}가 {@code accounts.google.com}, {@code aud}가 설정된 구글 client-id,
 * {@code exp}가 만료되지 않았는지 검증한다. {@code sub}로 oauthId를, {@code email}로 이메일을 도출한다.</p>
 *
 * @author seunggu.lee
 */
@Component
public class GoogleTokenVerifier extends IdTokenVerifier implements ProviderTokenVerifier {

    private static final String JWKS_URL = "https://www.googleapis.com/oauth2/v3/certs";
    private static final String ISSUER_HTTPS = "https://accounts.google.com";
    private static final String ISSUER_BARE = "accounts.google.com";

    private final String clientId;

    /**
     * 구글 검증 전략을 생성한다.
     *
     * @param restClient      OAuth 전용 RestClient
     * @param oAuthProperties OAuth 설정 프로퍼티 (구글 client-id 포함)
     */
    public GoogleTokenVerifier(
            @Qualifier("oauthRestClient") RestClient restClient,
            OAuthProperties oAuthProperties) {
        super(new JwksKeyProvider(restClient, JWKS_URL, "Google"));
        this.clientId = oAuthProperties.google().clientId();
    }

    @Override
    public User.OAuthProvider provider() {
        return User.OAuthProvider.GOOGLE;
    }

    /**
     * 구글 id_token을 검증하여 식별 정보를 반환한다.
     *
     * @param providerToken 구글 id_token
     * @return 검증된 식별 정보
     * @throws OAuthVerificationException 검증 실패 시
     */
    @Override
    public VerifiedOAuthIdentity verify(String providerToken) {
        return verifyIdToken(providerToken);
    }

    /**
     * 구글은 {@code iss}로 {@code https://accounts.google.com}과 {@code accounts.google.com}을
     * 모두 사용할 수 있어 둘 다 허용한다.
     *
     * @return 허용 발급자 집합
     */
    @Override
    protected Set<String> allowedIssuers() {
        return Set.of(ISSUER_HTTPS, ISSUER_BARE);
    }

    @Override
    protected String configuredAudience() {
        return clientId;
    }

    @Override
    protected String providerName() {
        return "Google";
    }

    @Override
    protected String extractNickname(Claims claims) {
        String name = claims.get("name", String.class);
        if (name != null && !name.isBlank()) {
            return name;
        }
        return claims.get("given_name", String.class);
    }
}
