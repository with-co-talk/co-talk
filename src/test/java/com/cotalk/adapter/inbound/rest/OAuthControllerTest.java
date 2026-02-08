package com.cotalk.adapter.inbound.rest;

import com.cotalk.adapter.inbound.rest.dto.auth.OAuthLoginRequest;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.port.inbound.auth.OAuthLoginUseCase;
import com.cotalk.infrastructure.ratelimit.RateLimitTestConfiguration;
import com.cotalk.infrastructure.security.JwtAuthenticationFilter;
import com.cotalk.infrastructure.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OAuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(RateLimitTestConfiguration.class)
class OAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OAuthLoginUseCase oAuthLoginService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("카카오 로그인 성공 - 신규 사용자")
    void should_returnCreatedStatus_when_newKakaoUser() throws Exception {
        // given
        OAuthLoginRequest request = OAuthLoginRequest.of(
                "KAKAO",
                "kakao_12345",
                "user@kakao.com",
                "카카오유저",
                "https://kakao.com/avatar.png"
        );

        given(oAuthLoginService.loginWithOAuth(
                eq(User.OAuthProvider.KAKAO),
                eq("kakao_12345"),
                eq("user@kakao.com"),
                eq("카카오유저"),
                eq("https://kakao.com/avatar.png")))
                .willReturn(new OAuthLoginUseCase.OAuthLoginResult("jwt_token", true, 100L));

        // when & then
        mockMvc.perform(post("/api/v1/auth/oauth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt_token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.isNewUser").value(true))
                .andExpect(jsonPath("$.userId").value(100));
    }

    @Test
    @DisplayName("구글 로그인 성공 - 기존 사용자")
    void should_returnOkStatus_when_existingGoogleUser() throws Exception {
        // given
        OAuthLoginRequest request = OAuthLoginRequest.of(
                "GOOGLE",
                "google_12345",
                "user@gmail.com",
                "구글유저",
                null
        );

        given(oAuthLoginService.loginWithOAuth(
                eq(User.OAuthProvider.GOOGLE),
                eq("google_12345"),
                eq("user@gmail.com"),
                eq("구글유저"),
                eq(null)))
                .willReturn(new OAuthLoginUseCase.OAuthLoginResult("google_jwt_token", false, 200L));

        // when & then
        mockMvc.perform(post("/api/v1/auth/oauth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("google_jwt_token"))
                .andExpect(jsonPath("$.isNewUser").value(false))
                .andExpect(jsonPath("$.userId").value(200));
    }

    @Test
    @DisplayName("애플 로그인 성공")
    void should_returnOkStatus_when_appleLogin() throws Exception {
        // given
        OAuthLoginRequest request = OAuthLoginRequest.of(
                "APPLE",
                "apple_12345",
                "user@icloud.com",
                "애플유저",
                null
        );

        given(oAuthLoginService.loginWithOAuth(
                eq(User.OAuthProvider.APPLE),
                eq("apple_12345"),
                eq("user@icloud.com"),
                eq("애플유저"),
                eq(null)))
                .willReturn(new OAuthLoginUseCase.OAuthLoginResult("apple_jwt_token", true, 300L));

        // when & then
        mockMvc.perform(post("/api/v1/auth/oauth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("apple_jwt_token"))
                .andExpect(jsonPath("$.isNewUser").value(true));
    }

    @Test
    @DisplayName("OAuth 로그인 실패 - provider가 없는 경우")
    void should_returnBadRequest_when_providerMissing() throws Exception {
        // given
        String request = """
                {
                    "oauthId": "kakao_12345",
                    "email": "user@kakao.com",
                    "nickname": "카카오유저"
                }
                """;

        // when & then
        mockMvc.perform(post("/api/v1/auth/oauth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("OAuth 로그인 실패 - oauthId가 없는 경우")
    void should_returnBadRequest_when_oauthIdMissing() throws Exception {
        // given
        String request = """
                {
                    "provider": "KAKAO",
                    "email": "user@kakao.com",
                    "nickname": "카카오유저"
                }
                """;

        // when & then
        mockMvc.perform(post("/api/v1/auth/oauth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }
}
