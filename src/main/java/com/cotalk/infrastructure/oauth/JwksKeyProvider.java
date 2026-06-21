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
 * <p>증폭 방어: 알 수 없는 {@code kid}가 들어올 때마다 매번 JWKS를 재조회하면, 무작위
 * kid를 흘리는 공격으로 제공자 JWKS 엔드포인트를 향한 무한 재요청을 유발할 수 있다.
 * 이를 막기 위해 캐시 미스로 인한 재조회는 {@link #MIN_REFRESH_INTERVAL} 간격으로만
 * 허용한다(네거티브 캐시). 알 수 없는 kid는 여전히 거부하므로(fail-closed) 보안은 유지된다.</p>
 *
 * @author seunggu.lee
 */
@Slf4j
class JwksKeyProvider {

    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    /**
     * 캐시 미스(알 수 없는 kid)로 인한 JWKS 재조회 사이의 최소 간격.
     * TTL 만료로 인한 정상 갱신에는 적용되지 않으며, 무작위 kid 플러딩 증폭만 차단한다.
     */
    private static final Duration MIN_REFRESH_INTERVAL = Duration.ofSeconds(30);

    private final RestClient restClient;
    private final String jwksUrl;
    private final String providerName;

    private volatile Map<String, Key> cachedKeys = Map.of();
    private volatile Instant expiresAt = Instant.EPOCH;
    private volatile Instant nextMissRefreshAllowedAt = Instant.EPOCH;

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
        Instant now = Instant.now();
        boolean ttlExpired = now.isAfter(expiresAt);
        // 캐시 미스 재조회는 최소 간격이 지났을 때에만 허용(무작위 kid 플러딩 증폭 방어).
        boolean missRefreshAllowed = !keys.containsKey(kid) && !now.isBefore(nextMissRefreshAllowedAt);
        if (ttlExpired || missRefreshAllowed) {
            keys = refresh();
        }

        Key key = keys.get(kid);
        if (key == null) {
            throw new OAuthVerificationException(providerName + " JWKS에서 kid=" + kid + " 키를 찾을 수 없습니다.");
        }
        return key;
    }

    private synchronized Map<String, Key> refresh() {
        // 다른 스레드가 방금 갱신했고(TTL 내) 미스 재조회 최소 간격도 아직이면 재조회 생략.
        // 미스 재조회 최소 간격이 지났다면(키 롤오버 대비) TTL 내라도 한 번은 재조회를 허용한다.
        Instant now = Instant.now();
        if (now.isBefore(expiresAt) && now.isBefore(nextMissRefreshAllowedAt)) {
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
            Instant refreshedAt = Instant.now();
            this.expiresAt = refreshedAt.plus(CACHE_TTL);
            // 방금 갱신했으므로 다음 캐시-미스 재조회는 최소 간격 이후에만 허용한다.
            this.nextMissRefreshAllowedAt = refreshedAt.plus(MIN_REFRESH_INTERVAL);
            return this.cachedKeys;
        } catch (RuntimeException e) {
            log.warn("{} JWKS 파싱 실패: {}", providerName, e.getMessage());
            throw new OAuthVerificationException(providerName + " 공개키 파싱에 실패했습니다.", e);
        }
    }
}
