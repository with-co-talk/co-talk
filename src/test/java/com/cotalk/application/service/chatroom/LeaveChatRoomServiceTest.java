package com.cotalk.application.service.chatroom;

import com.cotalk.domain.entity.ChatRoom;
import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.entity.Message;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.ChatRoomAccessDeniedException;
import com.cotalk.domain.port.outbound.ChatMessageBroker;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.ChatRoomRepository;
import com.cotalk.domain.port.outbound.IdGenerator;
import com.cotalk.domain.port.outbound.MessageRepository;
import com.cotalk.domain.port.outbound.UserEventBroker;
import com.cotalk.domain.port.outbound.UserRepository;
import com.cotalk.infrastructure.lock.DistributedLockExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LeaveChatRoomServiceTest {

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private DistributedLockExecutor lockExecutor;

    @Mock
    private IdGenerator idGenerator;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatMessageBroker chatMessageBroker;

    @Mock
    private UserEventBroker userEventBroker;

    private LeaveChatRoomService service;

    @BeforeEach
    void setUp() {
        service = new LeaveChatRoomService(
                chatRoomMemberRepository,
                chatRoomRepository,
                lockExecutor,
                idGenerator,
                messageRepository,
                userRepository,
                chatMessageBroker,
                userEventBroker
        );

        // 분산락 모킹: 락 획득 후 바로 실행
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(1);
            runnable.run();
            return null;
        }).when(lockExecutor).executeWithLock(anyString(), any(Runnable.class));
    }

    @Test
    @DisplayName("채팅방 나가기 성공")
    void should_leaveChatRoom_when_validMember() {
        // given
        Long chatRoomId = 100L;
        Long userId = 1L;

        ChatRoomMember member = ChatRoomMember.builder()
                .id(500L)
                .chatRoomId(chatRoomId)
                .userId(userId)
                .build();

        User leavingUser = User.builder()
                .id(userId)
                .nickname("테스트유저")
                .build();

        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
                .willReturn(Optional.of(member));
        given(userRepository.findById(userId))
                .willReturn(Optional.of(leavingUser));

        // 다른 멤버가 존재하는 경우
        ChatRoomMember otherMember = ChatRoomMember.builder()
                .id(501L)
                .chatRoomId(chatRoomId)
                .userId(2L)
                .build();

        given(chatRoomMemberRepository.findByChatRoomId(chatRoomId))
                .willReturn(List.of(otherMember));

        // 시스템 메시지 생성을 위한 mock
        given(idGenerator.nextId()).willReturn(1000L);

        Message systemMessage = Message.builder()
                .id(1000L)
                .chatRoomId(chatRoomId)
                .senderId(0L)
                .content("테스트유저님이 나갔습니다")
                .type(Message.MessageType.SYSTEM)
                .build();
        setCreatedAt(systemMessage, LocalDateTime.now());
        given(messageRepository.save(any(Message.class))).willReturn(systemMessage);

        // when
        service.leaveChatRoom(chatRoomId, userId);

        // then
        verify(chatRoomMemberRepository).delete(member);
        verify(messageRepository).save(any(Message.class));
        verify(chatMessageBroker).publish(any(), any());
    }

    @Test
    @DisplayName("채팅방 멤버가 아니면 예외 발생")
    void should_throwException_when_notMember() {
        // given
        Long chatRoomId = 100L;
        Long userId = 1L;

        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.leaveChatRoom(chatRoomId, userId))
                .isInstanceOf(ChatRoomAccessDeniedException.class);
    }

    @Test
    @DisplayName("마지막 멤버가 나가면 채팅방도 삭제")
    void should_deleteChatRoom_when_lastMemberLeaves() {
        // given
        Long chatRoomId = 100L;
        Long userId = 1L;

        ChatRoomMember member = ChatRoomMember.builder()
                .id(500L)
                .chatRoomId(chatRoomId)
                .userId(userId)
                .build();

        ChatRoom chatRoom = ChatRoom.builder()
                .id(chatRoomId)
                .type(ChatRoom.ChatRoomType.DIRECT)
                .build();

        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
                .willReturn(Optional.of(member));
        // delete 후에는 빈 리스트 반환 (이미 삭제되었으므로)
        given(chatRoomMemberRepository.findByChatRoomId(chatRoomId))
                .willReturn(List.of()); // 삭제 후 빈 리스트
        given(chatRoomRepository.findById(chatRoomId))
                .willReturn(Optional.of(chatRoom));

        // when
        service.leaveChatRoom(chatRoomId, userId);

        // then
        verify(chatRoomMemberRepository).delete(member);
        verify(chatRoomRepository).delete(chatRoom);
    }

    @Test
    @DisplayName("마지막 멤버가 나갔지만 채팅방이 이미 삭제된 경우 처리")
    void should_handleGracefully_when_chatRoomAlreadyDeleted() {
        // given
        Long chatRoomId = 100L;
        Long userId = 1L;

        ChatRoomMember member = ChatRoomMember.builder()
                .id(500L)
                .chatRoomId(chatRoomId)
                .userId(userId)
                .build();

        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
                .willReturn(Optional.of(member));
        given(chatRoomMemberRepository.findByChatRoomId(chatRoomId))
                .willReturn(List.of()); // 삭제 후 빈 리스트
        given(chatRoomRepository.findById(chatRoomId))
                .willReturn(Optional.empty()); // 채팅방이 이미 삭제됨

        // when
        service.leaveChatRoom(chatRoomId, userId);

        // then
        verify(chatRoomMemberRepository).delete(member);
        // 채팅방이 없으므로 delete 호출되지 않음
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
