package com.cotalk.infrastructure.oauth;

import com.cotalk.domain.exception.OAuthVerificationException;
import io.jsonwebtoken.security.Jwk;
import io.jsonwebtoken.security.JwkSet;
import io.jsonwebtoken.security.Jwks;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * 제공자 JWKS(JSON Web Key Set)를 가져와 {@code kid → 공개키} 매핑을 짧은 TTL로 캐싱하는 헬퍼.
 *
 * <p>구글/애플 id_token 서명 검증에 필요한 공개키를 제공한다. 캐시는 TTL이 지났거나
 * 요청한 {@code kid}가 캐시에 없을 때(키 롤오버 대비) 다시 가져온다.</p>
 *
 * @author seunggu.lee
 */
@Slf4j
class JwksKeyProvider {

    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private final RestClient restClient;
    private final String jwksUrl;
    private final String providerName;

    private volatile Map<String, Key> cachedKeys = Map.of();
    private volatile Instant expiresAt = Instant.EPOCH;

    /**
     * JWKS 키 제공자를 생성한다.
     *
     * @param restClient   JWKS를 가져올 RestClient
     * @param jwksUrl      제공자 JWKS 엔드포인트 URL
     * @param providerName 로그용 제공자 이름
     */
    JwksKeyProvider(RestClient restClient, String jwksUrl, String providerName) {
        this.restClient = restClient;
        this.jwksUrl = jwksUrl;
        this.providerName = providerName;
    }

    /**
     * 주어진 {@code kid}에 해당하는 서명 검증용 공개키를 반환한다.
     * 캐시 미스나 TTL 만료 시 JWKS를 다시 가져온다.
     *
     * @param kid id_token 헤더의 키 식별자
     * @return 해당 키 (없으면 {@link OAuthVerificationException})
     * @throws OAuthVerificationException 키를 찾을 수 없거나 JWKS 조회에 실패한 경우
     */
    Key getKey(String kid) {
        if (kid == null || kid.isBlank()) {
            throw new OAuthVerificationException(providerName + " id_token에 kid가 없습니다.");
        }

        Map<String, Key> keys = cachedKeys;
        if (Instant.now().isAfter(expiresAt) || !keys.containsKey(kid)) {
            keys = refresh();
        }

        Key key = keys.get(kid);
        if (key == null) {
            throw new OAuthVerificationException(providerName + " JWKS에서 kid=" + kid + " 키를 찾을 수 없습니다.");
        }
        return key;
    }

    private synchronized Map<String, Key> refresh() {
        // 다른 스레드가 방금 갱신했고 현재 키 셋으로 충분하면 재조회 생략
        if (Instant.now().isBefore(expiresAt)) {
            return cachedKeys;
        }
        String json;
        try {
            json = restClient.get()
                    .uri(jwksUrl)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException e) {
            log.warn("{} JWKS 조회 실패: {}", providerName, e.getMessage());
            throw new OAuthVerificationException(providerName + " 공개키 조회에 실패했습니다.", e);
        }

        if (json == null || json.isBlank()) {
            throw new OAuthVerificationException(providerName + " JWKS 응답이 비어 있습니다.");
        }

        try {
            JwkSet jwkSet = Jwks.setParser().build().parse(json);
            Map<String, Key> keys = new HashMap<>();
            for (Jwk<?> jwk : jwkSet.getKeys()) {
                String kid = jwk.getId();
                if (kid != null) {
                    keys.put(kid, jwk.toKey());
                }
            }
            this.cachedKeys = Map.copyOf(keys);
            this.expiresAt = Instant.now().plus(CACHE_TTL);
            return this.cachedKeys;
        } catch (RuntimeException e) {
            log.warn("{} JWKS 파싱 실패: {}", providerName, e.getMessage());
            throw new OAuthVerificationException(providerName + " 공개키 파싱에 실패했습니다.", e);
        }
    }
}
