package com.cotalk.integration;

import com.cotalk.adapter.inbound.rest.dto.auth.SignUpRequest;
import com.cotalk.adapter.inbound.rest.dto.chatroom.CreateChatRoomRequest;
import com.cotalk.adapter.inbound.rest.dto.friend.SendFriendRequestRequest;
import com.cotalk.adapter.inbound.rest.dto.message.SendMessageRequest;
import com.cotalk.adapter.inbound.rest.dto.message.UpdateMessageRequest;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@Import(TestRedisConfiguration.class)
class MessageIntegrationTest extends PostgresIntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Long user1Id;
    private Long user2Id;
    private Long chatRoomId;

    @BeforeEach
    void setUp() throws Exception {
        user1Id = createUser("msg1@test.com", "Password123!", "메시지유저1");
        user2Id = createUser("msg2@test.com", "Password123!", "메시지유저2");
        makeFriends(user1Id, user2Id);
        chatRoomId = createChatRoom(user1Id, user2Id);
    }

    private Long createUser(String email, String password, String nickname) throws Exception {
        SignUpRequest request = new SignUpRequest(email, password, nickname, null);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("userId").asLong();
    }

    private void makeFriends(Long userId1, Long userId2) throws Exception {
        SendFriendRequestRequest sendRequest = new SendFriendRequestRequest(userId2);

        setSecurityContext(userId1);
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

    private Long createChatRoom(Long userId1, Long userId2) throws Exception {
        CreateChatRoomRequest request = new CreateChatRoomRequest(userId2);

        setSecurityContext(userId1);
        MvcResult result = mockMvc.perform(post("/api/v1/chat/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("roomId").asLong();
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
    @DisplayName("메시지 전송 및 조회")
    void should_sendAndGetMessages() throws Exception {
        // 1. 메시지 전송
        SendMessageRequest sendRequest = new SendMessageRequest(chatRoomId, "안녕하세요!");

        setSecurityContext(user1Id);
        mockMvc.perform(post("/api/v1/chat/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.messageId").exists())
                .andExpect(jsonPath("$.content").value("안녕하세요!"));

        // 2. 두 번째 메시지 전송
        SendMessageRequest sendRequest2 = new SendMessageRequest(chatRoomId, "반갑습니다!");

        setSecurityContext(user2Id);
        mockMvc.perform(post("/api/v1/chat/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendRequest2)))
                .andExpect(status().isCreated());

        // 3. 메시지 히스토리 조회
        setSecurityContext(user1Id);
        mockMvc.perform(get("/api/v1/chat/messages/rooms/{roomId}", chatRoomId)
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages").isArray())
                .andExpect(jsonPath("$.messages.length()").value(2));
    }

    @Test
    @DisplayName("메시지 수정")
    void should_updateMessage() throws Exception {
        // 1. 메시지 전송
        SendMessageRequest sendRequest = new SendMessageRequest(chatRoomId, "원본 메시지");

        setSecurityContext(user1Id);
        MvcResult sendResult = mockMvc.perform(post("/api/v1/chat/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode response = objectMapper.readTree(sendResult.getResponse().getContentAsString());
        Long messageId = response.get("messageId").asLong();

        // 2. 메시지 수정
        UpdateMessageRequest updateRequest = new UpdateMessageRequest("수정된 메시지");

        setSecurityContext(user1Id);
        mockMvc.perform(put("/api/v1/chat/messages/{messageId}", messageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("수정된 메시지"));
    }

    @Test
    @DisplayName("메시지 삭제")
    void should_deleteMessage() throws Exception {
        // 1. 메시지 전송
        SendMessageRequest sendRequest = new SendMessageRequest(chatRoomId, "삭제될 메시지");

        setSecurityContext(user1Id);
        MvcResult sendResult = mockMvc.perform(post("/api/v1/chat/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode response = objectMapper.readTree(sendResult.getResponse().getContentAsString());
        Long messageId = response.get("messageId").asLong();

        // 2. 메시지 삭제
        setSecurityContext(user1Id);
        mockMvc.perform(delete("/api/v1/chat/messages/{messageId}", messageId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("메시지가 삭제되었습니다."));

        // 3. 삭제된 메시지는 히스토리에서 제외됨
        mockMvc.perform(get("/api/v1/chat/messages/rooms/{roomId}", chatRoomId)
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages.length()").value(0));
    }

    @Test
    @DisplayName("메시지 검색 (블라인드 인덱스 3글자 이상 부분일치)")
    void should_searchMessages() throws Exception {
        // 1. 여러 메시지 전송 — 두 메시지가 "안녕하세" 3-gram을 공유
        String[] messages = {"안녕하세요", "반갑습니다", "오늘 날씨가 좋네요", "다들 안녕하세 인사"};
        setSecurityContext(user1Id);
        for (String msg : messages) {
            SendMessageRequest request = new SendMessageRequest(chatRoomId, msg);
            mockMvc.perform(post("/api/v1/chat/messages")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        // 2. "안녕하세" 키워드로 검색 (3글자 이상 — 블라인드 인덱스 부분일치)
        setSecurityContext(user1Id);
        mockMvc.perform(get("/api/v1/messages/search")
                        .param("chatRoomId", chatRoomId.toString())
                        .param("keyword", "안녕하세")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages").isArray())
                .andExpect(jsonPath("$.messages.length()").value(2));
    }

    @Test
    @DisplayName("3글자 미만 검색어는 400을 반환한다")
    void should_returnBadRequest_when_keywordTooShort() throws Exception {
        SendMessageRequest request = new SendMessageRequest(chatRoomId, "안녕하세요");
        setSecurityContext(user1Id);
        mockMvc.perform(post("/api/v1/chat/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        setSecurityContext(user1Id);
        mockMvc.perform(get("/api/v1/messages/search")
                        .param("chatRoomId", chatRoomId.toString())
                        .param("keyword", "안녕")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("타인의 메시지 수정 실패")
    void should_failUpdateMessage_when_notOwner() throws Exception {
        // 1. user1이 메시지 전송
        SendMessageRequest sendRequest = new SendMessageRequest(chatRoomId, "user1의 메시지");

        setSecurityContext(user1Id);
        MvcResult sendResult = mockMvc.perform(post("/api/v1/chat/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode response = objectMapper.readTree(sendResult.getResponse().getContentAsString());
        Long messageId = response.get("messageId").asLong();

        // 2. user2가 수정 시도
        UpdateMessageRequest updateRequest = new UpdateMessageRequest("해킹 시도!");

        setSecurityContext(user2Id);
        mockMvc.perform(put("/api/v1/chat/messages/{messageId}", messageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());
    }
}
