package com.cotalk.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OAuth 제공자 토큰 검증 설정 프로퍼티.
 *
 * <p>구글/애플 id_token의 {@code aud}(audience) 검증에 사용할 클라이언트 ID를 보관한다.
 * 프로덕션에서는 반드시 환경변수로 주입해야 하며, 미설정 시 해당 제공자 검증은
 * fail-closed(거부)된다.</p>
 *
 * @param google 구글 OAuth 설정
 * @param apple  애플 OAuth 설정
 * @author seunggu.lee
 */
@ConfigurationProperties(prefix = "app.oauth")
public record OAuthProperties(
        Google google,
        Apple apple
) {

    /**
     * 누락된 하위 설정을 기본값(빈 client-id)으로 보정한다.
     *
     * @param google 구글 OAuth 설정
     * @param apple  애플 OAuth 설정
     */
    public OAuthProperties {
        if (google == null) {
            google = new Google("");
        }
        if (apple == null) {
            apple = new Apple("");
        }
    }

    /**
     * 구글 OAuth 설정.
     *
     * @param clientId 구글 OAuth 클라이언트 ID (id_token aud 검증용). 미설정 시 fail-closed.
     */
    public record Google(String clientId) {
        public Google {
            if (clientId == null) {
                clientId = "";
            }
        }
    }

    /**
     * 애플 OAuth 설정.
     *
     * @param clientId 애플 OAuth 클라이언트 ID(서비스 ID, id_token aud 검증용). 미설정 시 fail-closed.
     */
    public record Apple(String clientId) {
        public Apple {
            if (clientId == null) {
                clientId = "";
            }
        }
    }
}
