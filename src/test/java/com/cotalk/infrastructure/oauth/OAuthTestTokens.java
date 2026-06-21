package com.cotalk.infrastructure.oauth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Jwks;
import io.jsonwebtoken.security.PublicJwk;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

/**
 * OAuth id_token 검증 어댑터 테스트용 헬퍼.
 * RSA 키쌍으로 서명된 id_token과 그에 대응하는 JWKS JSON을 생성한다.
 *
 * @author seunggu.lee
 */
final class OAuthTestTokens {

    static final String KID = "test-kid-1";

    private final KeyPair keyPair;

    private OAuthTestTokens(KeyPair keyPair) {
        this.keyPair = keyPair;
    }

    /**
     * 2048비트 RSA 키쌍으로 헬퍼를 생성한다.
     *
     * @return 토큰/ JWKS 생성기
     * @throws Exception 키 생성 실패 시
     */
    static OAuthTestTokens create() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return new OAuthTestTokens(generator.generateKeyPair());
    }

    /**
     * 이 키쌍의 공개키를 담은 JWKS({@code {"keys":[...]}}) JSON을 반환한다.
     *
     * @return JWKS JSON 문자열
     */
    String jwksJson() {
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        PublicJwk<?> jwk = Jwks.builder().key(publicKey).id(KID).build();
        return "{\"keys\":[" + Jwks.json(jwk) + "]}";
    }

    /**
     * 잘못된(다른) 키로 서명된 JWKS를 반환한다. 서명 불일치 테스트용.
     *
     * @return 다른 키의 JWKS JSON
     * @throws Exception 키 생성 실패 시
     */
    static String foreignJwksJson() throws Exception {
        return create().jwksJson();
    }

    /**
     * 지정한 claim으로 서명된 RS256 id_token을 생성한다.
     *
     * @param issuer    iss
     * @param audience  aud
     * @param subject   sub
     * @param expiresAt 만료 시각
     * @param extra     추가 claim (email/name 등)
     * @return 서명된 JWT 문자열
     */
    String signToken(String issuer, String audience, String subject, Instant expiresAt, Map<String, Object> extra) {
        var builder = Jwts.builder()
                .header().keyId(KID).and()
                .issuer(issuer)
                .audience().add(audience).and()
                .subject(subject)
                .issuedAt(Date.from(Instant.now().minusSeconds(60)))
                .expiration(Date.from(expiresAt));
        extra.forEach(builder::claim);
        return builder.signWith(keyPair.getPrivate(), Jwts.SIG.RS256).compact();
    }

    /**
     * 키쌍의 공개키를 반환한다.
     *
     * @return 공개키
     */
    PublicKey publicKey() {
        return keyPair.getPublic();
    }
}
