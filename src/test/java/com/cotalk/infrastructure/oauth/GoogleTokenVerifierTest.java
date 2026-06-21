package com.cotalk.infrastructure.oauth;

import com.cotalk.domain.exception.OAuthVerificationException;
import com.cotalk.domain.model.VerifiedOAuthIdentity;
import com.cotalk.infrastructure.config.properties.OAuthProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GoogleTokenVerifierTest {

    private static final String JWKS_URL = "https://www.googleapis.com/oauth2/v3/certs";
    private static final String ISSUER = "https://accounts.google.com";
    private static final String CLIENT_ID = "test-google-client-id.apps.googleusercontent.com";

    private OAuthTestTokens tokens;
    private MockRestServiceServer mockServer;
    private GoogleTokenVerifier verifier;

    @BeforeEach
    void setUp() throws Exception {
        tokens = OAuthTestTokens.create();

        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        OAuthProperties props = new OAuthProperties(
                new OAuthProperties.Google(CLIENT_ID), new OAuthProperties.Apple(""), null);
        verifier = new GoogleTokenVerifier(restClient, props);
    }

    private void expectJwks(String jwksJson) {
        mockServer.expect(ExpectedCount.manyTimes(), requestTo(JWKS_URL))
                .andRespond(withSuccess(jwksJson, org.springframework.http.MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("유효한 구글 id_token을 검증하면 sub/email로 식별 정보를 도출한다")
    void should_returnIdentity_when_validToken() {
        // given
        expectJwks(tokens.jwksJson());
        String token = tokens.signToken(ISSUER, CLIENT_ID, "google-sub-123",
                Instant.now().plusSeconds(600),
                Map.of("email", "user@gmail.com", "name", "구글유저"));

        // when
        VerifiedOAuthIdentity identity = verifier.verify(token);

        // then
        assertThat(identity.oauthId()).isEqualTo("google-sub-123");
        assertThat(identity.email()).isEqualTo("user@gmail.com");
        assertThat(identity.nickname()).isEqualTo("구글유저");
    }

    @Test
    @DisplayName("aud가 설정된 client-id와 다르면 검증을 거부한다")
    void should_reject_when_audMismatch() {
        // given
        expectJwks(tokens.jwksJson());
        String token = tokens.signToken(ISSUER, "attacker-client-id", "google-sub-123",
                Instant.now().plusSeconds(600), Map.of("email", "user@gmail.com"));

        // when & then
        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(OAuthVerificationException.class);
    }

    @Test
    @DisplayName("만료된 id_token은 검증을 거부한다")
    void should_reject_when_expired() {
        // given
        expectJwks(tokens.jwksJson());
        String token = tokens.signToken(ISSUER, CLIENT_ID, "google-sub-123",
                Instant.now().minusSeconds(60), Map.of("email", "user@gmail.com"));

        // when & then
        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(OAuthVerificationException.class);
    }

    @Test
    @DisplayName("iss가 구글이 아니면 검증을 거부한다")
    void should_reject_when_issuerMismatch() {
        // given
        expectJwks(tokens.jwksJson());
        String token = tokens.signToken("https://evil.example.com", CLIENT_ID, "google-sub-123",
                Instant.now().plusSeconds(600), Map.of("email", "user@gmail.com"));

        // when & then
        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(OAuthVerificationException.class);
    }

    @Test
    @DisplayName("다른 키로 서명된 토큰은 서명 불일치로 거부한다")
    void should_reject_when_signatureMismatch() throws Exception {
        // given: JWKS는 우리 키를 주지만 토큰은 외부 키로 서명
        expectJwks(tokens.jwksJson());
        OAuthTestTokens foreign = OAuthTestTokens.create();
        String token = foreign.signToken(ISSUER, CLIENT_ID, "google-sub-123",
                Instant.now().plusSeconds(600), Map.of("email", "user@gmail.com"));

        // when & then
        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(OAuthVerificationException.class);
    }

    @Test
    @DisplayName("client-id가 미설정이면 fail-closed로 검증을 거부한다")
    void should_failClosed_when_clientIdUnset() {
        // given
        OAuthProperties emptyProps = new OAuthProperties(
                new OAuthProperties.Google(""), new OAuthProperties.Apple(""), null);
        GoogleTokenVerifier unconfigured = new GoogleTokenVerifier(RestClient.builder().build(), emptyProps);
        String token = tokens.signToken(ISSUER, CLIENT_ID, "google-sub-123",
                Instant.now().plusSeconds(600), Map.of("email", "user@gmail.com"));

        // when & then
        assertThatThrownBy(() -> unconfigured.verify(token))
                .isInstanceOf(OAuthVerificationException.class);
    }
}
