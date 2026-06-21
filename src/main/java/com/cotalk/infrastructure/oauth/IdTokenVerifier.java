package com.cotalk.infrastructure.oauth;

import com.cotalk.domain.exception.OAuthVerificationException;
import com.cotalk.domain.model.VerifiedOAuthIdentity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.LocatorAdapter;
import io.jsonwebtoken.ProtectedHeader;
import lombok.extern.slf4j.Slf4j;

import java.security.Key;
import java.util.Set;

/**
 * 구글/애플 id_token(JWT) 검증을 위한 공통 베이스.
 *
 * <p>JWKS로 서명을 검증하고, jjwt가 {@code exp} 만료를 자동 검증한다. 추가로 발급자({@code iss})와
 * 대상({@code aud}=설정된 client-id)을 명시적으로 검증한다. client-id가 설정되지 않았으면
 * fail-closed로 검증을 거부한다.</p>
 *
 * @author seunggu.lee
 */
@Slf4j
abstract class IdTokenVerifier {

    private final JwksKeyProvider keyProvider;

    /**
     * id_token 검증기를 생성한다.
     *
     * @param keyProvider 제공자 JWKS 키 제공자
     */
    protected IdTokenVerifier(JwksKeyProvider keyProvider) {
        this.keyProvider = keyProvider;
    }

    /**
     * 이 제공자의 신뢰 가능한 발급자({@code iss}) 값 집합.
     * 구글처럼 https/bare 두 형태를 쓰는 제공자를 위해 집합으로 둔다.
     *
     * @return 허용 발급자 집합
     */
    protected abstract Set<String> allowedIssuers();

    /**
     * {@code aud} 검증에 사용할 설정된 client-id.
     *
     * @return client-id (미설정 시 빈 문자열)
     */
    protected abstract String configuredAudience();

    /**
     * 로그/예외 메시지용 제공자 이름.
     *
     * @return 제공자 이름
     */
    protected abstract String providerName();

    /**
     * id_token을 검증하고 claims에서 검증된 식별 정보를 도출한다.
     *
     * @param idToken 제공자 id_token (JWT)
     * @return 검증된 식별 정보
     * @throws OAuthVerificationException 서명/만료/iss/aud 검증 실패 또는 client-id 미설정 시
     */
    protected VerifiedOAuthIdentity verifyIdToken(String idToken) {
        String audience = configuredAudience();
        if (audience == null || audience.isBlank()) {
            // fail-closed: client-id가 설정되지 않으면 aud 검증이 불가능하므로 거부한다.
            log.error("{} client-id가 설정되지 않아 id_token 검증을 거부합니다(fail-closed).", providerName());
            throw new OAuthVerificationException(providerName() + " 검증 설정(client-id)이 누락되었습니다.");
        }

        Claims claims;
        try {
            Jws<Claims> jws = Jwts.parser()
                    .keyLocator(new LocatorAdapter<Key>() {
                        @Override
                        protected Key locate(ProtectedHeader header) {
                            return keyProvider.getKey(header.getKeyId());
                        }
                    })
                    .requireAudience(audience)
                    .build()
                    .parseSignedClaims(idToken);
            claims = jws.getPayload();
        } catch (OAuthVerificationException e) {
            throw e;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("{} id_token 검증 실패: {}", providerName(), e.getMessage());
            throw new OAuthVerificationException(providerName() + " 토큰 검증에 실패했습니다.", e);
        }

        String issuer = claims.getIssuer();
        if (issuer == null || !allowedIssuers().contains(issuer)) {
            log.warn("{} id_token 발급자(iss) 불일치: {}", providerName(), issuer);
            throw new OAuthVerificationException(providerName() + " 토큰 발급자가 올바르지 않습니다.");
        }

        String sub = claims.getSubject();
        if (sub == null || sub.isBlank()) {
            throw new OAuthVerificationException(providerName() + " 토큰에 sub가 없습니다.");
        }

        String email = claims.get("email", String.class);
        return new VerifiedOAuthIdentity(sub, email, extractNickname(claims), null);
    }

    /**
     * claims에서 닉네임/표시 이름을 추출한다. 제공자별로 오버라이드할 수 있다.
     *
     * @param claims 검증된 토큰 claims
     * @return 닉네임 또는 null
     */
    protected String extractNickname(Claims claims) {
        return claims.get("name", String.class);
    }
}
