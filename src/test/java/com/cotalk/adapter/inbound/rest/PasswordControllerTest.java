package com.cotalk.adapter.inbound.rest;

import com.cotalk.domain.exception.InvalidPasswordResetTokenException;
import com.cotalk.domain.port.inbound.auth.ChangePasswordUseCase;
import com.cotalk.domain.port.inbound.auth.RequestPasswordResetUseCase;
import com.cotalk.domain.port.inbound.auth.ResetPasswordUseCase;
import com.cotalk.infrastructure.exception.GlobalExceptionHandler;
import com.cotalk.infrastructure.ratelimit.RateLimitTestConfiguration;
import com.cotalk.infrastructure.security.JwtAuthenticationFilter;
import com.cotalk.infrastructure.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PasswordController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({RateLimitTestConfiguration.class, GlobalExceptionHandler.class})
class PasswordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RequestPasswordResetUseCase requestPasswordResetUseCase;

    @MockitoBean
    private ResetPasswordUseCase resetPasswordUseCase;

    @MockitoBean
    private ChangePasswordUseCase changePasswordUseCase;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Nested
    @DisplayName("비밀번호 재설정 요청 API")
    class RequestPasswordResetTests {

        @Test
        @DisplayName("비밀번호 재설정 요청 성공")
        void should_returnOk_when_requestPasswordResetSuccess() throws Exception {
            // given
            String email = "test@example.com";

            willDoNothing().given(requestPasswordResetUseCase).requestPasswordReset(eq(email));

            String requestBody = """
                    {
                        "email": "test@example.com"
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/v1/password/reset-request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("비밀번호 재설정 링크가 이메일로 발송되었습니다. 이메일을 확인해주세요."));
        }

        @Test
        @DisplayName("존재하지 않는 이메일도 동일한 응답 반환 (보안)")
        void should_returnOk_when_emailNotExists() throws Exception {
            // given - 존재하지 않는 이메일이어도 예외 발생 안함
            String email = "nonexistent@example.com";

            willDoNothing().given(requestPasswordResetUseCase).requestPasswordReset(eq(email));

            String requestBody = """
                    {
                        "email": "nonexistent@example.com"
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/v1/password/reset-request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("비밀번호 재설정 링크가 이메일로 발송되었습니다. 이메일을 확인해주세요."));
        }

        @Test
        @DisplayName("이메일 누락 시 400 에러")
        void should_returnBadRequest_when_emailMissing() throws Exception {
            // given
            String requestBody = "{}";

            // when & then
            mockMvc.perform(post("/api/v1/password/reset-request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("빈 이메일 시 400 에러")
        void should_returnBadRequest_when_emailEmpty() throws Exception {
            // given
            String requestBody = """
                    {
                        "email": ""
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/v1/password/reset-request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("공백 이메일 시 400 에러")
        void should_returnBadRequest_when_emailBlank() throws Exception {
            // given
            String requestBody = """
                    {
                        "email": "   "
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/v1/password/reset-request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("잘못된 이메일 형식 시 400 에러")
        void should_returnBadRequest_when_invalidEmailFormat() throws Exception {
            // given
            String requestBody = """
                    {
                        "email": "invalid-email"
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/v1/password/reset-request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("@만 있는 이메일 형식 시 400 에러")
        void should_returnBadRequest_when_emailOnlyAtSign() throws Exception {
            // given
            String requestBody = """
                    {
                        "email": "@"
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/v1/password/reset-request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("토큰 유효성 검증 API")
    class ValidateTokenTests {

        @Test
        @DisplayName("유효한 토큰 검증 성공")
        void should_returnValid_when_tokenIsValid() throws Exception {
            // given
            String token = "valid-token-123";

            given(resetPasswordUseCase.validateToken(eq(token))).willReturn(true);

            // when & then
            mockMvc.perform(get("/api/v1/password/reset-validate")
                            .param("token", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valid").value(true));
        }

        @Test
        @DisplayName("만료된 토큰 검증 실패")
        void should_returnInvalid_when_tokenExpired() throws Exception {
            // given
            String token = "expired-token";

            given(resetPasswordUseCase.validateToken(eq(token))).willReturn(false);

            // when & then
            mockMvc.perform(get("/api/v1/password/reset-validate")
                            .param("token", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valid").value(false));
        }

        @Test
        @DisplayName("존재하지 않는 토큰 검증 실패")
        void should_returnInvalid_when_tokenNotFound() throws Exception {
            // given
            String token = "non-existent-token";

            given(resetPasswordUseCase.validateToken(eq(token))).willReturn(false);

            // when & then
            mockMvc.perform(get("/api/v1/password/reset-validate")
                            .param("token", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valid").value(false));
        }

        @Test
        @DisplayName("토큰 파라미터 누락 시 400 에러")
        void should_returnBadRequest_when_tokenMissing() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/password/reset-validate"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("빈 토큰 파라미터")
        void should_returnInvalid_when_tokenEmpty() throws Exception {
            // given
            String token = "";

            given(resetPasswordUseCase.validateToken(eq(token))).willReturn(false);

            // when & then
            mockMvc.perform(get("/api/v1/password/reset-validate")
                            .param("token", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valid").value(false));
        }
    }

    @Nested
    @DisplayName("비밀번호 재설정 API")
    class ResetPasswordTests {

        @Test
        @DisplayName("비밀번호 재설정 성공")
        void should_returnOk_when_resetPasswordSuccess() throws Exception {
            // given
            String token = "valid-token-123";
            String newPassword = "newPassword123!";

            willDoNothing().given(resetPasswordUseCase).resetPassword(eq(token), eq(newPassword));

            String requestBody = """
                    {
                        "token": "valid-token-123",
                        "newPassword": "newPassword123!"
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/v1/password/reset")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("비밀번호가 성공적으로 변경되었습니다."));
        }

        @Test
        @DisplayName("만료된 토큰으로 재설정 시 400 에러")
        void should_returnBadRequest_when_tokenExpired() throws Exception {
            // given
            willThrow(InvalidPasswordResetTokenException.expired())
                    .given(resetPasswordUseCase).resetPassword(anyString(), anyString());

            String requestBody = """
                    {
                        "token": "expired-token",
                        "newPassword": "newPassword123!"
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/v1/password/reset")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("이미 사용된 토큰으로 재설정 시 400 에러")
        void should_returnBadRequest_when_tokenAlreadyUsed() throws Exception {
            // given
            willThrow(InvalidPasswordResetTokenException.alreadyUsed())
                    .given(resetPasswordUseCase).resetPassword(anyString(), anyString());

            String requestBody = """
                    {
                        "token": "used-token",
                        "newPassword": "newPassword123!"
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/v1/password/reset")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("유효하지 않은 토큰으로 재설정 시 400 에러")
        void should_returnBadRequest_when_tokenNotFound() throws Exception {
            // given
            willThrow(InvalidPasswordResetTokenException.notFound())
                    .given(resetPasswordUseCase).resetPassword(anyString(), anyString());

            String requestBody = """
                    {
                        "token": "invalid-token",
                        "newPassword": "newPassword123!"
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/v1/password/reset")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("토큰 누락 시 400 에러")
        void should_returnBadRequest_when_tokenMissing() throws Exception {
            // given
            String requestBody = """
                    {
                        "newPassword": "newPassword123!"
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/v1/password/reset")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("빈 토큰 시 400 에러")
        void should_returnBadRequest_when_tokenEmpty() throws Exception {
            // given
            String requestBody = """
                    {
                        "token": "",
                        "newPassword": "newPassword123!"
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/v1/password/reset")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("새 비밀번호 누락 시 400 에러")
        void should_returnBadRequest_when_newPasswordMissing() throws Exception {
            // given
            String requestBody = """
                    {
                        "token": "valid-token"
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/v1/password/reset")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("빈 새 비밀번호 시 400 에러")
        void should_returnBadRequest_when_newPasswordEmpty() throws Exception {
            // given
            String requestBody = """
                    {
                        "token": "valid-token",
                        "newPassword": ""
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/v1/password/reset")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("너무 짧은 비밀번호 시 400 에러 (8자 미만)")
        void should_returnBadRequest_when_passwordTooShort() throws Exception {
            // given
            String requestBody = """
                    {
                        "token": "valid-token",
                        "newPassword": "short"
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/v1/password/reset")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("너무 긴 비밀번호 시 400 에러 (100자 초과)")
        void should_returnBadRequest_when_passwordTooLong() throws Exception {
            // given
            String longPassword = "a".repeat(101);
            String requestBody = String.format("""
                    {
                        "token": "valid-token",
                        "newPassword": "%s"
                    }
                    """, longPassword);

            // when & then
            mockMvc.perform(post("/api/v1/password/reset")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("정확히 8자 비밀번호 성공")
        void should_returnOk_when_password8Chars() throws Exception {
            // given
            String newPassword = "Abcd123!";

            willDoNothing().given(resetPasswordUseCase).resetPassword(anyString(), eq(newPassword));

            String requestBody = """
                    {
                        "token": "valid-token",
                        "newPassword": "Abcd123!"
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/v1/password/reset")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("정확히 100자 비밀번호 성공")
        void should_returnOk_when_password100Chars() throws Exception {
            // given: 100자 비밀번호 (대문자, 소문자, 숫자, 특수문자 포함)
            String newPassword = "Aa1!" + "a".repeat(96);

            willDoNothing().given(resetPasswordUseCase).resetPassword(anyString(), eq(newPassword));

            String requestBody = String.format("""
                    {
                        "token": "valid-token",
                        "newPassword": "%s"
                    }
                    """, newPassword);

            // when & then
            mockMvc.perform(post("/api/v1/password/reset")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("빈 요청 body 시 400 에러")
        void should_returnBadRequest_when_emptyBody() throws Exception {
            // when & then
            mockMvc.perform(post("/api/v1/password/reset")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }
}
