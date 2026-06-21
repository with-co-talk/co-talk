package com.cotalk.infrastructure.oauth;

import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.OAuthVerificationException;
import com.cotalk.domain.model.VerifiedOAuthIdentity;
import com.cotalk.domain.port.outbound.OAuthIdentityVerifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 제공자별 {@link ProviderTokenVerifier} 전략으로 라우팅하는 {@link OAuthIdentityVerifier} 구현.
 *
 * <p>스프링이 주입한 모든 전략 빈을 {@link ProviderTokenVerifier#provider()} 키로 매핑하여
 * 요청 제공자에 맞는 전략에 검증을 위임한다. 토큰이 null/공백이거나 지원하지 않는 제공자면
 * fail-closed로 {@link OAuthVerificationException}을 던진다.</p>
 *
 * @author seunggu.lee
 */
@Slf4j
@Component
public class OAuthIdentityVerifierDispatcher implements OAuthIdentityVerifier {

    private final Map<User.OAuthProvider, ProviderTokenVerifier> verifiers;

    /**
     * 전략 빈 목록으로 디스패처를 생성한다.
     *
     * @param providerVerifiers 제공자별 검증 전략 목록
     */
    public OAuthIdentityVerifierDispatcher(List<ProviderTokenVerifier> providerVerifiers) {
        this.verifiers = new EnumMap<>(User.OAuthProvider.class);
        for (ProviderTokenVerifier verifier : providerVerifiers) {
            this.verifiers.put(verifier.provider(), verifier);
        }
    }

    /**
     * 제공자 토큰을 검증하여 신뢰 가능한 식별 정보를 반환한다.
     *
     * @param provider      OAuth 제공자
     * @param providerToken 제공자 토큰
     * @return 검증된 식별 정보
     * @throws OAuthVerificationException 토큰 부재, 미지원 제공자, 검증 실패 시
     */
    @Override
    public VerifiedOAuthIdentity verify(User.OAuthProvider provider, String providerToken) {
        if (providerToken == null || providerToken.isBlank()) {
            throw new OAuthVerificationException("제공자 토큰이 비어 있습니다.");
        }
        ProviderTokenVerifier verifier = verifiers.get(provider);
        if (verifier == null) {
            throw new OAuthVerificationException("지원하지 않는 OAuth 제공자입니다: " + provider);
        }
        return verifier.verify(providerToken);
    }
}
