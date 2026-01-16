package com.cotalk.adapter.inbound.rest;

import com.cotalk.domain.entity.ChatRoom;
import com.cotalk.domain.entity.ChatRoomSummary;
import com.cotalk.domain.port.inbound.CreateChatRoomUseCase;
import com.cotalk.domain.port.inbound.GetChatRoomsUseCase;
import com.cotalk.domain.port.inbound.LeaveChatRoomUseCase;
import com.cotalk.domain.port.inbound.MarkAsReadUseCase;
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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatRoomController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(RateLimitTestConfiguration.class)
class ChatRoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CreateChatRoomUseCase createChatRoomUseCase;

    @MockBean
    private GetChatRoomsUseCase getChatRoomsUseCase;

    @MockBean
    private LeaveChatRoomUseCase leaveChatRoomUseCase;

    @MockBean
    private MarkAsReadUseCase markAsReadUseCase;

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
            ChatRoomController.CreateChatRoomRequest request = new ChatRoomController.CreateChatRoomRequest(1L, 2L);

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
}
