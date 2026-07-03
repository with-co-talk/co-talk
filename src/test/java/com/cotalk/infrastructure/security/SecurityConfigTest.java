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

    @Test
    @DisplayName("should_인증게이트_통과_when_actuator_prometheus_무인증_스크랩허용")
    void should_passSecurityGate_when_actuatorPrometheusWithoutAuth() throws Exception {
        // when & then: prometheus는 무인증 스크랩 허용(permitAll). 공개 노출은 nginx 엣지에서 차단한다.
        // health/info와 동일하게 @WebMvcTest 슬라이스에 actuator 핸들러가 없어 catch-all에 의해
        // 500으로 매핑된다. 핵심은 401(인증 차단)이 아니라는 점 → permitAll 검증.
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("should_401_반환_when_actuator_metrics_인증없이_접근")
    void should_returnUnauthorized_when_actuatorMetricsWithoutAuth() throws Exception {
        // when & then: metrics 등 그 외 actuator 엔드포인트는 여전히 ADMIN 전용 → 무인증 시 401.
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("should_인증게이트_통과_when_actuator_health_공개유지")
    void should_passSecurityGate_when_actuatorHealthPublic() throws Exception {
        // when & then: health는 공개 유지 → 인증 게이트를 통과(401 아님).
        // @WebMvcTest 슬라이스에는 actuator 핸들러가 없어 디스패치 시 NoResourceFoundException이
        // 발생하고, GlobalExceptionHandler의 catch-all(Exception)이 이를 500으로 매핑한다.
        // 핵심은 401(인증 차단)이 아니라는 점이며, 이는 health가 permitAll임을 검증한다.
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("should_인증게이트_통과_when_actuator_info_공개유지")
    void should_passSecurityGate_when_actuatorInfoPublic() throws Exception {
        // when & then: info는 공개 유지 → 인증 게이트를 통과(401 아님).
        // health와 동일하게 슬라이스에 핸들러가 없어 catch-all에 의해 500으로 매핑된다.
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isInternalServerError());
    }
}
