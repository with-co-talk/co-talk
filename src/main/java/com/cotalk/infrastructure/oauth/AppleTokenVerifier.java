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
 * 애플 id_token 검증 전략.
 *
 * <p>애플 JWKS(https://appleid.apple.com/auth/keys)로 서명을 검증하고,
 * {@code iss}가 {@code https://appleid.apple.com}, {@code aud}가 설정된 애플 client-id(서비스 ID),
 * {@code exp}가 만료되지 않았는지 검증한다. {@code sub}로 oauthId를 도출한다.
 * 애플은 최초 로그인 이후 {@code email}을 내려주지 않을 수 있으므로 이메일 부재를 정상 처리한다.</p>
 *
 * @author seunggu.lee
 */
@Component
public class AppleTokenVerifier extends IdTokenVerifier implements ProviderTokenVerifier {

    private static final String JWKS_URL = "https://appleid.apple.com/auth/keys";
    private static final String ISSUER = "https://appleid.apple.com";

    private final String clientId;

    /**
     * 애플 검증 전략을 생성한다.
     *
     * @param restClient      OAuth 전용 RestClient
     * @param oAuthProperties OAuth 설정 프로퍼티 (애플 client-id 포함)
     */
    public AppleTokenVerifier(
            @Qualifier("oauthRestClient") RestClient restClient,
            OAuthProperties oAuthProperties) {
        super(new JwksKeyProvider(restClient, JWKS_URL, "Apple"));
        this.clientId = oAuthProperties.apple().clientId();
    }

    @Override
    public User.OAuthProvider provider() {
        return User.OAuthProvider.APPLE;
    }

    /**
     * 애플 id_token을 검증하여 식별 정보를 반환한다.
     *
     * @param providerToken 애플 id_token
     * @return 검증된 식별 정보 (이메일은 없을 수 있음)
     * @throws OAuthVerificationException 검증 실패 시
     */
    @Override
    public VerifiedOAuthIdentity verify(String providerToken) {
        return verifyIdToken(providerToken);
    }

    @Override
    protected Set<String> allowedIssuers() {
        return Set.of(ISSUER);
    }

    @Override
    protected String configuredAudience() {
        return clientId;
    }

    @Override
    protected String providerName() {
        return "Apple";
    }

    /**
     * 애플 id_token은 표시 이름 claim을 제공하지 않는다(이름은 최초 동의 시 별도 페이로드로 전달).
     *
     * @param claims 검증된 토큰 claims
     * @return 항상 null
     */
    @Override
    protected String extractNickname(Claims claims) {
        return null;
    }
}
