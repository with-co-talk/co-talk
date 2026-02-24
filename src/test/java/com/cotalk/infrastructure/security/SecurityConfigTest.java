package com.cotalk.infrastructure.security;

import com.cotalk.adapter.inbound.rest.AuthController;
import com.cotalk.adapter.inbound.rest.FriendController;
import com.cotalk.domain.port.inbound.auth.LoginResult;
import com.cotalk.domain.port.inbound.auth.LoginUseCase;
import com.cotalk.domain.port.inbound.auth.RefreshTokenUseCase;
import com.cotalk.domain.port.inbound.auth.SignUpUseCase;
import com.cotalk.domain.port.inbound.user.FindEmailUseCase;
import com.cotalk.domain.port.inbound.user.GetUserUseCase;
import com.cotalk.domain.port.inbound.friend.SendFriendRequestUseCase;
import com.cotalk.domain.port.inbound.friend.AcceptFriendRequestUseCase;
import com.cotalk.domain.port.inbound.friend.RejectFriendRequestUseCase;
import com.cotalk.domain.port.inbound.friend.RemoveFriendUseCase;
import com.cotalk.domain.port.inbound.friend.GetFriendListUseCase;
import com.cotalk.domain.port.inbound.friend.GetReceivedFriendRequestsUseCase;
import com.cotalk.domain.port.inbound.friend.GetSentFriendRequestsUseCase;
import com.cotalk.domain.port.inbound.friend.HideFriendUseCase;
import com.cotalk.domain.port.inbound.friend.UnhideFriendUseCase;
import com.cotalk.domain.port.inbound.friend.GetHiddenFriendsUseCase;
import com.cotalk.domain.port.outbound.UserRepository;
import com.cotalk.infrastructure.config.properties.AppProperties;
import com.cotalk.infrastructure.config.properties.JwtProperties;
import com.cotalk.infrastructure.ratelimit.RateLimitTestConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AuthController.class, FriendController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtTokenProvider.class, RateLimitTestConfiguration.class})
@EnableConfigurationProperties({JwtProperties.class, AppProperties.class})
@TestPropertySource(properties = {
        "jwt.secret=test-secret-key-for-testing-purposes-only-minimum-32-chars",
        "jwt.expiration=3600000",
        "app.cors.allowed-origins=http://localhost:3000"
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    // Inbound ports
    @MockitoBean
    private SignUpUseCase signUpUseCase;

    @MockitoBean
    private LoginUseCase loginUseCase;

    @MockitoBean
    private RefreshTokenUseCase refreshTokenUseCase;

    @MockitoBean
    private SecurityContextHelper securityContextHelper;

    @MockitoBean
    private SendFriendRequestUseCase sendFriendRequestUseCase;

    @MockitoBean
    private AcceptFriendRequestUseCase acceptFriendRequestUseCase;

    @MockitoBean
    private RejectFriendRequestUseCase rejectFriendRequestUseCase;

    @MockitoBean
    private RemoveFriendUseCase removeFriendUseCase;

    @MockitoBean
    private GetFriendListUseCase getFriendListUseCase;

    @MockitoBean
    private GetReceivedFriendRequestsUseCase getReceivedFriendRequestsUseCase;

    @MockitoBean
    private GetSentFriendRequestsUseCase getSentFriendRequestsUseCase;

    @MockitoBean
    private HideFriendUseCase hideFriendUseCase;

    @MockitoBean
    private UnhideFriendUseCase unhideFriendUseCase;

    @MockitoBean
    private GetHiddenFriendsUseCase getHiddenFriendsUseCase;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private FindEmailUseCase findEmailUseCase;

    @MockitoBean
    private GetUserUseCase getUserUseCase;

    @Test
    @DisplayName("인증 없이 회원가입 API 접근 가능")
    void should_allowAccess_when_signUpWithoutAuth() throws Exception {
        // given
        given(signUpUseCase.signUp(anyString(), anyString(), anyString(), any())).willReturn(1L);

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
        given(loginUseCase.login(anyString(), anyString())).willReturn(new LoginResult("jwt-token", 1L, 3600L));
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
