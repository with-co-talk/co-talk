package com.cotalk.integration;

import com.cotalk.adapter.inbound.rest.dto.auth.LoginRequest;
import com.cotalk.adapter.inbound.rest.dto.auth.SignUpRequest;
import com.cotalk.adapter.inbound.rest.dto.friend.SendFriendRequestRequest;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(IntegrationTestSecurityConfig.class)
class FriendIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Long user1Id;
    private Long user2Id;
    private String user1Token;
    private String user2Token;

    @BeforeEach
    void setUp() throws Exception {
        // 두 명의 사용자 생성 및 로그인
        user1Id = createUserAndGetId("user1@test.com", "password123", "사용자1");
        user1Token = loginAndGetToken("user1@test.com", "password123");

        user2Id = createUserAndGetId("user2@test.com", "password123", "사용자2");
        user2Token = loginAndGetToken("user2@test.com", "password123");
    }

    private Long createUserAndGetId(String email, String password, String nickname) throws Exception {
        SignUpRequest request = new SignUpRequest(email, password, nickname);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("userId").asLong();
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        LoginRequest request = new LoginRequest(email, password);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("token").asText();
    }

    @Test
    @DisplayName("친구 요청 전체 플로우 - 요청, 수락, 목록 조회")
    void should_completeFriendRequestFlow() throws Exception {
        // 1. 친구 요청 전송
        SendFriendRequestRequest sendRequest = new SendFriendRequestRequest(user1Id, user2Id);

        MvcResult requestResult = mockMvc.perform(post("/api/v1/friends/requests")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("친구 요청이 전송되었습니다."))
                .andReturn();

        JsonNode requestResponse = objectMapper.readTree(requestResult.getResponse().getContentAsString());
        Long requestId = requestResponse.get("requestId").asLong();

        // 2. 친구 요청 수락
        mockMvc.perform(post("/api/v1/friends/requests/{requestId}/accept", requestId)
                        .header("Authorization", "Bearer " + user2Token)
                        .param("userId", user2Id.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("친구 요청을 수락했습니다."));

        // 3. 친구 목록 조회 - user1
        mockMvc.perform(get("/api/v1/friends")
                        .header("Authorization", "Bearer " + user1Token)
                        .param("userId", user1Id.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.friends").isArray())
                .andExpect(jsonPath("$.friends.length()").value(1))
                .andExpect(jsonPath("$.friends[0].nickname").value("사용자2"));

        // 4. 친구 목록 조회 - user2
        mockMvc.perform(get("/api/v1/friends")
                        .header("Authorization", "Bearer " + user2Token)
                        .param("userId", user2Id.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.friends").isArray())
                .andExpect(jsonPath("$.friends.length()").value(1))
                .andExpect(jsonPath("$.friends[0].nickname").value("사용자1"));
    }

    @Test
    @DisplayName("친구 요청 거절 플로우")
    void should_rejectFriendRequest() throws Exception {
        // 1. 친구 요청 전송
        SendFriendRequestRequest sendRequest = new SendFriendRequestRequest(user1Id, user2Id);

        MvcResult requestResult = mockMvc.perform(post("/api/v1/friends/requests")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode requestResponse = objectMapper.readTree(requestResult.getResponse().getContentAsString());
        Long requestId = requestResponse.get("requestId").asLong();

        // 2. 친구 요청 거절
        mockMvc.perform(post("/api/v1/friends/requests/{requestId}/reject", requestId)
                        .header("Authorization", "Bearer " + user2Token)
                        .param("userId", user2Id.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("친구 요청을 거절했습니다."));

        // 3. 친구 목록이 비어있어야 함
        mockMvc.perform(get("/api/v1/friends")
                        .header("Authorization", "Bearer " + user1Token)
                        .param("userId", user1Id.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.friends").isArray())
                .andExpect(jsonPath("$.friends.length()").value(0));
    }

    @Test
    @DisplayName("친구 삭제 플로우")
    void should_removeFriend() throws Exception {
        // 1. 친구 관계 생성
        SendFriendRequestRequest sendRequest = new SendFriendRequestRequest(user1Id, user2Id);

        MvcResult requestResult = mockMvc.perform(post("/api/v1/friends/requests")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode requestResponse = objectMapper.readTree(requestResult.getResponse().getContentAsString());
        Long requestId = requestResponse.get("requestId").asLong();

        mockMvc.perform(post("/api/v1/friends/requests/{requestId}/accept", requestId)
                        .header("Authorization", "Bearer " + user2Token)
                        .param("userId", user2Id.toString()))
                .andExpect(status().isOk());

        // 2. 친구 삭제
        mockMvc.perform(delete("/api/v1/friends/{friendId}", user2Id)
                        .header("Authorization", "Bearer " + user1Token)
                        .param("userId", user1Id.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("친구가 삭제되었습니다."));

        // 3. 양쪽 모두 친구 목록이 비어있어야 함
        mockMvc.perform(get("/api/v1/friends")
                        .header("Authorization", "Bearer " + user1Token)
                        .param("userId", user1Id.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.friends.length()").value(0));

        mockMvc.perform(get("/api/v1/friends")
                        .header("Authorization", "Bearer " + user2Token)
                        .param("userId", user2Id.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.friends.length()").value(0));
    }

    @Test
    @DisplayName("자기 자신에게 친구 요청 실패")
    void should_failSendFriendRequest_when_self() throws Exception {
        SendFriendRequestRequest request = new SendFriendRequestRequest(user1Id, user1Id);

        mockMvc.perform(post("/api/v1/friends/requests")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
