package com.cotalk.adapter.inbound.rest;

import com.cotalk.domain.exception.DuplicateEmailException;
import com.cotalk.domain.exception.InvalidCredentialsException;
import com.cotalk.domain.port.inbound.LoginUseCase;
import com.cotalk.domain.port.inbound.SignUpUseCase;
import com.cotalk.infrastructure.security.JwtAuthenticationFilter;
import com.cotalk.infrastructure.security.JwtTokenProvider;
import com.cotalk.infrastructure.ratelimit.RateLimitTestConfiguration;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(RateLimitTestConfiguration.class)
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
            AuthController.SignUpRequest request = new AuthController.SignUpRequest("test@example.com", "password123", "테스트유저");
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
            AuthController.SignUpRequest request = new AuthController.SignUpRequest("", "password123", "테스트유저");

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
            AuthController.SignUpRequest request = new AuthController.SignUpRequest("test@example.com", "", "테스트유저");

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
            AuthController.SignUpRequest request = new AuthController.SignUpRequest("duplicate@example.com", "password123", "테스트유저");
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
            AuthController.LoginRequest request = new AuthController.LoginRequest("test@example.com", "password123");
            given(loginUseCase.login(anyString(), anyString())).willReturn("jwt-token-12345");

            // when & then
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("jwt-token-12345"))
                    .andExpect(jsonPath("$.tokenType").value("Bearer"));
        }

        @Test
        @DisplayName("잘못된 자격 증명으로 로그인 시 401 에러")
        void should_returnUnauthorized_when_invalidCredentials() throws Exception {
            // given
            AuthController.LoginRequest request = new AuthController.LoginRequest("test@example.com", "wrongpassword");
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
}
