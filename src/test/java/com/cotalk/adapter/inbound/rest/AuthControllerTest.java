package com.cotalk.adapter.inbound.rest;

import com.cotalk.adapter.inbound.rest.dto.auth.LoginRequest;
import com.cotalk.adapter.inbound.rest.dto.auth.SignUpRequest;
import com.cotalk.adapter.inbound.rest.dto.auth.TokenRefreshRequest;
import com.cotalk.domain.exception.DuplicateEmailException;
import com.cotalk.domain.exception.InvalidCredentialsException;
import com.cotalk.domain.exception.InvalidRefreshTokenException;
import com.cotalk.domain.port.inbound.auth.LoginResult;
import com.cotalk.domain.port.inbound.auth.LoginUseCase;
import com.cotalk.domain.port.inbound.auth.RefreshTokenUseCase;
import com.cotalk.domain.port.inbound.auth.SignUpUseCase;
import com.cotalk.infrastructure.exception.GlobalExceptionHandler;
import com.cotalk.infrastructure.ratelimit.RateLimitTestConfiguration;
import com.cotalk.infrastructure.security.JwtAuthenticationFilter;
import com.cotalk.infrastructure.security.JwtTokenProvider;
import com.cotalk.infrastructure.security.SecurityContextHelper;
import com.cotalk.infrastructure.security.WithMockCustomUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({RateLimitTestConfiguration.class, GlobalExceptionHandler.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SignUpUseCase signUpUseCase;

    @MockBean
    private LoginUseCase loginUseCase;

    @MockBean
    private RefreshTokenUseCase refreshTokenUseCase;

    @MockBean
    private SecurityContextHelper securityContextHelper;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Nested
    @DisplayName("회원가입 API")
    class SignUpApi {

        @Test
        @DisplayName("유효한 요청으로 회원가입 성공")
        void should_returnCreated_when_validSignUpRequest() throws Exception {
            // given
            SignUpRequest request = new SignUpRequest("test@example.com", "Password123!", "테스트유저");
            given(signUpUseCase.signUp(anyString(), anyString(), anyString())).willReturn(1L);

            // when & then
            mockMvc.perform(post("/api/v1/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.userId").value(1L))
                    .andExpect(jsonPath("$.message").value("회원가입이 완료되었습니다."));
        }

        @Test
        @DisplayName("이메일이 비어있으면 400 에러")
        void should_returnBadRequest_when_emptyEmail() throws Exception {
            // given
            SignUpRequest request = new SignUpRequest("", "Password123!", "테스트유저");

            // when & then
            mockMvc.perform(post("/api/v1/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("비밀번호가 비어있으면 400 에러")
        void should_returnBadRequest_when_emptyPassword() throws Exception {
            // given
            SignUpRequest request = new SignUpRequest("test@example.com", "", "테스트유저");

            // when & then
            mockMvc.perform(post("/api/v1/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("중복된 이메일로 가입 시 409 에러")
        void should_returnConflict_when_duplicateEmail() throws Exception {
            // given
            SignUpRequest request = new SignUpRequest("duplicate@example.com", "Password123!", "테스트유저");
            given(signUpUseCase.signUp(anyString(), anyString(), anyString()))
                    .willThrow(new DuplicateEmailException());

            // when & then
            mockMvc.perform(post("/api/v1/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").value("이미 존재하는 이메일입니다."));
        }
    }

    @Nested
    @DisplayName("로그인 API")
    class LoginApi {

        @Test
        @DisplayName("유효한 요청으로 로그인 성공")
        void should_returnOkWithToken_when_validLoginRequest() throws Exception {
            // given
            LoginRequest request = new LoginRequest("test@example.com", "Password123!");
            LoginResult loginResult = new LoginResult("jwt-token-12345", 1L);
            given(loginUseCase.login(anyString(), anyString())).willReturn(loginResult);
            given(refreshTokenUseCase.createRefreshToken(1L)).willReturn("refresh-token-12345");

            // when & then
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("jwt-token-12345"))
                    .andExpect(jsonPath("$.refreshToken").value("refresh-token-12345"))
                    .andExpect(jsonPath("$.tokenType").value("Bearer"));
        }

        @Test
        @DisplayName("잘못된 자격 증명으로 로그인 시 401 에러")
        void should_returnUnauthorized_when_invalidCredentials() throws Exception {
            // given
            LoginRequest request = new LoginRequest("test@example.com", "wrongpassword");
            given(loginUseCase.login(anyString(), anyString()))
                    .willThrow(new InvalidCredentialsException());

            // when & then
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("이메일 또는 비밀번호가 올바르지 않습니다."));
        }
    }

    @Nested
    @DisplayName("토큰 갱신 API")
    class RefreshTokenApi {

        @Test
        @DisplayName("유효한 Refresh Token으로 토큰 갱신 성공")
        void should_returnNewAccessToken_when_validRefreshToken() throws Exception {
            // given
            String refreshToken = "valid-refresh-token";
            String newAccessToken = "new-access-token";
            TokenRefreshRequest request = new TokenRefreshRequest(refreshToken);

            given(refreshTokenUseCase.refreshAccessToken(refreshToken)).willReturn(newAccessToken);

            // when & then
            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value(newAccessToken))
                    .andExpect(jsonPath("$.refreshToken").value(refreshToken));
        }

        @Test
        @DisplayName("유효하지 않은 Refresh Token으로 갱신 시 401 에러")
        void should_returnUnauthorized_when_invalidRefreshToken() throws Exception {
            // given
            String invalidRefreshToken = "invalid-refresh-token";
            TokenRefreshRequest request = new TokenRefreshRequest(invalidRefreshToken);

            willThrow(new InvalidRefreshTokenException("유효하지 않은 Refresh Token입니다."))
                    .given(refreshTokenUseCase).refreshAccessToken(invalidRefreshToken);

            // when & then
            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("로그아웃 API")
    class LogoutApi {

        @Test
        @DisplayName("유효한 요청으로 로그아웃 성공")
        @WithMockCustomUser(userId = 1L)
        void should_returnOk_when_validLogout() throws Exception {
            // given
            Long userId = 1L;
            willDoNothing().given(refreshTokenUseCase).revokeAllTokensByUserId(userId);

            // when & then
            mockMvc.perform(post("/api/v1/auth/logout"))
                    .andExpect(status().isOk());
        }
    }
}
