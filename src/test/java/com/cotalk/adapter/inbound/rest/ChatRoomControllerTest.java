package com.cotalk.adapter.inbound.rest;

import com.cotalk.adapter.inbound.rest.dto.chatroom.CreateChatRoomRequest;
import com.cotalk.domain.entity.ChatRoom;
import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.entity.ChatRoomSummary;
import com.cotalk.domain.port.inbound.chatroom.CreateChatRoomUseCase;
import com.cotalk.domain.port.inbound.chatroom.GetChatRoomMembersUseCase;
import com.cotalk.domain.port.inbound.chatroom.GetChatRoomUseCase;
import com.cotalk.domain.port.inbound.chatroom.GetChatRoomsUseCase;
import com.cotalk.domain.port.inbound.chatroom.LeaveChatRoomUseCase;
import com.cotalk.domain.port.inbound.chatroom.ReinviteDirectChatMemberUseCase;
import com.cotalk.domain.port.inbound.message.MarkAsReadUseCase;
import com.cotalk.infrastructure.exception.GlobalExceptionHandler;
import com.cotalk.infrastructure.ratelimit.RateLimitTestConfiguration;
import com.cotalk.infrastructure.security.JwtAuthenticationFilter;
import com.cotalk.infrastructure.security.JwtTokenProvider;
import com.cotalk.infrastructure.security.WithMockCustomUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatRoomController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({RateLimitTestConfiguration.class, GlobalExceptionHandler.class})
class ChatRoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CreateChatRoomUseCase createChatRoomUseCase;

    @MockitoBean
    private GetChatRoomsUseCase getChatRoomsUseCase;

    @MockitoBean
    private GetChatRoomUseCase getChatRoomUseCase;

    @MockitoBean
    private LeaveChatRoomUseCase leaveChatRoomUseCase;

    @MockitoBean
    private MarkAsReadUseCase markAsReadUseCase;

    @MockitoBean
    private GetChatRoomMembersUseCase getChatRoomMembersUseCase;

    @MockitoBean
    private ReinviteDirectChatMemberUseCase reinviteDirectChatMemberUseCase;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Nested
    @DisplayName("채팅방 생성 API")
    class CreateChatRoomApi {

        @Test
        @DisplayName("유효한 요청으로 1:1 채팅방 생성 성공")
        @WithMockCustomUser(userId = 1L)
        void should_returnCreated_when_validRequest() throws Exception {
            // given
            CreateChatRoomRequest request = new CreateChatRoomRequest(2L);

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
        @DisplayName("사용자의 채팅방 목록 조회 성공 - 페이지네이션 메타데이터 포함")
        @WithMockCustomUser(userId = 1L)
        void should_returnChatRoomSummaries_when_validUserId() throws Exception {
            // given
            Long userId = 1L;
            LocalDateTime now = LocalDateTime.now();
            List<ChatRoomSummary> chatRooms = List.of(
                    new ChatRoomSummary(
                            100L,
                            "채팅방1",
                            null,
                            ChatRoom.ChatRoomType.DIRECT,
                            now,
                            "마지막 메시지입니다",
                            "TEXT",
                            now,
                            5L,
                            2L,
                            "상대방",
                            "https://example.com/avatar.png",
                            false,
                            true,
                            now
                    ),
                    new ChatRoomSummary(
                            101L,
                            "채팅방2",
                            null,
                            ChatRoom.ChatRoomType.DIRECT,
                            now,
                            "안녕하세요",
                            "TEXT",
                            now.minusMinutes(10),
                            0L,
                            3L,
                            "다른상대방",
                            null,
                            false,
                            false,
                            null
                    )
            );

            Page<ChatRoomSummary> chatRoomPage = new PageImpl<>(chatRooms, PageRequest.of(0, 20), 2);
            given(getChatRoomsUseCase.getChatRooms(eq(userId), any(Pageable.class))).willReturn(chatRoomPage);

            // when & then
            mockMvc.perform(get("/api/v1/chat/rooms")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rooms").isArray())
                    .andExpect(jsonPath("$.rooms.length()").value(2))
                    .andExpect(jsonPath("$.rooms[0].id").value(100L))
                    .andExpect(jsonPath("$.rooms[0].name").value("채팅방1"))
                    .andExpect(jsonPath("$.rooms[0].lastMessage").value("마지막 메시지입니다"))
                    .andExpect(jsonPath("$.rooms[0].unreadCount").value(5))
                    .andExpect(jsonPath("$.rooms[0].otherUserId").value(2))
                    .andExpect(jsonPath("$.rooms[0].otherUserNickname").value("상대방"))
                    .andExpect(jsonPath("$.rooms[0].otherUserAvatarUrl").value("https://example.com/avatar.png"))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(20))
                    .andExpect(jsonPath("$.totalElements").value(2))
                    .andExpect(jsonPath("$.totalPages").value(1));
        }

        @Test
        @DisplayName("채팅방이 없을 때 빈 배열과 페이지네이션 메타데이터 반환")
        @WithMockCustomUser(userId = 1L)
        void should_returnEmptyArray_when_noChatRooms() throws Exception {
            // given
            Long userId = 1L;
            Page<ChatRoomSummary> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
            given(getChatRoomsUseCase.getChatRooms(eq(userId), any(Pageable.class))).willReturn(emptyPage);

            // when & then
            mockMvc.perform(get("/api/v1/chat/rooms"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rooms").isArray())
                    .andExpect(jsonPath("$.rooms.length()").value(0))
                    .andExpect(jsonPath("$.totalElements").value(0))
                    .andExpect(jsonPath("$.totalPages").value(0));
        }

        @Test
        @DisplayName("페이지네이션 파라미터로 DB 레벨 페이지네이션 동작")
        @WithMockCustomUser(userId = 1L)
        void should_paginateAtDbLevel_when_pageAndSizeProvided() throws Exception {
            // given
            Long userId = 1L;
            LocalDateTime now = LocalDateTime.now();
            ChatRoomSummary room = new ChatRoomSummary(
                    100L, "채팅방", null, ChatRoom.ChatRoomType.DIRECT, now,
                    "msg", "TEXT", now, 0L, 2L, "상대방", null, false, false, null
            );

            Page<ChatRoomSummary> chatRoomPage = new PageImpl<>(List.of(room), PageRequest.of(1, 10), 25);
            given(getChatRoomsUseCase.getChatRooms(eq(userId), any(Pageable.class))).willReturn(chatRoomPage);

            // when & then
            mockMvc.perform(get("/api/v1/chat/rooms")
                            .param("page", "1")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rooms.length()").value(1))
                    .andExpect(jsonPath("$.page").value(1))
                    .andExpect(jsonPath("$.size").value(10))
                    .andExpect(jsonPath("$.totalElements").value(25))
                    .andExpect(jsonPath("$.totalPages").value(3));
        }
    }

    @Nested
    @DisplayName("채팅방 멤버 목록 조회 API")
    class GetChatRoomMembersApi {

        @Test
        @DisplayName("채팅방 멤버 목록 조회 성공")
        @WithMockCustomUser(userId = 1L)
        void should_returnMembers_when_validRequest() throws Exception {
            // given
            Long roomId = 100L;
            Long userId = 1L;

            List<GetChatRoomMembersUseCase.MemberInfo> members = List.of(
                    new GetChatRoomMembersUseCase.MemberInfo(
                            1L, "관리자", "https://example.com/admin.png", ChatRoomMember.MemberRole.ADMIN),
                    new GetChatRoomMembersUseCase.MemberInfo(
                            2L, "멤버1", null, ChatRoomMember.MemberRole.MEMBER)
            );

            given(getChatRoomMembersUseCase.getChatRoomMembers(roomId, userId)).willReturn(members);

            // when & then
            mockMvc.perform(get("/api/v1/chat/rooms/{roomId}/members", roomId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.members").isArray())
                    .andExpect(jsonPath("$.members.length()").value(2))
                    .andExpect(jsonPath("$.members[0].userId").value(1))
                    .andExpect(jsonPath("$.members[0].nickname").value("관리자"))
                    .andExpect(jsonPath("$.members[0].role").value("ADMIN"))
                    .andExpect(jsonPath("$.members[1].userId").value(2))
                    .andExpect(jsonPath("$.members[1].role").value("MEMBER"));
        }

        @Test
        @DisplayName("빈 멤버 목록 조회 성공")
        @WithMockCustomUser(userId = 1L)
        void should_returnEmptyList_when_noMembers() throws Exception {
            // given
            Long roomId = 100L;
            Long userId = 1L;

            given(getChatRoomMembersUseCase.getChatRoomMembers(roomId, userId)).willReturn(List.of());

            // when & then
            mockMvc.perform(get("/api/v1/chat/rooms/{roomId}/members", roomId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.members").isArray())
                    .andExpect(jsonPath("$.members.length()").value(0));
        }
    }

    @Nested
    @DisplayName("채팅방 나가기 API")
    class LeaveChatRoomApi {

        @Test
        @DisplayName("유효한 요청으로 채팅방 나가기 성공")
        @WithMockCustomUser(userId = 1L)
        void should_returnOk_when_validRequest() throws Exception {
            // given
            Long roomId = 100L;

            willDoNothing().given(leaveChatRoomUseCase).leaveChatRoom(anyLong(), anyLong());

            // when & then
            mockMvc.perform(post("/api/v1/chat/rooms/{roomId}/leave", roomId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("채팅방을 나갔습니다."));
        }
    }

    @Nested
    @DisplayName("읽음 표시 API")
    class MarkAsReadApi {

        @Test
        @DisplayName("유효한 요청으로 읽음 표시 성공")
        @WithMockCustomUser(userId = 1L)
        void should_returnOk_when_validRequest() throws Exception {
            // given
            Long roomId = 100L;

            willDoNothing().given(markAsReadUseCase).markAsRead(anyLong(), anyLong());

            // when & then
            mockMvc.perform(post("/api/v1/chat/rooms/{roomId}/read", roomId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("읽음 처리되었습니다."));
        }
    }

}
