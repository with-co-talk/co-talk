package com.cotalk.adapter.inbound.rest;

import com.cotalk.adapter.inbound.rest.dto.auth.ResendVerificationRequest;
import com.cotalk.domain.exception.InvalidEmailVerificationTokenException;
import com.cotalk.domain.port.inbound.auth.ResendVerificationUseCase;
import com.cotalk.domain.port.inbound.auth.VerifyEmailUseCase;
import com.cotalk.infrastructure.ratelimit.RateLimitTestConfiguration;
import com.cotalk.infrastructure.security.JwtAuthenticationFilter;
import com.cotalk.infrastructure.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 이메일 인증 컨트롤러 단위 테스트.
 * <p>
 * 이메일 인증 및 인증 이메일 재발송 엔드포인트를 테스트한다.
 *
 * @author seunggu.lee
 */
@WebMvcTest(EmailVerificationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(RateLimitTestConfiguration.class)
class EmailVerificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private VerifyEmailUseCase verifyEmailUseCase;

    @MockitoBean
    private ResendVerificationUseCase resendVerificationUseCase;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Nested
    @DisplayName("이메일 인증 API")
    class VerifyEmailApi {

        @Test
        @DisplayName("유효한 토큰으로 이메일 인증 성공")
        void should_returnOk_when_validToken() throws Exception {
            // given
            String token = "valid-token-123";
            willDoNothing().given(verifyEmailUseCase).verifyEmail(token);

            // when & then
            mockMvc.perform(get("/api/v1/auth/verify-email")
                            .param("token", token))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("이메일 인증이 완료되었습니다.")));
        }

        @Test
        @DisplayName("이미 인증된 토큰이면 안내 HTML 반환")
        void should_returnHtmlNotice_when_alreadyVerified() throws Exception {
            // given
            String token = "verified-token-123";
            willThrow(InvalidEmailVerificationTokenException.alreadyVerified())
                    .given(verifyEmailUseCase).verifyEmail(token);

            // when & then
            mockMvc.perform(get("/api/v1/auth/verify-email")
                            .param("token", token))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("이미 인증이 완료된 이메일입니다.")));
        }

        @Test
        @DisplayName("유효하지 않은 토큰이면 에러 안내 HTML 반환")
        void should_returnHtmlError_when_invalidToken() throws Exception {
            // given
            String token = "invalid-token-123";
            willThrow(InvalidEmailVerificationTokenException.notFound())
                    .given(verifyEmailUseCase).verifyEmail(token);

            // when & then
            mockMvc.perform(get("/api/v1/auth/verify-email")
                            .param("token", token))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("유효하지 않은 이메일 인증 링크입니다.")));
        }
    }

    @Nested
    @DisplayName("인증 이메일 재발송 API")
    class ResendVerificationApi {

        @Test
        @DisplayName("유효한 이메일로 재발송 요청 성공")
        void should_returnOk_when_validEmail() throws Exception {
            // given
            String email = "user@example.com";
            ResendVerificationRequest request = new ResendVerificationRequest(email);
            willDoNothing().given(resendVerificationUseCase).resendVerification(email);

            // when & then
            mockMvc.perform(post("/api/v1/auth/resend-verification")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("인증 이메일이 발송되었습니다. 이메일을 확인해주세요."));
        }

        @Test
        @DisplayName("이메일 존재 여부와 관계없이 동일한 응답 반환")
        void should_returnOk_when_emailNotExists() throws Exception {
            // given
            String email = "nonexistent@example.com";
            ResendVerificationRequest request = new ResendVerificationRequest(email);
            willDoNothing().given(resendVerificationUseCase).resendVerification(email);

            // when & then
            mockMvc.perform(post("/api/v1/auth/resend-verification")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("인증 이메일이 발송되었습니다. 이메일을 확인해주세요."));
        }
    }
}
