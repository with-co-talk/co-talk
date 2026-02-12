package com.cotalk.integration;

import com.cotalk.adapter.inbound.rest.dto.auth.LoginRequest;
import com.cotalk.adapter.inbound.rest.dto.auth.SignUpRequest;
import com.cotalk.adapter.outbound.persistence.auth.EmailVerificationTokenJpaRepository;
import com.cotalk.domain.entity.EmailVerificationToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmailVerificationTokenJpaRepository emailVerificationTokenJpaRepository;

    @Test
    @DisplayName("회원가입 후 로그인 성공")
    void should_loginSuccessfully_after_signUp() throws Exception {
        // given
        String email = "test@example.com";
        String password = "Password123!";
        String nickname = "테스트유저";

        SignUpRequest signUpRequest = new SignUpRequest(email, password, nickname);
        LoginRequest loginRequest = new LoginRequest(email, password);

        // when - 회원가입
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUpRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.message").value("회원가입이 완료되었습니다."));

        // when - 이메일 인증
        verifyEmailForUser(email);

        // when - 로그인
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();

        // then
        String responseBody = result.getResponse().getContentAsString();
        assertThat(responseBody).contains("accessToken");
        assertThat(responseBody).contains("refreshToken");
    }

    @Test
    @DisplayName("중복 이메일로 회원가입 시 실패")
    void should_failSignUp_when_duplicateEmail() throws Exception {
        // given
        String email = "duplicate@example.com";
        String password = "Password123!";

        SignUpRequest request1 = new SignUpRequest(email, password, "유저1");
        SignUpRequest request2 = new SignUpRequest(email, password, "유저2");

        // when - 첫 번째 회원가입 성공
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        // then - 두 번째 회원가입 실패 (409 Conflict)
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("이미 존재하는 이메일입니다."));
    }

    @Test
    @DisplayName("잘못된 비밀번호로 로그인 시 실패")
    void should_failLogin_when_wrongPassword() throws Exception {
        // given
        String email = "user@example.com";
        String password = "CorrectP@ss123";
        String wrongPassword = "WrongP@ss123";

        SignUpRequest signUpRequest = new SignUpRequest(email, password, "테스트");
        LoginRequest loginRequest = new LoginRequest(email, wrongPassword);

        // when - 회원가입
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUpRequest)))
                .andExpect(status().isCreated());

        // when - 이메일 인증
        verifyEmailForUser(email);

        // then - 잘못된 비밀번호로 로그인 실패
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    /**
     * 테스트용 이메일 인증 헬퍼.
     * 가장 최근 생성된 인증 토큰을 찾아 인증 API를 호출한다.
     */
    private void verifyEmailForUser(String email) throws Exception {
        EmailVerificationToken token = emailVerificationTokenJpaRepository.findAll().stream()
                .filter(t -> t.getEmail().equals(email))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Verification token not found for: " + email));

        mockMvc.perform(get("/api/v1/auth/verify-email")
                        .param("token", token.getToken()))
                .andExpect(status().isOk());
    }
}
