package com.cotalk.application.service.chatroom;

import com.cotalk.domain.entity.ChatRoom;
import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.InvalidGroupChatException;
import com.cotalk.domain.exception.UserNotFoundException;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.ChatRoomRepository;
import com.cotalk.domain.validator.UserValidator;
import com.cotalk.infrastructure.id.SnowflakeIdGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.cotalk.common.fixture.UserTestFixture.createUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CreateGroupChatRoomServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Mock
    private UserValidator userValidator;

    @Mock
    private SnowflakeIdGenerator idGenerator;

    @InjectMocks
    private CreateGroupChatRoomService createGroupChatRoomService;

    @Test
    @DisplayName("그룹 채팅방 생성 성공")
    void should_createGroupChatRoom_when_validInput() {
        // given
        Long creatorId = 1L;
        String roomName = "개발팀 채팅방";
        List<Long> memberIds = List.of(2L, 3L, 4L);
        Long chatRoomId = 100L;

        doNothing().when(userValidator).validateUsersExist(any());
        given(idGenerator.nextId()).willReturn(chatRoomId, 101L, 102L, 103L, 104L);
        given(chatRoomRepository.save(any(ChatRoom.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(chatRoomMemberRepository.saveAll(any()))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        Long result = createGroupChatRoomService.createGroupChatRoom(creatorId, roomName, memberIds);

        // then
        assertThat(result).isEqualTo(chatRoomId);

        ArgumentCaptor<ChatRoom> chatRoomCaptor = ArgumentCaptor.forClass(ChatRoom.class);
        verify(chatRoomRepository).save(chatRoomCaptor.capture());
        assertThat(chatRoomCaptor.getValue().getType()).isEqualTo(ChatRoom.ChatRoomType.GROUP);
        assertThat(chatRoomCaptor.getValue().getName()).isEqualTo(roomName);

        // 생성자 + 멤버 3명 = 총 4명이 saveAll로 한 번에 저장됨
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ChatRoomMember>> membersCaptor = ArgumentCaptor.forClass(List.class);
        verify(chatRoomMemberRepository).saveAll(membersCaptor.capture());
        assertThat(membersCaptor.getValue()).hasSize(4);
    }

    @Test
    @DisplayName("그룹 채팅방 이름이 없으면 예외 발생")
    void should_throwException_when_roomNameIsEmpty() {
        // given
        Long creatorId = 1L;
        String roomName = "";
        List<Long> memberIds = List.of(2L, 3L);

        // when & then
        assertThatThrownBy(() -> createGroupChatRoomService.createGroupChatRoom(creatorId, roomName, memberIds))
                .isInstanceOf(InvalidGroupChatException.class)
                .hasMessageContaining("그룹 채팅방 이름은 필수입니다");
    }

    @Test
    @DisplayName("그룹 채팅방 이름이 null이면 예외 발생")
    void should_throwException_when_roomNameIsNull() {
        // given
        Long creatorId = 1L;
        List<Long> memberIds = List.of(2L, 3L);

        // when & then
        assertThatThrownBy(() -> createGroupChatRoomService.createGroupChatRoom(creatorId, null, memberIds))
                .isInstanceOf(InvalidGroupChatException.class)
                .hasMessageContaining("그룹 채팅방 이름은 필수입니다");
    }

    @Test
    @DisplayName("그룹 채팅방 멤버가 2명 미만이면 예외 발생")
    void should_throwException_when_memberCountIsLessThanTwo() {
        // given
        Long creatorId = 1L;
        String roomName = "테스트방";
        List<Long> memberIds = List.of(2L); // 생성자 포함 2명 - 최소 3명 필요

        // when & then
        assertThatThrownBy(() -> createGroupChatRoomService.createGroupChatRoom(creatorId, roomName, memberIds))
                .isInstanceOf(InvalidGroupChatException.class)
                .hasMessageContaining("그룹 채팅방은 최소 3명 이상이어야 합니다");
    }

    @Test
    @DisplayName("존재하지 않는 사용자가 포함되면 예외 발생")
    void should_throwException_when_memberNotFound() {
        // given
        Long creatorId = 1L;
        String roomName = "테스트방";
        List<Long> memberIds = List.of(2L, 999L);

        willThrow(new UserNotFoundException(999L))
                .given(userValidator).validateUsersExist(any());

        // when & then
        assertThatThrownBy(() -> createGroupChatRoomService.createGroupChatRoom(creatorId, roomName, memberIds))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("생성자가 존재하지 않으면 예외 발생")
    void should_throwException_when_creatorNotFound() {
        // given
        Long creatorId = 999L;
        String roomName = "테스트방";
        List<Long> memberIds = List.of(2L, 3L);

        willThrow(new UserNotFoundException(creatorId))
                .given(userValidator).validateUsersExist(any());

        // when & then
        assertThatThrownBy(() -> createGroupChatRoomService.createGroupChatRoom(creatorId, roomName, memberIds))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("그룹 채팅방 이름이 50자를 초과하면 예외 발생")
    void should_throwException_when_roomNameTooLong() {
        // given
        Long creatorId = 1L;
        String roomName = "a".repeat(51);
        List<Long> memberIds = List.of(2L, 3L);

        // when & then
        assertThatThrownBy(() -> createGroupChatRoomService.createGroupChatRoom(creatorId, roomName, memberIds))
                .isInstanceOf(InvalidGroupChatException.class)
                .hasMessageContaining("그룹 채팅방 이름은 50자를 초과할 수 없습니다");
    }
}
