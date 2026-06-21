package com.cotalk.infrastructure.oauth;

import com.cotalk.domain.exception.OAuthVerificationException;
import com.cotalk.domain.model.VerifiedOAuthIdentity;
import com.cotalk.infrastructure.config.properties.OAuthProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KakaoTokenVerifierTest {

    private static final String USERINFO_URL = "https://kapi.kakao.com/v2/user/me";
    private static final String TOKEN_INFO_URL = "https://kapi.kakao.com/v1/user/access_token_info";
    private static final String APP_ID = "123456";

    private MockRestServiceServer mockServer;
    private KakaoTokenVerifier verifier;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        verifier = newVerifier(builder.build(), APP_ID);
    }

    private KakaoTokenVerifier newVerifier(RestClient restClient, String appId) {
        OAuthProperties properties = new OAuthProperties(null, null, new OAuthProperties.Kakao(appId));
        return new KakaoTokenVerifier(restClient, properties);
    }

    /**
     * access_token_info 응답(app_id 일치)을 먼저 모킹한다.
     */
    private void expectTokenInfo(String appId) {
        mockServer.expect(requestTo(TOKEN_INFO_URL))
                .andExpect(header("Authorization", "Bearer kakao-access-token"))
                .andRespond(withSuccess("{\"app_id\": " + appId + "}", MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("app_id가 일치하면 카카오 userinfo 응답을 식별 정보로 매핑한다")
    void should_mapUserinfo_when_appIdMatches() {
        // given
        expectTokenInfo(APP_ID);
        String json = """
                {
                  "id": 1234567890,
                  "kakao_account": {
                    "email": "user@kakao.com",
                    "profile": {
                      "nickname": "카카오유저",
                      "profile_image_url": "https://k.kakaocdn.net/avatar.png"
                    }
                  }
                }
                """;
        mockServer.expect(requestTo(USERINFO_URL))
                .andExpect(header("Authorization", "Bearer kakao-access-token"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        // when
        VerifiedOAuthIdentity identity = verifier.verify("kakao-access-token");

        // then
        assertThat(identity.oauthId()).isEqualTo("1234567890");
        assertThat(identity.email()).isEqualTo("user@kakao.com");
        assertThat(identity.nickname()).isEqualTo("카카오유저");
        assertThat(identity.avatarUrl()).isEqualTo("https://k.kakaocdn.net/avatar.png");
    }

    @Test
    @DisplayName("이메일/프로필이 없어도 id만 있으면 매핑한다")
    void should_mapMinimal_when_onlyIdPresent() {
        // given
        expectTokenInfo(APP_ID);
        String json = """
                { "id": 999 }
                """;
        mockServer.expect(requestTo(USERINFO_URL))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        // when
        VerifiedOAuthIdentity identity = verifier.verify("kakao-access-token");

        // then
        assertThat(identity.oauthId()).isEqualTo("999");
        assertThat(identity.email()).isNull();
        assertThat(identity.nickname()).isNull();
    }

    @Test
    @DisplayName("토큰 app_id가 설정 app-id와 다르면 검증을 거부한다(cross-app 리플레이 방어)")
    void should_reject_when_appIdMismatch() {
        // given
        expectTokenInfo("999999");

        // when & then
        assertThatThrownBy(() -> verifier.verify("kakao-access-token"))
                .isInstanceOf(OAuthVerificationException.class);
    }

    @Test
    @DisplayName("설정 app-id가 비어 있으면 검증을 거부한다(fail-closed)")
    void should_reject_when_appIdBlank() {
        // given: app-id 미설정. token_info를 호출하기 전에 거부되어야 한다.
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KakaoTokenVerifier blankVerifier = newVerifier(builder.build(), "");

        // when & then
        assertThatThrownBy(() -> blankVerifier.verify("kakao-access-token"))
                .isInstanceOf(OAuthVerificationException.class);
        server.verify(); // 어떤 HTTP 호출도 없었음을 확인
    }

    @Test
    @DisplayName("카카오가 401을 반환하면(토큰 무효) 검증을 거부한다")
    void should_reject_when_kakaoReturnsUnauthorized() {
        // given: access_token_info 단계에서 401
        mockServer.expect(requestTo(TOKEN_INFO_URL))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        // when & then
        assertThatThrownBy(() -> verifier.verify("invalid-token"))
                .isInstanceOf(OAuthVerificationException.class);
    }
}
