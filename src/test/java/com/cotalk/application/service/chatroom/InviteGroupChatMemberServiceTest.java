package com.cotalk.application.service.chatroom;

import com.cotalk.domain.entity.ChatRoom;
import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.exception.ChatRoomAccessDeniedException;
import com.cotalk.domain.exception.ChatRoomNotFoundException;
import com.cotalk.domain.exception.InvalidChatRoomException;
import com.cotalk.domain.exception.UserNotFoundException;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.ChatRoomRepository;
import com.cotalk.domain.port.outbound.UserRepository;
import com.cotalk.infrastructure.id.SnowflakeIdGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.cotalk.common.fixture.ChatRoomTestFixture.createChatRoomMember;
import static com.cotalk.common.fixture.ChatRoomTestFixture.createGroupChatRoom;
import static com.cotalk.common.fixture.UserTestFixture.createUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class InviteGroupChatMemberServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SnowflakeIdGenerator idGenerator;

    @InjectMocks
    private InviteGroupChatMemberService inviteGroupChatMemberService;

    @Test
    @DisplayName("그룹 채팅방에 멤버 초대 성공")
    void should_inviteMember_when_validInput() {
        // given
        Long roomId = 100L;
        Long inviterId = 1L;
        List<Long> inviteeIds = List.of(5L, 6L);

        given(chatRoomRepository.findById(roomId))
                .willReturn(Optional.of(createGroupChatRoom(roomId, "테스트방")));
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(roomId, inviterId))
                .willReturn(Optional.of(createChatRoomMember(1L, roomId, inviterId)));
        given(userRepository.findAllById(inviteeIds))
                .willReturn(List.of(createUser(5L), createUser(6L)));
        given(chatRoomMemberRepository.findByChatRoomId(roomId))
                .willReturn(List.of(createChatRoomMember(1L, roomId, inviterId)));
        given(idGenerator.nextId()).willReturn(201L, 202L);
        given(chatRoomMemberRepository.saveAll(any()))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        inviteGroupChatMemberService.inviteMembers(roomId, inviterId, inviteeIds);

        // then
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ChatRoomMember>> membersCaptor = ArgumentCaptor.forClass(List.class);
        verify(chatRoomMemberRepository).saveAll(membersCaptor.capture());
        assertThat(membersCaptor.getValue()).hasSize(2);
    }

    @Test
    @DisplayName("존재하지 않는 채팅방이면 예외 발생")
    void should_throwException_when_chatRoomNotFound() {
        // given
        Long roomId = 999L;
        Long inviterId = 1L;
        List<Long> inviteeIds = List.of(5L);

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> inviteGroupChatMemberService.inviteMembers(roomId, inviterId, inviteeIds))
                .isInstanceOf(ChatRoomNotFoundException.class);
    }

    @Test
    @DisplayName("1:1 채팅방에는 멤버 초대 불가")
    void should_throwException_when_directChatRoom() {
        // given
        Long roomId = 100L;
        Long inviterId = 1L;
        List<Long> inviteeIds = List.of(5L);

        ChatRoom directRoom = ChatRoom.builder()
                .id(roomId)
                .type(ChatRoom.ChatRoomType.DIRECT)
                .build();

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(directRoom));

        // when & then
        assertThatThrownBy(() -> inviteGroupChatMemberService.inviteMembers(roomId, inviterId, inviteeIds))
                .isInstanceOf(InvalidChatRoomException.class)
                .hasMessageContaining("멤버를 초대할 수 없습니다");
    }

    @Test
    @DisplayName("채팅방 멤버가 아니면 초대 불가")
    void should_throwException_when_inviterNotMember() {
        // given
        Long roomId = 100L;
        Long inviterId = 1L;
        List<Long> inviteeIds = List.of(5L);

        given(chatRoomRepository.findById(roomId))
                .willReturn(Optional.of(createGroupChatRoom(roomId, "테스트방")));
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(roomId, inviterId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> inviteGroupChatMemberService.inviteMembers(roomId, inviterId, inviteeIds))
                .isInstanceOf(ChatRoomAccessDeniedException.class);
    }

    @Test
    @DisplayName("존재하지 않는 사용자를 초대하면 예외 발생")
    void should_throwException_when_inviteeNotFound() {
        // given
        Long roomId = 100L;
        Long inviterId = 1L;
        List<Long> inviteeIds = List.of(999L);

        given(chatRoomRepository.findById(roomId))
                .willReturn(Optional.of(createGroupChatRoom(roomId, "테스트방")));
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(roomId, inviterId))
                .willReturn(Optional.of(createChatRoomMember(1L, roomId, inviterId)));
        given(userRepository.findAllById(inviteeIds))
                .willReturn(List.of()); // 사용자가 없음
        given(chatRoomMemberRepository.findByChatRoomId(roomId))
                .willReturn(List.of(createChatRoomMember(1L, roomId, inviterId)));

        // when & then
        assertThatThrownBy(() -> inviteGroupChatMemberService.inviteMembers(roomId, inviterId, inviteeIds))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("이미 채팅방 멤버인 사용자는 초대 무시")
    void should_skipInvitation_when_alreadyMember() {
        // given
        Long roomId = 100L;
        Long inviterId = 1L;
        Long existingMemberId = 5L;
        Long newMemberId = 6L;
        List<Long> inviteeIds = List.of(existingMemberId, newMemberId);

        given(chatRoomRepository.findById(roomId))
                .willReturn(Optional.of(createGroupChatRoom(roomId, "테스트방")));
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(roomId, inviterId))
                .willReturn(Optional.of(createChatRoomMember(1L, roomId, inviterId)));
        given(userRepository.findAllById(inviteeIds))
                .willReturn(List.of(createUser(existingMemberId), createUser(newMemberId)));
        given(chatRoomMemberRepository.findByChatRoomId(roomId))
                .willReturn(List.of(
                        createChatRoomMember(1L, roomId, inviterId),
                        createChatRoomMember(2L, roomId, existingMemberId) // 이미 멤버
                ));
        given(idGenerator.nextId()).willReturn(201L);
        given(chatRoomMemberRepository.saveAll(any()))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        inviteGroupChatMemberService.inviteMembers(roomId, inviterId, inviteeIds);

        // then - 기존 멤버(5L)는 건너뛰고 새 멤버(6L)만 저장
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ChatRoomMember>> membersCaptor = ArgumentCaptor.forClass(List.class);
        verify(chatRoomMemberRepository).saveAll(membersCaptor.capture());
        assertThat(membersCaptor.getValue()).hasSize(1);
    }

    @Test
    @DisplayName("모든 초대 대상이 이미 멤버인 경우 저장하지 않음")
    void should_notSave_when_allInviteesAlreadyMembers() {
        // given
        Long roomId = 100L;
        Long inviterId = 1L;
        Long existingMemberId = 5L;
        List<Long> inviteeIds = List.of(existingMemberId);

        given(chatRoomRepository.findById(roomId))
                .willReturn(Optional.of(createGroupChatRoom(roomId, "테스트방")));
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(roomId, inviterId))
                .willReturn(Optional.of(createChatRoomMember(1L, roomId, inviterId)));
        given(userRepository.findAllById(inviteeIds))
                .willReturn(List.of(createUser(existingMemberId)));
        given(chatRoomMemberRepository.findByChatRoomId(roomId))
                .willReturn(List.of(
                        createChatRoomMember(1L, roomId, inviterId),
                        createChatRoomMember(2L, roomId, existingMemberId) // 이미 멤버
                ));

        // when
        inviteGroupChatMemberService.inviteMembers(roomId, inviterId, inviteeIds);

        // then - 모든 초대 대상이 이미 멤버이므로 saveAll 호출되지 않음
        verify(chatRoomMemberRepository, org.mockito.Mockito.never()).saveAll(any());
    }
}
