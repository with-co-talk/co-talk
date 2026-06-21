package com.cotalk.integration;

import com.cotalk.adapter.inbound.rest.dto.auth.SignUpRequest;
import com.cotalk.adapter.inbound.rest.dto.friend.SendFriendRequestRequest;
import com.cotalk.adapter.outbound.persistence.auth.EmailVerificationTokenJpaRepository;
import com.cotalk.adapter.outbound.persistence.entity.EmailVerificationTokenJpaEntity;
import com.cotalk.config.TestRedisConfiguration;
import com.cotalk.domain.model.Email;
import com.cotalk.infrastructure.security.CustomUserPrincipal;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@Import(TestRedisConfiguration.class)
class FriendIntegrationTest extends PostgresIntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmailVerificationTokenJpaRepository emailVerificationTokenJpaRepository;

    private Long user1Id;
    private Long user2Id;

    @BeforeEach
    void setUp() throws Exception {
        // 두 명의 사용자 생성 및 이메일 인증 (인증 후 setSecurityContext로 테스트)
        user1Id = createUserAndGetId("user1@test.com", "Password123!", "사용자1");
        verifyEmailForUser("user1@test.com");

        user2Id = createUserAndGetId("user2@test.com", "Password123!", "사용자2");
        verifyEmailForUser("user2@test.com");
    }

    private Long createUserAndGetId(String email, String password, String nickname) throws Exception {
        SignUpRequest request = new SignUpRequest(email, password, nickname, null);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("userId").asLong();
    }

    private void verifyEmailForUser(String email) throws Exception {
        EmailVerificationTokenJpaEntity token = emailVerificationTokenJpaRepository.findAll().stream()
                .filter(t -> t.getEmail().equals(new Email(email)))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Verification token not found for: " + email));

        mockMvc.perform(get("/api/v1/auth/verify-email")
                        .param("token", token.getToken()))
                .andExpect(status().isOk());
    }

    private void setSecurityContext(Long userId) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        CustomUserPrincipal principal = new CustomUserPrincipal(
                userId,
                "USER",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        context.setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
        SecurityContextHolder.setContext(context);
    }

    @Test
    @DisplayName("친구 요청 전체 플로우 - 요청, 수락, 목록 조회")
    void should_completeFriendRequestFlow() throws Exception {
        // 1. 친구 요청 전송
        setSecurityContext(user1Id);
        SendFriendRequestRequest sendRequest = new SendFriendRequestRequest(user2Id);

        MvcResult requestResult = mockMvc.perform(post("/api/v1/friends/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("친구 요청이 전송되었습니다."))
                .andReturn();

        JsonNode requestResponse = objectMapper.readTree(requestResult.getResponse().getContentAsString());
        Long requestId = requestResponse.get("requestId").asLong();

        // 2. 친구 요청 수락
        setSecurityContext(user2Id);
        mockMvc.perform(post("/api/v1/friends/requests/{requestId}/accept", requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("친구 요청을 수락했습니다."));

        // 3. 친구 목록 조회 - user1
        setSecurityContext(user1Id);
        mockMvc.perform(get("/api/v1/friends"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.friends").isArray())
                .andExpect(jsonPath("$.friends.length()").value(1))
                .andExpect(jsonPath("$.friends[0].user.nickname").value("사용자2"));

        // 4. 친구 목록 조회 - user2
        setSecurityContext(user2Id);
        mockMvc.perform(get("/api/v1/friends"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.friends").isArray())
                .andExpect(jsonPath("$.friends.length()").value(1))
                .andExpect(jsonPath("$.friends[0].user.nickname").value("사용자1"));
    }

    @Test
    @DisplayName("친구 요청 거절 플로우")
    void should_rejectFriendRequest() throws Exception {
        // 1. 친구 요청 전송
        setSecurityContext(user1Id);
        SendFriendRequestRequest sendRequest = new SendFriendRequestRequest(user2Id);

        MvcResult requestResult = mockMvc.perform(post("/api/v1/friends/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode requestResponse = objectMapper.readTree(requestResult.getResponse().getContentAsString());
        Long requestId = requestResponse.get("requestId").asLong();

        // 2. 친구 요청 거절
        setSecurityContext(user2Id);
        mockMvc.perform(post("/api/v1/friends/requests/{requestId}/reject", requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("친구 요청을 거절했습니다."));

        // 3. 친구 목록이 비어있어야 함
        setSecurityContext(user1Id);
        mockMvc.perform(get("/api/v1/friends"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.friends").isArray())
                .andExpect(jsonPath("$.friends.length()").value(0));
    }

    @Test
    @DisplayName("친구 삭제 플로우")
    void should_removeFriend() throws Exception {
        // 1. 친구 관계 생성
        setSecurityContext(user1Id);
        SendFriendRequestRequest sendRequest = new SendFriendRequestRequest(user2Id);

        MvcResult requestResult = mockMvc.perform(post("/api/v1/friends/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode requestResponse = objectMapper.readTree(requestResult.getResponse().getContentAsString());
        Long requestId = requestResponse.get("requestId").asLong();

        setSecurityContext(user2Id);
        mockMvc.perform(post("/api/v1/friends/requests/{requestId}/accept", requestId))
                .andExpect(status().isOk());

        // 2. 친구 삭제
        setSecurityContext(user1Id);
        mockMvc.perform(delete("/api/v1/friends/{friendId}", user2Id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("친구가 삭제되었습니다."));

        // 3. 양쪽 모두 친구 목록이 비어있어야 함
        mockMvc.perform(get("/api/v1/friends"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.friends.length()").value(0));

        setSecurityContext(user2Id);
        mockMvc.perform(get("/api/v1/friends"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.friends.length()").value(0));
    }

    @Test
    @DisplayName("자기 자신에게 친구 요청 실패")
    void should_failSendFriendRequest_when_self() throws Exception {
        setSecurityContext(user1Id);
        SendFriendRequestRequest request = new SendFriendRequestRequest(user1Id);

        mockMvc.perform(post("/api/v1/friends/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
