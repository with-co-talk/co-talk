package com.cotalk.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OAuth 제공자 토큰 검증 설정 프로퍼티.
 *
 * <p>구글/애플 id_token의 {@code aud}(audience) 검증에 사용할 클라이언트 ID와,
 * 카카오 access token이 우리 앱에 발급된 토큰인지 확인할 app-id를 보관한다.
 * 프로덕션에서는 반드시 환경변수로 주입해야 하며, 미설정 시 해당 제공자 검증은
 * fail-closed(거부)된다.</p>
 *
 * @param google 구글 OAuth 설정
 * @param apple  애플 OAuth 설정
 * @param kakao  카카오 OAuth 설정
 * @author seunggu.lee
 */
@ConfigurationProperties(prefix = "app.oauth")
public record OAuthProperties(
        Google google,
        Apple apple,
        Kakao kakao
) {

    /**
     * 누락된 하위 설정을 기본값(빈 식별자)으로 보정한다.
     *
     * @param google 구글 OAuth 설정
     * @param apple  애플 OAuth 설정
     * @param kakao  카카오 OAuth 설정
     */
    public OAuthProperties {
        if (google == null) {
            google = new Google("");
        }
        if (apple == null) {
            apple = new Apple("");
        }
        if (kakao == null) {
            kakao = new Kakao("");
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

    /**
     * 카카오 OAuth 설정.
     *
     * <p>카카오 access token은 우리 앱에 발급된 것이 아니어도 {@code /v2/user/me}로
     * 사용자 정보를 얻을 수 있어, 다른 앱에서 유출된 토큰이 리플레이될 수 있다.
     * 이를 막기 위해 {@code /v1/user/access_token_info}로 토큰의 {@code app_id}를 조회해
     * 이 설정값과 일치하는지 검증한다. 미설정 시 fail-closed(거부)된다.</p>
     *
     * @param appId 카카오 애플리케이션 ID(access token app_id 검증용). 미설정 시 fail-closed.
     */
    public record Kakao(String appId) {
        public Kakao {
            if (appId == null) {
                appId = "";
            }
        }
    }
}
