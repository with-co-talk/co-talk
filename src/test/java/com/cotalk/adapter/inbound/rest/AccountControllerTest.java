package com.cotalk.adapter.inbound.rest;

import com.cotalk.domain.exception.PasswordMismatchException;
import com.cotalk.domain.exception.UserNotFoundException;
import com.cotalk.domain.port.inbound.user.DeleteAccountUseCase;
import com.cotalk.infrastructure.exception.GlobalExceptionHandler;
import com.cotalk.infrastructure.ratelimit.RateLimitTestConfiguration;
import com.cotalk.infrastructure.security.JwtAuthenticationFilter;
import com.cotalk.infrastructure.security.JwtTokenProvider;
import com.cotalk.infrastructure.security.WithMockCustomUser;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({RateLimitTestConfiguration.class, GlobalExceptionHandler.class})
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DeleteAccountUseCase deleteAccountUseCase;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Nested
    @DisplayName("회원 탈퇴 API")
    class DeleteAccountTests {

        @Test
        @DisplayName("회원 탈퇴 성공")
        @WithMockCustomUser(userId = 1L)
        void should_returnOk_when_deleteAccountSuccess() throws Exception {
            // given
            Long userId = 1L;
            String password = "correctPassword123!";

            willDoNothing().given(deleteAccountUseCase).deleteAccount(eq(userId), eq(password));

            String requestBody = """
                    {
                        "password": "correctPassword123!"
                    }
                    """;

            // when & then
            mockMvc.perform(delete("/api/v1/account")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("회원 탈퇴가 완료되었습니다."));
        }

        @Test
        @DisplayName("잘못된 비밀번호로 탈퇴 시 400 에러")
        @WithMockCustomUser(userId = 1L)
        void should_returnUnauthorized_when_wrongPassword() throws Exception {
            // given
            Long userId = 1L;
            String wrongPassword = "wrongPassword";

            willThrow(new PasswordMismatchException())
                    .given(deleteAccountUseCase).deleteAccount(eq(userId), eq(wrongPassword));

            String requestBody = """
                    {
                        "password": "wrongPassword"
                    }
                    """;

            // when & then
            mockMvc.perform(delete("/api/v1/account")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("PASSWORD_MISMATCH"));
        }

        @Test
        @DisplayName("존재하지 않는 사용자 탈퇴 시 404 에러")
        @WithMockCustomUser(userId = 999L)
        void should_returnNotFound_when_userNotFound() throws Exception {
            // given
            Long userId = 999L;
            String password = "anyPassword";

            willThrow(new UserNotFoundException("사용자를 찾을 수 없습니다."))
                    .given(deleteAccountUseCase).deleteAccount(eq(userId), eq(password));

            String requestBody = """
                    {
                        "password": "anyPassword"
                    }
                    """;

            // when & then
            mockMvc.perform(delete("/api/v1/account")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("비밀번호 누락 시 400 에러")
        @WithMockCustomUser(userId = 1L)
        void should_returnBadRequest_when_passwordMissing() throws Exception {
            // given
            String requestBody = "{}";

            // when & then
            mockMvc.perform(delete("/api/v1/account")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("빈 비밀번호 시 400 에러")
        @WithMockCustomUser(userId = 1L)
        void should_returnBadRequest_when_passwordEmpty() throws Exception {
            // given
            String requestBody = """
                    {
                        "password": ""
                    }
                    """;

            // when & then
            mockMvc.perform(delete("/api/v1/account")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("공백 비밀번호 시 400 에러")
        @WithMockCustomUser(userId = 1L)
        void should_returnBadRequest_when_passwordBlank() throws Exception {
            // given
            String requestBody = """
                    {
                        "password": "   "
                    }
                    """;

            // when & then
            mockMvc.perform(delete("/api/v1/account")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }


        @Test
        @DisplayName("request body 없이 요청 시 500 에러 (HttpMessageNotReadable)")
        @WithMockCustomUser(userId = 1L)
        void should_returnInternalError_when_noRequestBody() throws Exception {
            // given
            // when & then - body 없이 요청 시 HttpMessageNotReadableException 발생
            mockMvc.perform(delete("/api/v1/account")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("null password 시 400 에러")
        @WithMockCustomUser(userId = 1L)
        void should_returnBadRequest_when_passwordNull() throws Exception {
            // given
            String requestBody = """
                    {
                        "password": null
                    }
                    """;

            // when & then
            mockMvc.perform(delete("/api/v1/account")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("특수문자 포함 비밀번호로 탈퇴 성공")
        @WithMockCustomUser(userId = 1L)
        void should_returnOk_when_passwordWithSpecialChars() throws Exception {
            // given
            Long userId = 1L;
            String password = "P@ssw0rd!#$%";

            willDoNothing().given(deleteAccountUseCase).deleteAccount(eq(userId), eq(password));

            String requestBody = """
                    {
                        "password": "P@ssw0rd!#$%"
                    }
                    """;

            // when & then
            mockMvc.perform(delete("/api/v1/account")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("회원 탈퇴가 완료되었습니다."));
        }

        @Test
        @DisplayName("한글 비밀번호로 탈퇴 성공")
        @WithMockCustomUser(userId = 1L)
        void should_returnOk_when_koreanPassword() throws Exception {
            // given
            Long userId = 1L;
            String password = "한글비밀번호123";

            willDoNothing().given(deleteAccountUseCase).deleteAccount(eq(userId), eq(password));

            String requestBody = """
                    {
                        "password": "한글비밀번호123"
                    }
                    """;

            // when & then
            mockMvc.perform(delete("/api/v1/account")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("회원 탈퇴가 완료되었습니다."));
        }

        @Test
        @DisplayName("매우 긴 비밀번호로 탈퇴")
        @WithMockCustomUser(userId = 1L)
        void should_handleLongPassword() throws Exception {
            // given
            Long userId = 1L;
            String longPassword = "a".repeat(500);

            willDoNothing().given(deleteAccountUseCase).deleteAccount(eq(userId), eq(longPassword));

            String requestBody = String.format("""
                    {
                        "password": "%s"
                    }
                    """, longPassword);

            // when & then
            mockMvc.perform(delete("/api/v1/account")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("음수 userId 시에도 정상 요청 처리")
        @WithMockCustomUser(userId = -1L)
        void should_processRequest_when_negativeUserId() throws Exception {
            // given
            Long userId = -1L;
            String password = "validPassword";

            willThrow(new UserNotFoundException("사용자를 찾을 수 없습니다."))
                    .given(deleteAccountUseCase).deleteAccount(eq(userId), eq(password));

            String requestBody = """
                    {
                        "password": "validPassword"
                    }
                    """;

            // when & then
            mockMvc.perform(delete("/api/v1/account")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("0 userId 시에도 정상 요청 처리")
        @WithMockCustomUser(userId = 0L)
        void should_processRequest_when_zeroUserId() throws Exception {
            // given
            Long userId = 0L;
            String password = "validPassword";

            willThrow(new UserNotFoundException("사용자를 찾을 수 없습니다."))
                    .given(deleteAccountUseCase).deleteAccount(eq(userId), eq(password));

            String requestBody = """
                    {
                        "password": "validPassword"
                    }
                    """;

            // when & then
            mockMvc.perform(delete("/api/v1/account")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isNotFound());
        }
    }
}
