package com.cotalk.infrastructure.oauth;

import com.cotalk.domain.exception.OAuthVerificationException;
import com.cotalk.domain.model.VerifiedOAuthIdentity;
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

    private MockRestServiceServer mockServer;
    private KakaoTokenVerifier verifier;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        verifier = new KakaoTokenVerifier(builder.build());
    }

    @Test
    @DisplayName("카카오 userinfo 응답을 식별 정보로 매핑한다")
    void should_mapUserinfo_when_validAccessToken() {
        // given
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
    @DisplayName("카카오가 401을 반환하면(토큰 무효) 검증을 거부한다")
    void should_reject_when_kakaoReturnsUnauthorized() {
        // given
        mockServer.expect(requestTo(USERINFO_URL))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        // when & then
        assertThatThrownBy(() -> verifier.verify("invalid-token"))
                .isInstanceOf(OAuthVerificationException.class);
    }
}
