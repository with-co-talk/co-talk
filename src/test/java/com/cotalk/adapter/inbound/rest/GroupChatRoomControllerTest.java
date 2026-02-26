package com.cotalk.adapter.inbound.rest;

import com.cotalk.adapter.inbound.rest.dto.chatroom.CreateGroupChatRoomRequest;
import com.cotalk.adapter.inbound.rest.dto.chatroom.InviteMembersRequest;
import com.cotalk.adapter.inbound.rest.dto.chatroom.UpdateChatRoomNameRequest;
import com.cotalk.domain.entity.ChatRoom;
import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.port.inbound.chatroom.ChatRoomManagementUseCase;
import com.cotalk.domain.port.inbound.chatroom.CreateGroupChatRoomUseCase;
import com.cotalk.domain.port.inbound.chatroom.InviteGroupChatMemberUseCase;
import com.cotalk.domain.port.inbound.chatroom.KickChatRoomMemberUseCase;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 그룹 채팅방 컨트롤러 단위 테스트.
 * <p>
 * 그룹 채팅방 생성, 멤버 초대, 이름 변경, 멤버 강제 퇴장 등의 엔드포인트를 테스트한다.
 *
 * @author seunggu.lee
 */
@WebMvcTest(GroupChatRoomController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(RateLimitTestConfiguration.class)
class GroupChatRoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CreateGroupChatRoomUseCase createGroupChatRoomUseCase;

    @MockitoBean
    private InviteGroupChatMemberUseCase inviteGroupChatMemberUseCase;

    @MockitoBean
    private ChatRoomManagementUseCase chatRoomManagementUseCase;

    @MockitoBean
    private KickChatRoomMemberUseCase kickChatRoomMemberUseCase;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Nested
    @DisplayName("그룹 채팅방 생성 API")
    class CreateGroupChatRoomApi {

        @Test
        @DisplayName("유효한 요청으로 그룹 채팅방 생성 성공")
        @WithMockCustomUser(userId = 1L)
        void should_returnCreated_when_validRequest() throws Exception {
            // given
            Long creatorId = 1L;
            String roomName = "친구들과의 채팅방";
            List<Long> memberIds = List.of(2L, 3L, 4L);
            CreateGroupChatRoomRequest request = new CreateGroupChatRoomRequest(roomName, memberIds);

            Long roomId = 100L;
            given(createGroupChatRoomUseCase.createGroupChatRoom(eq(creatorId), eq(roomName), anyList()))
                    .willReturn(roomId);

            // when & then
            mockMvc.perform(post("/api/v1/chat/rooms/group")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.roomId").value(roomId))
                    .andExpect(jsonPath("$.message").value("그룹 채팅방이 생성되었습니다."));
        }
    }

    @Nested
    @DisplayName("그룹 채팅방 멤버 초대 API")
    class InviteMembersApi {

        @Test
        @DisplayName("유효한 요청으로 멤버 초대 성공")
        @WithMockCustomUser(userId = 1L)
        void should_returnOk_when_validInvite() throws Exception {
            // given
            Long roomId = 100L;
            Long inviterId = 1L;
            List<Long> inviteeIds = List.of(5L, 6L);
            InviteMembersRequest request = new InviteMembersRequest(inviteeIds);

            willDoNothing().given(inviteGroupChatMemberUseCase).inviteMembers(roomId, inviterId, inviteeIds);

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
        @DisplayName("관리자가 채팅방 이름 변경 성공")
        @WithMockCustomUser(userId = 1L)
        void should_returnOk_when_adminUpdatesName() throws Exception {
            // given
            Long roomId = 100L;
            Long adminId = 1L;
            String newName = "새로운 채팅방 이름";
            UpdateChatRoomNameRequest request = new UpdateChatRoomNameRequest(newName);

            ChatRoom chatRoom = ChatRoom.builder()
                    .id(roomId)
                    .name(newName)
                    .type(ChatRoom.ChatRoomType.GROUP)
                    .build();

            given(chatRoomManagementUseCase.updateChatRoomName(roomId, adminId, newName))
                    .willReturn(chatRoom);

            // when & then
            mockMvc.perform(put("/api/v1/chat/rooms/{roomId}/name", roomId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value(newName))
                    .andExpect(jsonPath("$.message").value("채팅방 이름이 변경되었습니다."));
        }
    }

    @Nested
    @DisplayName("멤버 강제 퇴장 API")
    class KickMemberApi {

        @Test
        @DisplayName("관리자가 멤버 강제 퇴장 성공")
        @WithMockCustomUser(userId = 1L)
        void should_returnOk_when_adminKicksMember() throws Exception {
            // given
            Long roomId = 100L;
            Long adminId = 1L;
            Long targetUserId = 5L;

            willDoNothing().given(kickChatRoomMemberUseCase).kickMember(roomId, adminId, targetUserId);

            // when & then
            mockMvc.perform(delete("/api/v1/chat/rooms/{roomId}/members/{targetUserId}", roomId, targetUserId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("멤버가 강제 퇴장되었습니다."));
        }
    }

    @Nested
    @DisplayName("관리자 권한 관리 API")
    class AdminManagementApi {

        @Test
        @DisplayName("관리자가 멤버를 관리자로 임명 성공")
        @WithMockCustomUser(userId = 1L)
        void should_returnOk_when_promoteToAdmin() throws Exception {
            // given
            Long roomId = 100L;
            Long adminId = 1L;
            Long targetUserId = 5L;

            ChatRoomMember member = ChatRoomMember.builder()
                    .userId(targetUserId)
                    .chatRoomId(roomId)
                    .role(ChatRoomMember.MemberRole.ADMIN)
                    .build();

            given(chatRoomManagementUseCase.promoteToAdmin(roomId, adminId, targetUserId))
                    .willReturn(member);

            // when & then
            mockMvc.perform(post("/api/v1/chat/rooms/{roomId}/admins/{targetUserId}", roomId, targetUserId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(targetUserId))
                    .andExpect(jsonPath("$.role").value("ADMIN"))
                    .andExpect(jsonPath("$.message").value("관리자로 임명되었습니다."));
        }
    }
}
