package com.cotalk.application.service.chatroom;

import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.ChatRoomAccessDeniedException;
import com.cotalk.domain.port.inbound.chatroom.GetChatRoomMembersUseCase;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.UserRepository;
import com.cotalk.domain.validator.ChatRoomMemberValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;

/**
 * 채팅방 멤버 목록 조회 서비스 테스트.
 *
 * @author seunggu.lee
 */
@ExtendWith(MockitoExtension.class)
class GetChatRoomMembersServiceTest {

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatRoomMemberValidator chatRoomMemberValidator;

    @InjectMocks
    private GetChatRoomMembersService getChatRoomMembersService;

    private Long chatRoomId;
    private Long requestUserId;

    @BeforeEach
    void setUp() {
        chatRoomId = 1L;
        requestUserId = 100L;
    }

    @Nested
    @DisplayName("채팅방 멤버 목록 조회")
    class GetChatRoomMembers {

        @Test
        @DisplayName("채팅방 멤버가 멤버 목록을 성공적으로 조회한다")
        void should_returnMembers_when_validMember() {
            // given
            ChatRoomMember requestMember = ChatRoomMember.builder()
                    .id(1L)
                    .chatRoomId(chatRoomId)
                    .userId(requestUserId)
                    .role(ChatRoomMember.MemberRole.MEMBER)
                    .build();

            ChatRoomMember member1 = ChatRoomMember.builder()
                    .id(2L)
                    .chatRoomId(chatRoomId)
                    .userId(200L)
                    .role(ChatRoomMember.MemberRole.ADMIN)
                    .build();

            ChatRoomMember member2 = ChatRoomMember.builder()
                    .id(3L)
                    .chatRoomId(chatRoomId)
                    .userId(300L)
                    .role(ChatRoomMember.MemberRole.MEMBER)
                    .build();

            User user1 = User.builder()
                    .id(200L)
                    .email("user1@example.com")
                    .nickname("사용자1")
                    .passwordHash("hash")
                    .avatarUrl("https://example.com/avatar1.png")
                    .build();

            User user2 = User.builder()
                    .id(300L)
                    .email("user2@example.com")
                    .nickname("사용자2")
                    .passwordHash("hash")
                    .build();

            User requestUser = User.builder()
                    .id(requestUserId)
                    .email("requester@example.com")
                    .nickname("요청자")
                    .passwordHash("hash")
                    .build();

            given(chatRoomMemberValidator.getMemberOrThrow(chatRoomId, requestUserId))
                    .willReturn(requestMember);
            given(chatRoomMemberRepository.findByChatRoomId(chatRoomId))
                    .willReturn(List.of(requestMember, member1, member2));
            given(userRepository.findAllById(List.of(requestUserId, 200L, 300L)))
                    .willReturn(List.of(requestUser, user1, user2));

            // when
            List<GetChatRoomMembersUseCase.MemberInfo> result =
                    getChatRoomMembersService.getChatRoomMembers(chatRoomId, requestUserId);

            // then
            assertThat(result).hasSize(3);
            assertThat(result).extracting(GetChatRoomMembersUseCase.MemberInfo::userId)
                    .containsExactlyInAnyOrder(requestUserId, 200L, 300L);
        }

        @Test
        @DisplayName("채팅방 멤버가 아닌 사용자가 조회 시 실패한다")
        void should_throwException_when_notMember() {
            // given
            doThrow(new ChatRoomAccessDeniedException("채팅방 멤버가 아닙니다."))
                    .when(chatRoomMemberValidator).getMemberOrThrow(chatRoomId, requestUserId);

            // when & then
            assertThatThrownBy(() ->
                    getChatRoomMembersService.getChatRoomMembers(chatRoomId, requestUserId))
                    .isInstanceOf(ChatRoomAccessDeniedException.class);
        }

        @Test
        @DisplayName("관리자가 먼저 정렬되어 반환된다")
        void should_returnAdminsFirst_when_membersExist() {
            // given
            ChatRoomMember member = ChatRoomMember.builder()
                    .id(1L)
                    .chatRoomId(chatRoomId)
                    .userId(requestUserId)
                    .role(ChatRoomMember.MemberRole.MEMBER)
                    .build();

            ChatRoomMember admin = ChatRoomMember.builder()
                    .id(2L)
                    .chatRoomId(chatRoomId)
                    .userId(200L)
                    .role(ChatRoomMember.MemberRole.ADMIN)
                    .build();

            User user = User.builder()
                    .id(requestUserId)
                    .email("user@example.com")
                    .nickname("일반멤버")
                    .passwordHash("hash")
                    .build();

            User adminUser = User.builder()
                    .id(200L)
                    .email("admin@example.com")
                    .nickname("관리자")
                    .passwordHash("hash")
                    .build();

            given(chatRoomMemberValidator.getMemberOrThrow(chatRoomId, requestUserId))
                    .willReturn(member);
            given(chatRoomMemberRepository.findByChatRoomId(chatRoomId))
                    .willReturn(List.of(member, admin));
            given(userRepository.findAllById(List.of(requestUserId, 200L)))
                    .willReturn(List.of(user, adminUser));

            // when
            List<GetChatRoomMembersUseCase.MemberInfo> result =
                    getChatRoomMembersService.getChatRoomMembers(chatRoomId, requestUserId);

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).role()).isEqualTo(ChatRoomMember.MemberRole.ADMIN);
            assertThat(result.get(0).nickname()).isEqualTo("관리자");
        }
    }
}
