package com.cotalk.application.service.chatroom;

import com.cotalk.domain.entity.ChatRoom;
import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.exception.ChatRoomAccessDeniedException;
import com.cotalk.domain.exception.InvalidChatRoomException;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.ChatRoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * 채팅방 멤버 강제 퇴장 서비스 테스트.
 *
 * @author seunggu.lee
 */
@ExtendWith(MockitoExtension.class)
class KickChatRoomMemberServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @InjectMocks
    private KickChatRoomMemberService kickChatRoomMemberService;

    private Long chatRoomId;
    private Long adminUserId;
    private Long targetUserId;
    private ChatRoom groupChatRoom;
    private ChatRoomMember adminMember;
    private ChatRoomMember targetMember;

    @BeforeEach
    void setUp() {
        chatRoomId = 1L;
        adminUserId = 100L;
        targetUserId = 200L;

        groupChatRoom = ChatRoom.builder()
                .id(chatRoomId)
                .name("테스트 그룹")
                .type(ChatRoom.ChatRoomType.GROUP)
                .build();

        adminMember = ChatRoomMember.builder()
                .id(1L)
                .chatRoomId(chatRoomId)
                .userId(adminUserId)
                .role(ChatRoomMember.MemberRole.ADMIN)
                .build();

        targetMember = ChatRoomMember.builder()
                .id(2L)
                .chatRoomId(chatRoomId)
                .userId(targetUserId)
                .role(ChatRoomMember.MemberRole.MEMBER)
                .build();
    }

    @Nested
    @DisplayName("멤버 강제 퇴장")
    class KickMember {

        @Test
        @DisplayName("관리자가 일반 멤버를 성공적으로 강제 퇴장시킨다")
        void should_kickMember_when_adminKicksMember() {
            // given
            given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.of(groupChatRoom));
            given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, adminUserId))
                    .willReturn(Optional.of(adminMember));
            given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, targetUserId))
                    .willReturn(Optional.of(targetMember));

            // when
            kickChatRoomMemberService.kickMember(chatRoomId, adminUserId, targetUserId);

            // then
            verify(chatRoomMemberRepository).delete(targetMember);
        }

        @Test
        @DisplayName("일반 멤버가 강제 퇴장 시도 시 실패한다")
        void should_throwException_when_normalUserTriesToKick() {
            // given
            ChatRoomMember normalMember = ChatRoomMember.builder()
                    .id(3L)
                    .chatRoomId(chatRoomId)
                    .userId(300L)
                    .role(ChatRoomMember.MemberRole.MEMBER)
                    .build();

            given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.of(groupChatRoom));
            given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, 300L))
                    .willReturn(Optional.of(normalMember));

            // when & then
            assertThatThrownBy(() ->
                    kickChatRoomMemberService.kickMember(chatRoomId, 300L, targetUserId))
                    .isInstanceOf(ChatRoomAccessDeniedException.class)
                    .hasMessageContaining("관리자");
        }

        @Test
        @DisplayName("1:1 채팅방에서는 강제 퇴장이 불가능하다")
        void should_throwException_when_directChat() {
            // given
            ChatRoom directChat = ChatRoom.builder()
                    .id(2L)
                    .type(ChatRoom.ChatRoomType.DIRECT)
                    .build();
            given(chatRoomRepository.findById(2L)).willReturn(Optional.of(directChat));

            // when & then
            assertThatThrownBy(() ->
                    kickChatRoomMemberService.kickMember(2L, adminUserId, targetUserId))
                    .isInstanceOf(InvalidChatRoomException.class)
                    .hasMessageContaining("1:1 채팅방");
        }

        @Test
        @DisplayName("자기 자신을 강제 퇴장시킬 수 없다")
        void should_throwException_when_kickSelf() {
            // given
            given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.of(groupChatRoom));

            // when & then
            assertThatThrownBy(() ->
                    kickChatRoomMemberService.kickMember(chatRoomId, adminUserId, adminUserId))
                    .isInstanceOf(InvalidChatRoomException.class)
                    .hasMessageContaining("자기 자신");
        }

        @Test
        @DisplayName("채팅방에 없는 멤버를 강제 퇴장시키려 하면 실패한다")
        void should_throwException_when_targetNotMember() {
            // given
            given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.of(groupChatRoom));
            given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, adminUserId))
                    .willReturn(Optional.of(adminMember));
            given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, targetUserId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    kickChatRoomMemberService.kickMember(chatRoomId, adminUserId, targetUserId))
                    .isInstanceOf(ChatRoomAccessDeniedException.class);
        }
    }
}
