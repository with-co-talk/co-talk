package com.cotalk.infrastructure.oauth;

import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.OAuthVerificationException;
import com.cotalk.domain.model.VerifiedOAuthIdentity;
import com.cotalk.infrastructure.config.properties.OAuthProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 카카오 access token 검증 전략.
 *
 * <p>먼저 {@code GET https://kapi.kakao.com/v1/user/access_token_info}로 토큰의
 * {@code app_id}를 조회해 우리 앱에 발급된 토큰인지 확인한다. 카카오의
 * {@code /v2/user/me}는 어떤 카카오 앱의 access token으로도 호출되므로, 다른 앱에서
 * 유출된 토큰이 우리 서비스로 리플레이되는 cross-app 계정 탈취를 막으려면 이 바인딩
 * 검증이 필수다. app_id가 설정값과 일치할 때에만
 * {@code GET https://kapi.kakao.com/v2/user/me}를 {@code Authorization: Bearer <access_token>}로
 * 호출하여 응답의 {@code id}, {@code kakao_account.email},
 * {@code kakao_account.profile.nickname}, {@code kakao_account.profile.profile_image_url}로부터
 * 검증된 식별 정보를 도출한다. 토큰이 유효하지 않으면 카카오가 401을 반환하며 이를
 * {@link OAuthVerificationException}으로 변환한다.</p>
 *
 * <p>fail-closed: 설정된 app-id가 비어 있으면(미설정) 검증을 거부한다.</p>
 *
 * @author seunggu.lee
 */
@Slf4j
@Component
public class KakaoTokenVerifier implements ProviderTokenVerifier {

    private static final String USERINFO_URL = "https://kapi.kakao.com/v2/user/me";
    private static final String TOKEN_INFO_URL = "https://kapi.kakao.com/v1/user/access_token_info";

    private final RestClient restClient;
    private final String appId;

    /**
     * 카카오 검증 전략을 생성한다.
     *
     * @param restClient      OAuth 전용 RestClient
     * @param oAuthProperties OAuth 설정 프로퍼티 (카카오 app-id 포함)
     */
    public KakaoTokenVerifier(
            @Qualifier("oauthRestClient") RestClient restClient,
            OAuthProperties oAuthProperties) {
        this.restClient = restClient;
        this.appId = oAuthProperties.kakao().appId();
    }

    @Override
    public User.OAuthProvider provider() {
        return User.OAuthProvider.KAKAO;
    }

    /**
     * 카카오 access token으로 userinfo를 조회해 검증된 식별 정보를 반환한다.
     *
     * @param providerToken 카카오 access token
     * @return 검증된 식별 정보
     * @throws OAuthVerificationException 토큰이 유효하지 않거나 호출에 실패한 경우
     */
    @Override
    public VerifiedOAuthIdentity verify(String providerToken) {
        verifyTokenBoundToOurApp(providerToken);

        JsonNode body;
        try {
            body = restClient.get()
                    .uri(USERINFO_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + providerToken)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException e) {
            log.warn("Kakao userinfo 호출 실패: {}", e.getMessage());
            throw new OAuthVerificationException("카카오 토큰 검증에 실패했습니다.", e);
        }

        if (body == null || !body.hasNonNull("id")) {
            throw new OAuthVerificationException("카카오 응답에 사용자 ID가 없습니다.");
        }

        String oauthId = body.get("id").asText();
        JsonNode account = body.path("kakao_account");
        JsonNode profile = account.path("profile");

        String email = textOrNull(account, "email");
        String nickname = textOrNull(profile, "nickname");
        String avatarUrl = textOrNull(profile, "profile_image_url");

        return new VerifiedOAuthIdentity(oauthId, email, nickname, avatarUrl);
    }

    /**
     * access token이 우리 카카오 앱에 발급된 토큰인지 검증한다.
     *
     * <p>{@code /v1/user/access_token_info}로 토큰의 {@code app_id}를 조회해 설정된
     * app-id와 일치하는지 확인한다. 설정값이 비어 있거나(미설정), app_id가 응답에 없거나,
     * 값이 일치하지 않으면 모두 거부한다(fail-closed). 메시지는 열거 단서를 주지 않도록
     * 일반화한다.</p>
     *
     * @param providerToken 카카오 access token
     * @throws OAuthVerificationException 설정 미비, 조회 실패, app_id 불일치 시
     */
    private void verifyTokenBoundToOurApp(String providerToken) {
        if (appId == null || appId.isBlank()) {
            log.warn("카카오 app-id가 설정되지 않아 토큰 검증을 거부한다(fail-closed).");
            throw new OAuthVerificationException("카카오 토큰 검증에 실패했습니다.");
        }

        JsonNode info;
        try {
            info = restClient.get()
                    .uri(TOKEN_INFO_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + providerToken)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException e) {
            log.warn("Kakao access_token_info 호출 실패: {}", e.getMessage());
            throw new OAuthVerificationException("카카오 토큰 검증에 실패했습니다.", e);
        }

        if (info == null || !info.hasNonNull("app_id")) {
            log.warn("Kakao access_token_info 응답에 app_id가 없습니다.");
            throw new OAuthVerificationException("카카오 토큰 검증에 실패했습니다.");
        }

        String tokenAppId = info.get("app_id").asText();
        if (!appId.equals(tokenAppId)) {
            log.warn("카카오 토큰 app_id 불일치로 거부(cross-app 리플레이 방어).");
            throw new OAuthVerificationException("카카오 토큰 검증에 실패했습니다.");
        }
    }

    /**
     * 노드에서 텍스트 필드를 읽되, 없거나 null이면 null을 반환한다.
     *
     * @param node  대상 JSON 노드
     * @param field 필드명
     * @return 필드 값 또는 null
     */
    private String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && !value.isNull() ? value.asText() : null;
    }
}
