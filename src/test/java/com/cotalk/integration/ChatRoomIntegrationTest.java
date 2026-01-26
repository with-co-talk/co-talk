package com.cotalk.integration;

import com.cotalk.adapter.inbound.rest.dto.auth.SignUpRequest;
import com.cotalk.adapter.inbound.rest.dto.chatroom.CreateChatRoomRequest;
import com.cotalk.adapter.inbound.rest.dto.chatroom.CreateGroupChatRoomRequest;
import com.cotalk.adapter.inbound.rest.dto.chatroom.InviteMembersRequest;
import com.cotalk.adapter.inbound.rest.dto.friend.SendFriendRequestRequest;
import com.cotalk.config.TestRedisConfiguration;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
@Import(TestRedisConfiguration.class)
class ChatRoomIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Long user1Id;
    private Long user2Id;
    private Long user3Id;

    @BeforeEach
    void setUp() throws Exception {
        user1Id = createUser("chat1@test.com", "Password123!", "채팅유저1");
        user2Id = createUser("chat2@test.com", "Password123!", "채팅유저2");
        user3Id = createUser("chat3@test.com", "Password123!", "채팅유저3");

        // 친구 관계 생성
        makeFriends(user1Id, user2Id);
        makeFriends(user1Id, user3Id);
        makeFriends(user2Id, user3Id);
    }

    private Long createUser(String email, String password, String nickname) throws Exception {
        SignUpRequest request = new SignUpRequest(email, password, nickname);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("userId").asLong();
    }

    private void makeFriends(Long userId1, Long userId2) throws Exception {
        setSecurityContext(userId1);
        SendFriendRequestRequest sendRequest = new SendFriendRequestRequest(userId2);

        MvcResult result = mockMvc.perform(post("/api/v1/friends/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        Long requestId = response.get("requestId").asLong();

        setSecurityContext(userId2);
        mockMvc.perform(post("/api/v1/friends/requests/{requestId}/accept", requestId))
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
    @DisplayName("1:1 채팅방 생성 및 조회")
    void should_createAndGetDirectChatRoom() throws Exception {
        // 1. 1:1 채팅방 생성
        CreateChatRoomRequest createRequest = new CreateChatRoomRequest(user1Id, user2Id);

        MvcResult createResult = mockMvc.perform(post("/api/v1/chat/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomId").exists())
                .andReturn();

        JsonNode response = objectMapper.readTree(createResult.getResponse().getContentAsString());
        Long chatRoomId = response.get("roomId").asLong();

        // 2. user1의 채팅방 목록 조회
        setSecurityContext(user1Id);
        mockMvc.perform(get("/api/v1/chat/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rooms").isArray())
                .andExpect(jsonPath("$.rooms.length()").value(1))
                .andExpect(jsonPath("$.rooms[0].id").value(chatRoomId));

        // 3. user2의 채팅방 목록 조회
        setSecurityContext(user2Id);
        mockMvc.perform(get("/api/v1/chat/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rooms").isArray())
                .andExpect(jsonPath("$.rooms.length()").value(1));
    }

    @Test
    @DisplayName("그룹 채팅방 생성 및 멤버 초대")
    void should_createGroupChatAndInviteMembers() throws Exception {
        // user4를 추가로 생성하고 친구 관계 설정
        Long user4Id = createUser("chat4@test.com", "Password123!", "채팅유저4");
        makeFriends(user1Id, user4Id);
        makeFriends(user2Id, user4Id);
        makeFriends(user3Id, user4Id);

        // 1. 그룹 채팅방 생성 (최소 3명 이상 필요)
        CreateGroupChatRoomRequest createRequest = new CreateGroupChatRoomRequest(
                user1Id,
                "테스트 그룹",
                List.of(user2Id, user3Id)
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/chat/rooms/group")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomId").exists())
                .andReturn();

        JsonNode response = objectMapper.readTree(createResult.getResponse().getContentAsString());
        Long chatRoomId = response.get("roomId").asLong();

        // 2. user4 초대
        InviteMembersRequest inviteRequest = new InviteMembersRequest(user1Id, List.of(user4Id));

        mockMvc.perform(post("/api/v1/chat/rooms/{roomId}/invite", chatRoomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inviteRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("멤버를 초대했습니다."));

        // 3. user4의 채팅방 목록에서 확인
        mockMvc.perform(get("/api/v1/chat/rooms")
                        .param("userId", user4Id.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rooms").isArray())
                .andExpect(jsonPath("$.rooms.length()").value(1))
                .andExpect(jsonPath("$.rooms[0].name").value("테스트 그룹"));
    }

    @Test
    @DisplayName("채팅방 나가기")
    void should_leaveChatRoom() throws Exception {
        // 1. 그룹 채팅방 생성
        CreateGroupChatRoomRequest createRequest = new CreateGroupChatRoomRequest(
                user1Id,
                "나가기 테스트 그룹",
                List.of(user2Id, user3Id)
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/chat/rooms/group")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode response = objectMapper.readTree(createResult.getResponse().getContentAsString());
        Long chatRoomId = response.get("roomId").asLong();

        // 2. user2가 채팅방 나가기
        setSecurityContext(user2Id);
        mockMvc.perform(post("/api/v1/chat/rooms/{roomId}/leave", chatRoomId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("채팅방을 나갔습니다."));

        // 3. user2의 채팅방 목록에서 사라짐
        mockMvc.perform(get("/api/v1/chat/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rooms.length()").value(0));
    }

    @Test
    @DisplayName("같은 1:1 채팅방 중복 생성 시 기존 채팅방 반환")
    void should_returnExistingChatRoom_when_duplicateDirectChat() throws Exception {
        // 1. 첫 번째 1:1 채팅방 생성
        CreateChatRoomRequest createRequest1 = new CreateChatRoomRequest(user1Id, user2Id);

        MvcResult result1 = mockMvc.perform(post("/api/v1/chat/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest1)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode response1 = objectMapper.readTree(result1.getResponse().getContentAsString());
        Long chatRoomId1 = response1.get("roomId").asLong();

        // 2. 같은 사용자로 다시 채팅방 생성 시도
        CreateChatRoomRequest createRequest2 = new CreateChatRoomRequest(user2Id, user1Id);

        MvcResult result2 = mockMvc.perform(post("/api/v1/chat/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest2)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode response2 = objectMapper.readTree(result2.getResponse().getContentAsString());
        Long chatRoomId2 = response2.get("roomId").asLong();

        // 3. 같은 채팅방 ID여야 함
        assertThat(chatRoomId1).isEqualTo(chatRoomId2);
    }
}
