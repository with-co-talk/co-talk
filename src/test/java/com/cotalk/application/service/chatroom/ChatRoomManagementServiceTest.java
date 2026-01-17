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

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatRoomManagementServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @InjectMocks
    private ChatRoomManagementService chatRoomManagementService;

    private Long chatRoomId;
    private Long adminUserId;
    private Long normalUserId;
    private ChatRoom groupChatRoom;
    private ChatRoomMember adminMember;
    private ChatRoomMember normalMember;

    @BeforeEach
    void setUp() {
        chatRoomId = 1L;
        adminUserId = 100L;
        normalUserId = 200L;

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

        normalMember = ChatRoomMember.builder()
                .id(2L)
                .chatRoomId(chatRoomId)
                .userId(normalUserId)
                .role(ChatRoomMember.MemberRole.MEMBER)
                .build();
    }

    @Nested
    @DisplayName("채팅방 이름 변경")
    class UpdateChatRoomName {

        @Test
        @DisplayName("관리자가 채팅방 이름을 성공적으로 변경한다")
        void should_updateName_when_adminUser() {
            // given
            String newName = "새로운 그룹 이름";
            given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.of(groupChatRoom));
            given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, adminUserId))
                    .willReturn(Optional.of(adminMember));
            given(chatRoomRepository.save(any(ChatRoom.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            ChatRoom result = chatRoomManagementService.updateChatRoomName(chatRoomId, adminUserId, newName);

            // then
            assertThat(result.getName()).isEqualTo(newName);
            verify(chatRoomRepository).save(any(ChatRoom.class));
        }

        @Test
        @DisplayName("일반 멤버가 이름 변경 시도 시 실패한다")
        void should_throwException_when_normalUserTriesToUpdate() {
            // given
            given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.of(groupChatRoom));
            given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, normalUserId))
                    .willReturn(Optional.of(normalMember));

            // when & then
            assertThatThrownBy(() ->
                    chatRoomManagementService.updateChatRoomName(chatRoomId, normalUserId, "새 이름"))
                    .isInstanceOf(ChatRoomAccessDeniedException.class);
        }

        @Test
        @DisplayName("1:1 채팅방은 이름 변경이 불가능하다")
        void should_throwException_when_directChat() {
            // given
            ChatRoom directChat = ChatRoom.builder()
                    .id(2L)
                    .type(ChatRoom.ChatRoomType.DIRECT)
                    .build();
            given(chatRoomRepository.findById(2L)).willReturn(Optional.of(directChat));

            // when & then
            assertThatThrownBy(() ->
                    chatRoomManagementService.updateChatRoomName(2L, adminUserId, "새 이름"))
                    .isInstanceOf(InvalidChatRoomException.class)
                    .hasMessageContaining("1:1 채팅방");
        }
    }

    @Nested
    @DisplayName("채팅방 공지사항")
    class ChatRoomAnnouncement {

        @Test
        @DisplayName("관리자가 공지사항을 성공적으로 설정한다")
        void should_setAnnouncement_when_adminUser() {
            // given
            String announcement = "중요 공지사항입니다!";
            given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.of(groupChatRoom));
            given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, adminUserId))
                    .willReturn(Optional.of(adminMember));
            given(chatRoomRepository.save(any(ChatRoom.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            ChatRoom result = chatRoomManagementService.setAnnouncement(chatRoomId, adminUserId, announcement);

            // then
            assertThat(result.getAnnouncement()).isEqualTo(announcement);
            verify(chatRoomRepository).save(any(ChatRoom.class));
        }

        @Test
        @DisplayName("일반 멤버가 공지사항 설정 시도 시 실패한다")
        void should_throwException_when_normalUserTriesToSetAnnouncement() {
            // given
            given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.of(groupChatRoom));
            given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, normalUserId))
                    .willReturn(Optional.of(normalMember));

            // when & then
            assertThatThrownBy(() ->
                    chatRoomManagementService.setAnnouncement(chatRoomId, normalUserId, "공지"))
                    .isInstanceOf(ChatRoomAccessDeniedException.class);
        }

        @Test
        @DisplayName("관리자가 공지사항을 삭제한다")
        void should_clearAnnouncement_when_adminUser() {
            // given
            groupChatRoom.setAnnouncement("기존 공지");
            given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.of(groupChatRoom));
            given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, adminUserId))
                    .willReturn(Optional.of(adminMember));
            given(chatRoomRepository.save(any(ChatRoom.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            ChatRoom result = chatRoomManagementService.clearAnnouncement(chatRoomId, adminUserId);

            // then
            assertThat(result.getAnnouncement()).isNull();
        }
    }

    @Nested
    @DisplayName("관리자 권한 관리")
    class AdminManagement {

        @Test
        @DisplayName("관리자가 다른 멤버를 관리자로 임명한다")
        void should_promoteToAdmin_when_adminUser() {
            // given
            given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.of(groupChatRoom));
            given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, adminUserId))
                    .willReturn(Optional.of(adminMember));
            given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, normalUserId))
                    .willReturn(Optional.of(normalMember));
            given(chatRoomMemberRepository.save(any(ChatRoomMember.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            ChatRoomMember result = chatRoomManagementService.promoteToAdmin(chatRoomId, adminUserId, normalUserId);

            // then
            assertThat(result.getRole()).isEqualTo(ChatRoomMember.MemberRole.ADMIN);
        }

        @Test
        @DisplayName("일반 멤버가 관리자 임명 시도 시 실패한다")
        void should_throwException_when_normalUserTriesToPromote() {
            // given
            Long anotherUserId = 300L;
            ChatRoomMember anotherMember = ChatRoomMember.builder()
                    .id(3L)
                    .chatRoomId(chatRoomId)
                    .userId(anotherUserId)
                    .role(ChatRoomMember.MemberRole.MEMBER)
                    .build();

            given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.of(groupChatRoom));
            given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, normalUserId))
                    .willReturn(Optional.of(normalMember));

            // when & then
            assertThatThrownBy(() ->
                    chatRoomManagementService.promoteToAdmin(chatRoomId, normalUserId, anotherUserId))
                    .isInstanceOf(ChatRoomAccessDeniedException.class);
        }

        @Test
        @DisplayName("관리자가 다른 관리자의 권한을 해제한다")
        void should_demoteFromAdmin_when_adminUser() {
            // given
            ChatRoomMember anotherAdmin = ChatRoomMember.builder()
                    .id(3L)
                    .chatRoomId(chatRoomId)
                    .userId(300L)
                    .role(ChatRoomMember.MemberRole.ADMIN)
                    .build();

            given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.of(groupChatRoom));
            given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, adminUserId))
                    .willReturn(Optional.of(adminMember));
            given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, 300L))
                    .willReturn(Optional.of(anotherAdmin));
            given(chatRoomMemberRepository.save(any(ChatRoomMember.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            ChatRoomMember result = chatRoomManagementService.demoteFromAdmin(chatRoomId, adminUserId, 300L);

            // then
            assertThat(result.getRole()).isEqualTo(ChatRoomMember.MemberRole.MEMBER);
        }
    }
}
