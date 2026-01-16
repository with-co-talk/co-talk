package com.cotalk.adapter.inbound.rest;

import com.cotalk.domain.entity.ChatRoom;
import com.cotalk.domain.entity.ChatRoomSummary;
import com.cotalk.domain.entity.Message;
import com.cotalk.domain.port.inbound.CreateChatRoomUseCase;
import com.cotalk.domain.port.inbound.GetChatRoomsUseCase;
import com.cotalk.domain.port.inbound.GetMessageHistoryUseCase;
import com.cotalk.domain.port.inbound.DeleteMessageUseCase;
import com.cotalk.domain.port.inbound.LeaveChatRoomUseCase;
import com.cotalk.domain.port.inbound.MarkAsReadUseCase;
import com.cotalk.domain.port.inbound.SendMessageUseCase;
import com.cotalk.domain.port.inbound.UpdateMessageUseCase;
import com.cotalk.infrastructure.security.JwtAuthenticationFilter;
import com.cotalk.infrastructure.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatController.class)
@AutoConfigureMockMvc(addFilters = false)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CreateChatRoomUseCase createChatRoomUseCase;

    @MockBean
    private SendMessageUseCase sendMessageUseCase;

    @MockBean
    private GetMessageHistoryUseCase getMessageHistoryUseCase;

    @MockBean
    private LeaveChatRoomUseCase leaveChatRoomUseCase;

    @MockBean
    private MarkAsReadUseCase markAsReadUseCase;

    @MockBean
    private GetChatRoomsUseCase getChatRoomsUseCase;

    @MockBean
    private UpdateMessageUseCase updateMessageUseCase;

    @MockBean
    private DeleteMessageUseCase deleteMessageUseCase;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Nested
    @DisplayName("채팅방 생성 API")
    class CreateChatRoomApi {

        @Test
        @DisplayName("유효한 요청으로 1:1 채팅방 생성 성공")
        void should_returnCreated_when_validRequest() throws Exception {
            // given
            ChatController.CreateChatRoomRequest request = new ChatController.CreateChatRoomRequest(1L, 2L);

            given(createChatRoomUseCase.createChatRoom(anyLong(), anyLong())).willReturn(100L);

            // when & then
            mockMvc.perform(post("/api/v1/chat/rooms")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.roomId").value(100L))
                    .andExpect(jsonPath("$.message").value("채팅방이 생성되었습니다."));
        }
    }

    @Nested
    @DisplayName("메시지 전송 API")
    class SendMessageApi {

        @Test
        @DisplayName("유효한 요청으로 메시지 전송 성공")
        void should_returnCreated_when_validMessage() throws Exception {
            // given
            ChatController.SendMessageRequest request = new ChatController.SendMessageRequest(
                    1L, 100L, "안녕하세요!");

            Message message = Message.builder()
                    .id(500L)
                    .senderId(1L)
                    .chatRoomId(100L)
                    .content("안녕하세요!")
                    .createdAt(LocalDateTime.now())
                    .build();

            given(sendMessageUseCase.sendMessage(anyLong(), anyLong(), anyString())).willReturn(message);

            // when & then
            mockMvc.perform(post("/api/v1/chat/messages")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.messageId").value(500L))
                    .andExpect(jsonPath("$.content").value("안녕하세요!"));
        }
    }

    @Nested
    @DisplayName("메시지 히스토리 조회 API")
    class GetMessageHistoryApi {

        @Test
        @DisplayName("커서 기반 메시지 조회 - 최신 메시지부터 (beforeMessageId 없음)")
        void should_returnLatestMessages_when_noBeforeMessageId() throws Exception {
            // given
            Long roomId = 100L;
            Long userId = 1L;
            List<Message> messages = List.of(
                    Message.builder()
                            .id(1000L)
                            .senderId(1L)
                            .chatRoomId(roomId)
                            .content("최신 메시지")
                            .createdAt(LocalDateTime.now())
                            .build(),
                    Message.builder()
                            .id(999L)
                            .senderId(2L)
                            .chatRoomId(roomId)
                            .content("이전 메시지")
                            .createdAt(LocalDateTime.now().minusMinutes(1))
                            .build()
            );

            given(getMessageHistoryUseCase.getMessageHistory(eq(roomId), eq(userId), isNull(), eq(20)))
                    .willReturn(messages);

            // when & then
            mockMvc.perform(get("/api/v1/chat/rooms/{roomId}/messages", roomId)
                            .param("userId", String.valueOf(userId))
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.messages").isArray())
                    .andExpect(jsonPath("$.messages.length()").value(2))
                    .andExpect(jsonPath("$.messages[0].id").value(1000))
                    .andExpect(jsonPath("$.messages[0].content").value("최신 메시지"))
                    .andExpect(jsonPath("$.nextCursor").value(999))
                    .andExpect(jsonPath("$.hasMore").value(false));
        }

        @Test
        @DisplayName("커서 기반 메시지 조회 - 특정 메시지 이전부터 (위로 스크롤)")
        void should_returnOlderMessages_when_beforeMessageIdProvided() throws Exception {
            // given
            Long roomId = 100L;
            Long userId = 1L;
            Long beforeMessageId = 1000L;
            int size = 20;
            
            List<Message> messages = List.of(
                    Message.builder()
                            .id(999L)
                            .senderId(2L)
                            .chatRoomId(roomId)
                            .content("이전 메시지 1")
                            .createdAt(LocalDateTime.now().minusMinutes(1))
                            .build(),
                    Message.builder()
                            .id(998L)
                            .senderId(1L)
                            .chatRoomId(roomId)
                            .content("이전 메시지 2")
                            .createdAt(LocalDateTime.now().minusMinutes(2))
                            .build()
            );

            given(getMessageHistoryUseCase.getMessageHistory(roomId, userId, beforeMessageId, size))
                    .willReturn(messages);

            // when & then
            mockMvc.perform(get("/api/v1/chat/rooms/{roomId}/messages", roomId)
                            .param("userId", String.valueOf(userId))
                            .param("beforeMessageId", String.valueOf(beforeMessageId))
                            .param("size", String.valueOf(size)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.messages").isArray())
                    .andExpect(jsonPath("$.messages.length()").value(2))
                    .andExpect(jsonPath("$.messages[0].id").value(999))
                    .andExpect(jsonPath("$.messages[1].id").value(998))
                    .andExpect(jsonPath("$.nextCursor").value(998))
                    .andExpect(jsonPath("$.hasMore").value(false));
        }

        @Test
        @DisplayName("커서 기반 메시지 조회 - 더 많은 메시지가 있을 때 hasMore가 true")
        void should_returnHasMoreTrue_when_morePagesExist() throws Exception {
            // given
            Long roomId = 100L;
            Long userId = 1L;
            int size = 2;
            
            // size와 동일한 개수의 메시지가 반환되면 hasMore = true
            List<Message> messages = List.of(
                    Message.builder()
                            .id(1000L)
                            .senderId(1L)
                            .chatRoomId(roomId)
                            .content("메시지 1")
                            .createdAt(LocalDateTime.now())
                            .build(),
                    Message.builder()
                            .id(999L)
                            .senderId(2L)
                            .chatRoomId(roomId)
                            .content("메시지 2")
                            .createdAt(LocalDateTime.now().minusMinutes(1))
                            .build()
            );

            given(getMessageHistoryUseCase.getMessageHistory(eq(roomId), eq(userId), isNull(), eq(size)))
                    .willReturn(messages);

            // when & then
            mockMvc.perform(get("/api/v1/chat/rooms/{roomId}/messages", roomId)
                            .param("userId", String.valueOf(userId))
                            .param("size", String.valueOf(size)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.messages.length()").value(2))
                    .andExpect(jsonPath("$.nextCursor").value(999))
                    .andExpect(jsonPath("$.hasMore").value(true));
        }
    }

    @Nested
    @DisplayName("채팅방 나가기 API")
    class LeaveChatRoomApi {

        @Test
        @DisplayName("유효한 요청으로 채팅방 나가기 성공")
        void should_returnOk_when_validRequest() throws Exception {
            // given
            Long roomId = 100L;
            Long userId = 1L;

            willDoNothing().given(leaveChatRoomUseCase).leaveChatRoom(anyLong(), anyLong());

            // when & then
            mockMvc.perform(post("/api/v1/chat/rooms/{roomId}/leave", roomId)
                            .param("userId", String.valueOf(userId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("채팅방을 나갔습니다."));
        }
    }

    @Nested
    @DisplayName("읽음 표시 API")
    class MarkAsReadApi {

        @Test
        @DisplayName("유효한 요청으로 읽음 표시 성공")
        void should_returnOk_when_validRequest() throws Exception {
            // given
            Long roomId = 100L;
            Long userId = 1L;

            willDoNothing().given(markAsReadUseCase).markAsRead(anyLong(), anyLong());

            // when & then
            mockMvc.perform(post("/api/v1/chat/rooms/{roomId}/read", roomId)
                            .param("userId", String.valueOf(userId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("읽음 처리되었습니다."));
        }
    }

    @Nested
    @DisplayName("채팅방 목록 조회 API")
    class GetChatRoomsApi {

        @Test
        @DisplayName("사용자의 채팅방 목록 조회 성공 - 마지막 메시지, 안읽은 개수, 상대방 정보 포함")
        void should_returnChatRoomSummaries_when_validUserId() throws Exception {
            // given
            Long userId = 1L;
            LocalDateTime now = LocalDateTime.now();
            List<ChatRoomSummary> chatRooms = List.of(
                    new ChatRoomSummary(
                            100L,
                            "채팅방1",
                            ChatRoom.ChatRoomType.DIRECT,
                            now,
                            "마지막 메시지입니다",
                            now,
                            5L,
                            2L,
                            "상대방",
                            "https://example.com/avatar.png"
                    ),
                    new ChatRoomSummary(
                            101L,
                            "채팅방2",
                            ChatRoom.ChatRoomType.DIRECT,
                            now,
                            "안녕하세요",
                            now.minusMinutes(10),
                            0L,
                            3L,
                            "다른상대방",
                            null
                    )
            );

            given(getChatRoomsUseCase.getChatRooms(userId)).willReturn(chatRooms);

            // when & then
            mockMvc.perform(get("/api/v1/chat/rooms")
                            .param("userId", String.valueOf(userId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rooms").isArray())
                    .andExpect(jsonPath("$.rooms.length()").value(2))
                    .andExpect(jsonPath("$.rooms[0].id").value(100L))
                    .andExpect(jsonPath("$.rooms[0].name").value("채팅방1"))
                    .andExpect(jsonPath("$.rooms[0].lastMessage").value("마지막 메시지입니다"))
                    .andExpect(jsonPath("$.rooms[0].unreadCount").value(5))
                    .andExpect(jsonPath("$.rooms[0].otherUserId").value(2))
                    .andExpect(jsonPath("$.rooms[0].otherUserNickname").value("상대방"))
                    .andExpect(jsonPath("$.rooms[0].otherUserAvatarUrl").value("https://example.com/avatar.png"));
        }

        @Test
        @DisplayName("채팅방이 없을 때 빈 배열 반환")
        void should_returnEmptyArray_when_noChatRooms() throws Exception {
            // given
            Long userId = 1L;
            given(getChatRoomsUseCase.getChatRooms(userId)).willReturn(List.of());

            // when & then
            mockMvc.perform(get("/api/v1/chat/rooms")
                            .param("userId", String.valueOf(userId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rooms").isArray())
                    .andExpect(jsonPath("$.rooms.length()").value(0));
        }
    }
}
