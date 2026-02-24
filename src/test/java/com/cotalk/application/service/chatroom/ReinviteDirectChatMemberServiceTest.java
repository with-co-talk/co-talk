package com.cotalk.application.service.chatroom;

import com.cotalk.domain.entity.ChatRoom;
import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.entity.Message;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.ChatRoomAccessDeniedException;
import com.cotalk.domain.model.Email;
import com.cotalk.domain.exception.ChatRoomNotFoundException;
import com.cotalk.domain.exception.InvalidChatRoomException;
import com.cotalk.domain.exception.UserNotFoundException;
import com.cotalk.domain.port.outbound.ChatMessageBroker;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.ChatRoomRepository;
import com.cotalk.domain.port.outbound.IdGenerator;
import com.cotalk.domain.port.outbound.MessageRepository;
import com.cotalk.domain.port.outbound.UserEventBroker;
import com.cotalk.domain.port.outbound.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.cotalk.common.fixture.ChatRoomTestFixture.createChatRoomMember;
import static com.cotalk.common.fixture.ChatRoomTestFixture.createDirectChatRoom;
import static com.cotalk.common.fixture.ChatRoomTestFixture.createGroupChatRoom;
import static com.cotalk.common.fixture.UserTestFixture.createUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * ReinviteDirectChatMemberService 단위 테스트.
 * 1:1 채팅방에서 나간 사용자를 재초대하는 기능을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class ReinviteDirectChatMemberServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private IdGenerator idGenerator;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ChatMessageBroker chatMessageBroker;

    @Mock
    private UserEventBroker userEventBroker;

    private ReinviteDirectChatMemberService service;

    @BeforeEach
    void setUp() {
        service = new ReinviteDirectChatMemberService(
                chatRoomRepository,
                chatRoomMemberRepository,
                userRepository,
                idGenerator,
                messageRepository,
                chatMessageBroker,
                userEventBroker
        );
    }

    @Test
    @DisplayName("1:1 채팅방에서 나간 사용자를 재초대 성공")
    void should_reinviteMember_when_validDirectChat() {
        // given
        Long roomId = 100L;
        Long inviterId = 1L;
        Long inviteeId = 2L;

        ChatRoom directChatRoom = createDirectChatRoom(roomId);
        User invitee = createUser(inviteeId);
        invitee = User.builder()
                .id(inviteeId)
                .nickname("재초대유저")
                .email(new Email("reinvite@test.com"))
                .build();

        given(chatRoomRepository.findById(roomId))
                .willReturn(Optional.of(directChatRoom));
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(roomId, inviterId))
                .willReturn(Optional.of(createChatRoomMember(1L, roomId, inviterId)));
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(roomId, inviteeId))
                .willReturn(Optional.empty()); // 나간 상태이므로 멤버 아님
        given(userRepository.findById(inviteeId))
                .willReturn(Optional.of(invitee));
        given(idGenerator.nextId()).willReturn(201L, 1000L);
        given(chatRoomMemberRepository.save(any(ChatRoomMember.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // 시스템 메시지 모킹
        Message systemMessage = Message.builder()
                .id(1000L)
                .chatRoomId(roomId)
                .senderId(0L)
                .content("재초대유저님이 다시 참여했습니다")
                .type(Message.MessageType.SYSTEM)
                .build();
        setCreatedAt(systemMessage, LocalDateTime.now());
        given(messageRepository.save(any(Message.class))).willReturn(systemMessage);

        // 멤버 목록 조회 (브로드캐스트용)
        given(chatRoomMemberRepository.findByChatRoomId(roomId))
                .willReturn(List.of(
                        createChatRoomMember(1L, roomId, inviterId),
                        createChatRoomMember(201L, roomId, inviteeId)
                ));

        // when
        service.reinviteMember(roomId, inviterId, inviteeId);

        // then
        ArgumentCaptor<ChatRoomMember> memberCaptor = ArgumentCaptor.forClass(ChatRoomMember.class);
        verify(chatRoomMemberRepository).save(memberCaptor.capture());

        ChatRoomMember savedMember = memberCaptor.getValue();
        assertThat(savedMember.getChatRoomId()).isEqualTo(roomId);
        assertThat(savedMember.getUserId()).isEqualTo(inviteeId);

        // 시스템 메시지 저장 검증
        verify(messageRepository).save(any(Message.class));
        // 브로드캐스트 검증
        verify(chatMessageBroker).publish(any(), any());
    }

    @Test
    @DisplayName("존재하지 않는 채팅방이면 예외 발생")
    void should_throwException_when_chatRoomNotFound() {
        // given
        Long roomId = 999L;
        Long inviterId = 1L;
        Long inviteeId = 2L;

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.reinviteMember(roomId, inviterId, inviteeId))
                .isInstanceOf(ChatRoomNotFoundException.class);
    }

    @Test
    @DisplayName("그룹 채팅방에서는 재초대 불가")
    void should_throwException_when_groupChatRoom() {
        // given
        Long roomId = 100L;
        Long inviterId = 1L;
        Long inviteeId = 2L;

        given(chatRoomRepository.findById(roomId))
                .willReturn(Optional.of(createGroupChatRoom(roomId, "그룹채팅방")));

        // when & then
        assertThatThrownBy(() -> service.reinviteMember(roomId, inviterId, inviteeId))
                .isInstanceOf(InvalidChatRoomException.class)
                .hasMessageContaining("1:1 채팅방에서만 재초대할 수 있습니다");
    }

    @Test
    @DisplayName("초대자가 채팅방 멤버가 아니면 예외 발생")
    void should_throwException_when_inviterNotMember() {
        // given
        Long roomId = 100L;
        Long inviterId = 1L;
        Long inviteeId = 2L;

        given(chatRoomRepository.findById(roomId))
                .willReturn(Optional.of(createDirectChatRoom(roomId)));
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(roomId, inviterId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.reinviteMember(roomId, inviterId, inviteeId))
                .isInstanceOf(ChatRoomAccessDeniedException.class);
    }

    @Test
    @DisplayName("재초대할 사용자가 존재하지 않으면 예외 발생")
    void should_throwException_when_inviteeNotFound() {
        // given
        Long roomId = 100L;
        Long inviterId = 1L;
        Long inviteeId = 999L;

        given(chatRoomRepository.findById(roomId))
                .willReturn(Optional.of(createDirectChatRoom(roomId)));
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(roomId, inviterId))
                .willReturn(Optional.of(createChatRoomMember(1L, roomId, inviterId)));
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(roomId, inviteeId))
                .willReturn(Optional.empty());
        given(userRepository.findById(inviteeId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.reinviteMember(roomId, inviterId, inviteeId))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("재초대할 사용자가 이미 채팅방 멤버이면 예외 발생")
    void should_throwException_when_inviteeAlreadyMember() {
        // given
        Long roomId = 100L;
        Long inviterId = 1L;
        Long inviteeId = 2L;

        given(chatRoomRepository.findById(roomId))
                .willReturn(Optional.of(createDirectChatRoom(roomId)));
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(roomId, inviterId))
                .willReturn(Optional.of(createChatRoomMember(1L, roomId, inviterId)));
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(roomId, inviteeId))
                .willReturn(Optional.of(createChatRoomMember(2L, roomId, inviteeId))); // 이미 멤버

        // when & then
        assertThatThrownBy(() -> service.reinviteMember(roomId, inviterId, inviteeId))
                .isInstanceOf(InvalidChatRoomException.class)
                .hasMessageContaining("이미 채팅방 멤버입니다");

        // 멤버 저장이 호출되지 않아야 함
        verify(chatRoomMemberRepository, never()).save(any(ChatRoomMember.class));
    }

    /**
     * 리플렉션을 사용하여 BaseEntity의 createdAt 필드를 설정한다.
     *
     * @param entity 대상 엔티티
     * @param createdAt 설정할 시간
     */
    private void setCreatedAt(Object entity, LocalDateTime createdAt) {
        try {
            Field field = entity.getClass().getSuperclass().getDeclaredField("createdAt");
            field.setAccessible(true);
            field.set(entity, createdAt);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set createdAt", e);
        }
    }
}
