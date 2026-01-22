package com.cotalk.infrastructure.security;

import com.cotalk.adapter.inbound.rest.AuthController;
import com.cotalk.adapter.inbound.rest.FriendController;
import com.cotalk.domain.port.inbound.auth.LoginUseCase;
import com.cotalk.domain.port.inbound.auth.RefreshTokenUseCase;
import com.cotalk.domain.port.inbound.auth.SignUpUseCase;
import com.cotalk.domain.port.inbound.friend.AcceptFriendRequestUseCase;
import com.cotalk.domain.port.inbound.friend.GetFriendListUseCase;
import com.cotalk.domain.port.inbound.friend.GetReceivedFriendRequestsUseCase;
import com.cotalk.domain.port.inbound.friend.GetSentFriendRequestsUseCase;
import com.cotalk.domain.port.inbound.friend.RejectFriendRequestUseCase;
import com.cotalk.domain.port.inbound.friend.RemoveFriendUseCase;
import com.cotalk.domain.port.inbound.friend.SendFriendRequestUseCase;
import com.cotalk.domain.port.outbound.UserRepository;
import com.cotalk.infrastructure.ratelimit.RateLimitTestConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AuthController.class, FriendController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtTokenProvider.class, RateLimitTestConfiguration.class})
@TestPropertySource(properties = {
        "jwt.secret=test-secret-key-for-testing-purposes-only-minimum-32-chars",
        "jwt.expiration=3600000"
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    // Inbound ports
    @MockBean
    private SignUpUseCase signUpUseCase;

    @MockBean
    private LoginUseCase loginUseCase;

    @MockBean
    private RefreshTokenUseCase refreshTokenUseCase;

    @MockBean
    private SecurityContextHelper securityContextHelper;

    @MockBean
    private SendFriendRequestUseCase sendFriendRequestUseCase;

    @MockBean
    private AcceptFriendRequestUseCase acceptFriendRequestUseCase;

    @MockBean
    private RejectFriendRequestUseCase rejectFriendRequestUseCase;

    @MockBean
    private RemoveFriendUseCase removeFriendUseCase;

    @MockBean
    private GetFriendListUseCase getFriendListUseCase;

    @MockBean
    private GetReceivedFriendRequestsUseCase getReceivedFriendRequestsUseCase;

    @MockBean
    private GetSentFriendRequestsUseCase getSentFriendRequestsUseCase;

    @MockBean
    private UserRepository userRepository;

    @Test
    @DisplayName("인증 없이 회원가입 API 접근 가능")
    void should_allowAccess_when_signUpWithoutAuth() throws Exception {
        // given
        given(signUpUseCase.signUp(anyString(), anyString(), anyString())).willReturn(1L);

        String requestBody = """
                {
                    "email": "test@example.com",
                    "password": "Password123!",
                    "nickname": "테스트"
                }
                """;

        // when & then
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("인증 없이 로그인 API 접근 가능")
    void should_allowAccess_when_loginWithoutAuth() throws Exception {
        // given
        given(loginUseCase.login(anyString(), anyString())).willReturn("jwt-token");
        given(loginUseCase.getUserIdByEmail(anyString())).willReturn(1L);
        given(refreshTokenUseCase.createRefreshToken(1L)).willReturn("refresh-token");

        String requestBody = """
                {
                    "email": "test@example.com",
                    "password": "Password123!"
                }
                """;

        // when & then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("인증 없이 보호된 리소스 접근 시 401 반환")
    void should_returnUnauthorized_when_accessProtectedResourceWithoutAuth() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/friends")
                        .param("userId", "1"))
                .andExpect(status().isUnauthorized());
    }
}
