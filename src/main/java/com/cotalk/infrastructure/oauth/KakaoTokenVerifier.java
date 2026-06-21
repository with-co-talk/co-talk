package com.cotalk.infrastructure.oauth;

import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.OAuthVerificationException;
import com.cotalk.domain.model.VerifiedOAuthIdentity;
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
 * <p>{@code GET https://kapi.kakao.com/v2/user/me}를 {@code Authorization: Bearer <access_token>}로
 * 호출하여 응답의 {@code id}, {@code kakao_account.email},
 * {@code kakao_account.profile.nickname}, {@code kakao_account.profile.profile_image_url}로부터
 * 검증된 식별 정보를 도출한다. 토큰이 유효하지 않으면 카카오가 401을 반환하며 이를
 * {@link OAuthVerificationException}으로 변환한다.</p>
 *
 * @author seunggu.lee
 */
@Slf4j
@Component
public class KakaoTokenVerifier implements ProviderTokenVerifier {

    private static final String USERINFO_URL = "https://kapi.kakao.com/v2/user/me";

    private final RestClient restClient;

    /**
     * 카카오 검증 전략을 생성한다.
     *
     * @param restClient OAuth 전용 RestClient
     */
    public KakaoTokenVerifier(@Qualifier("oauthRestClient") RestClient restClient) {
        this.restClient = restClient;
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
