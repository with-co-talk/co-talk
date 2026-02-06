package com.cotalk.adapter.inbound.rest;

import com.cotalk.adapter.inbound.rest.dto.chatroom.CreateChatRoomRequest;
import com.cotalk.adapter.inbound.rest.dto.chatroom.CreateGroupChatRoomRequest;
import com.cotalk.adapter.inbound.rest.dto.chatroom.InviteMembersRequest;
import com.cotalk.adapter.inbound.rest.dto.chatroom.SetAnnouncementRequest;
import com.cotalk.adapter.inbound.rest.dto.chatroom.UpdateChatRoomNameRequest;
import com.cotalk.domain.entity.ChatRoom;
import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.entity.ChatRoomSummary;
import com.cotalk.domain.port.inbound.chatroom.ChatRoomManagementUseCase;
import com.cotalk.domain.port.inbound.chatroom.CreateChatRoomUseCase;
import com.cotalk.domain.port.inbound.chatroom.CreateGroupChatRoomUseCase;
import com.cotalk.domain.port.inbound.chatroom.GetChatRoomMembersUseCase;
import com.cotalk.domain.port.inbound.chatroom.GetChatRoomUseCase;
import com.cotalk.domain.port.inbound.chatroom.GetChatRoomsUseCase;
import com.cotalk.domain.port.inbound.chatroom.InviteGroupChatMemberUseCase;
import com.cotalk.domain.port.inbound.chatroom.KickChatRoomMemberUseCase;
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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

    @MockBean
    private CreateChatRoomUseCase createChatRoomUseCase;

    @MockBean
    private GetChatRoomsUseCase getChatRoomsUseCase;

    @MockBean
    private GetChatRoomUseCase getChatRoomUseCase;

    @MockBean
    private LeaveChatRoomUseCase leaveChatRoomUseCase;

    @MockBean
    private MarkAsReadUseCase markAsReadUseCase;

    @MockBean
    private CreateGroupChatRoomUseCase createGroupChatRoomUseCase;

    @MockBean
    private InviteGroupChatMemberUseCase inviteGroupChatMemberUseCase;

    @MockBean
    private ChatRoomManagementUseCase chatRoomManagementUseCase;

    @MockBean
    private GetChatRoomMembersUseCase getChatRoomMembersUseCase;

    @MockBean
    private KickChatRoomMemberUseCase kickChatRoomMemberUseCase;

    @MockBean
    private ReinviteDirectChatMemberUseCase reinviteDirectChatMemberUseCase;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
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
        @DisplayName("사용자의 채팅방 목록 조회 성공 - 마지막 메시지, 안읽은 개수, 상대방 정보 포함")
        @WithMockCustomUser(userId = 1L)
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

            given(getChatRoomsUseCase.getChatRooms(userId)).willReturn(chatRooms);

            // when & then
            mockMvc.perform(get("/api/v1/chat/rooms"))
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
        @WithMockCustomUser(userId = 1L)
        void should_returnEmptyArray_when_noChatRooms() throws Exception {
            // given
            Long userId = 1L;
            given(getChatRoomsUseCase.getChatRooms(userId)).willReturn(List.of());

            // when & then
            mockMvc.perform(get("/api/v1/chat/rooms"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rooms").isArray())
                    .andExpect(jsonPath("$.rooms.length()").value(0));
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
            Long userId = 1L;

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
            Long userId = 1L;

            willDoNothing().given(markAsReadUseCase).markAsRead(anyLong(), anyLong());

            // when & then
            mockMvc.perform(post("/api/v1/chat/rooms/{roomId}/read", roomId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("읽음 처리되었습니다."));
        }
    }

    @Nested
    @DisplayName("그룹 채팅방 생성 API")
    class CreateGroupChatRoomApi {

        @Test
        @DisplayName("유효한 요청으로 그룹 채팅방 생성 성공")
        @WithMockCustomUser(userId = 1L)
        void should_returnCreated_when_validRequest() throws Exception {
            // given
            CreateGroupChatRoomRequest request =
                    new CreateGroupChatRoomRequest("개발팀 채팅방", List.of(2L, 3L, 4L));

            given(createGroupChatRoomUseCase.createGroupChatRoom(eq(1L), eq("개발팀 채팅방"), any()))
                    .willReturn(100L);

            // when & then
            mockMvc.perform(post("/api/v1/chat/rooms/group")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.roomId").value(100L))
                    .andExpect(jsonPath("$.message").value("그룹 채팅방이 생성되었습니다."));
        }

        @Test
        @DisplayName("채팅방 이름이 없으면 400 에러")
        @WithMockCustomUser(userId = 1L)
        void should_returnBadRequest_when_roomNameMissing() throws Exception {
            // given
            CreateGroupChatRoomRequest request =
                    new CreateGroupChatRoomRequest(null, List.of(2L, 3L));

            // when & then
            mockMvc.perform(post("/api/v1/chat/rooms/group")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("그룹 채팅방 멤버 초대 API")
    class InviteGroupChatMemberApi {

        @Test
        @DisplayName("유효한 요청으로 멤버 초대 성공")
        @WithMockCustomUser(userId = 1L)
        void should_returnOk_when_validRequest() throws Exception {
            // given
            Long roomId = 100L;
            InviteMembersRequest request = new InviteMembersRequest(List.of(5L, 6L));

            willDoNothing().given(inviteGroupChatMemberUseCase).inviteMembers(eq(roomId), eq(1L), any());

            // when & then
            mockMvc.perform(post("/api/v1/chat/rooms/{roomId}/invite", roomId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("멤버를 초대했습니다."));
        }
    }

    @Nested
    @DisplayName("채팅방 이름 변경 API")
    class UpdateChatRoomNameApi {

        @Test
        @DisplayName("유효한 요청으로 채팅방 이름 변경 성공")
        @WithMockCustomUser(userId = 1L)
        void should_returnOk_when_validRequest() throws Exception {
            // given
            Long roomId = 100L;
            UpdateChatRoomNameRequest request = UpdateChatRoomNameRequest.of("새로운 채팅방 이름");

            ChatRoom chatRoom = ChatRoom.builder()
                    .id(roomId)
                    .name("새로운 채팅방 이름")
                    .type(ChatRoom.ChatRoomType.GROUP)
                    .build();

            given(chatRoomManagementUseCase.updateChatRoomName(eq(roomId), eq(1L), eq("새로운 채팅방 이름")))
                    .willReturn(chatRoom);

            // when & then
            mockMvc.perform(put("/api/v1/chat/rooms/{roomId}/name", roomId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("새로운 채팅방 이름"))
                    .andExpect(jsonPath("$.message").value("채팅방 이름이 변경되었습니다."));
        }

        @Test
        @DisplayName("이름이 비어있으면 400 에러")
        @WithMockCustomUser(userId = 1L)
        void should_returnBadRequest_when_nameEmpty() throws Exception {
            // given
            Long roomId = 100L;
            UpdateChatRoomNameRequest request = UpdateChatRoomNameRequest.of("");

            // when & then
            mockMvc.perform(put("/api/v1/chat/rooms/{roomId}/name", roomId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("공지사항 설정 API")
    class SetAnnouncementApi {

        @Test
        @DisplayName("유효한 요청으로 공지사항 설정 성공")
        @WithMockCustomUser(userId = 1L)
        void should_returnOk_when_validRequest() throws Exception {
            // given
            Long roomId = 100L;
            SetAnnouncementRequest request = SetAnnouncementRequest.of("중요 공지사항입니다.");

            ChatRoom chatRoom = ChatRoom.builder()
                    .id(roomId)
                    .name("채팅방")
                    .type(ChatRoom.ChatRoomType.GROUP)
                    .announcement("중요 공지사항입니다.")
                    .build();

            given(chatRoomManagementUseCase.setAnnouncement(eq(roomId), eq(1L), eq("중요 공지사항입니다.")))
                    .willReturn(chatRoom);

            // when & then
            mockMvc.perform(post("/api/v1/chat/rooms/{roomId}/announcement", roomId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.announcement").value("중요 공지사항입니다."))
                    .andExpect(jsonPath("$.message").value("공지사항이 설정되었습니다."));
        }

        @Test
        @DisplayName("공지사항이 비어있으면 400 에러")
        @WithMockCustomUser(userId = 1L)
        void should_returnBadRequest_when_announcementEmpty() throws Exception {
            // given
            Long roomId = 100L;
            SetAnnouncementRequest request = SetAnnouncementRequest.of("");

            // when & then
            mockMvc.perform(post("/api/v1/chat/rooms/{roomId}/announcement", roomId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("공지사항 삭제 API")
    class ClearAnnouncementApi {

        @Test
        @DisplayName("유효한 요청으로 공지사항 삭제 성공")
        @WithMockCustomUser(userId = 1L)
        void should_returnOk_when_validRequest() throws Exception {
            // given
            Long roomId = 100L;
            Long userId = 1L;

            ChatRoom chatRoom = ChatRoom.builder()
                    .id(roomId)
                    .name("채팅방")
                    .type(ChatRoom.ChatRoomType.GROUP)
                    .announcement(null)
                    .build();

            given(chatRoomManagementUseCase.clearAnnouncement(roomId, userId)).willReturn(chatRoom);

            // when & then
            mockMvc.perform(delete("/api/v1/chat/rooms/{roomId}/announcement", roomId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("공지사항이 삭제되었습니다."));
        }
    }

    @Nested
    @DisplayName("관리자 임명 API")
    class PromoteToAdminApi {

        @Test
        @DisplayName("유효한 요청으로 관리자 임명 성공")
        @WithMockCustomUser(userId = 1L)
        void should_returnOk_when_validRequest() throws Exception {
            // given
            Long roomId = 100L;
            Long targetUserId = 2L;
            Long userId = 1L;

            ChatRoomMember member = ChatRoomMember.builder()
                    .id(200L)
                    .chatRoomId(roomId)
                    .userId(targetUserId)
                    .role(ChatRoomMember.MemberRole.ADMIN)
                    .build();

            given(chatRoomManagementUseCase.promoteToAdmin(roomId, userId, targetUserId))
                    .willReturn(member);

            // when & then
            mockMvc.perform(post("/api/v1/chat/rooms/{roomId}/admins/{targetUserId}", roomId, targetUserId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(targetUserId))
                    .andExpect(jsonPath("$.role").value("ADMIN"))
                    .andExpect(jsonPath("$.message").value("관리자로 임명되었습니다."));
        }
    }

    @Nested
    @DisplayName("관리자 해제 API")
    class DemoteFromAdminApi {

        @Test
        @DisplayName("유효한 요청으로 관리자 해제 성공")
        @WithMockCustomUser(userId = 1L)
        void should_returnOk_when_validRequest() throws Exception {
            // given
            Long roomId = 100L;
            Long targetUserId = 2L;
            Long userId = 1L;

            ChatRoomMember member = ChatRoomMember.builder()
                    .id(200L)
                    .chatRoomId(roomId)
                    .userId(targetUserId)
                    .role(ChatRoomMember.MemberRole.MEMBER)
                    .build();

            given(chatRoomManagementUseCase.demoteFromAdmin(roomId, userId, targetUserId))
                    .willReturn(member);

            // when & then
            mockMvc.perform(delete("/api/v1/chat/rooms/{roomId}/admins/{targetUserId}", roomId, targetUserId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(targetUserId))
                    .andExpect(jsonPath("$.role").value("MEMBER"))
                    .andExpect(jsonPath("$.message").value("관리자 권한이 해제되었습니다."));
        }
    }

    @Nested
    @DisplayName("멤버 강제 퇴장 API")
    class KickMemberApi {

        @Test
        @DisplayName("유효한 요청으로 멤버 강제 퇴장 성공")
        @WithMockCustomUser(userId = 1L)
        void should_returnOk_when_validRequest() throws Exception {
            // given
            Long roomId = 100L;
            Long targetUserId = 2L;
            Long userId = 1L;

            willDoNothing().given(kickChatRoomMemberUseCase).kickMember(roomId, userId, targetUserId);

            // when & then
            mockMvc.perform(delete("/api/v1/chat/rooms/{roomId}/members/{targetUserId}", roomId, targetUserId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("멤버가 강제 퇴장되었습니다."));
        }
    }
}
