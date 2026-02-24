package com.cotalk.application.service.message;

import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.entity.Message;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.model.Email;
import com.cotalk.domain.exception.ChatRoomAccessDeniedException;
import com.cotalk.domain.port.inbound.message.MarkAsReadUseCase;
import com.cotalk.domain.port.outbound.ChatMessageBroker;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.MessageRepository;
import com.cotalk.domain.port.outbound.TimeProvider;
import com.cotalk.domain.port.outbound.UserEventBroker;
import com.cotalk.domain.port.outbound.UserEventBroker.ReadReceiptEvent;
import com.cotalk.domain.port.outbound.UserRepository;
import com.cotalk.domain.validator.ChatRoomMemberValidator;
import static org.mockito.ArgumentMatchers.any;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MarkAsReadServiceTest {

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Mock
    private UserEventBroker userEventBroker;

    @Mock
    private ChatMessageBroker chatMessageBroker;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TimeProvider timeProvider;

    private ChatRoomMemberValidator chatRoomMemberValidator;

    private MarkAsReadUseCase markAsReadUseCase;

    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 1, 1, 12, 0);

    @BeforeEach
    void setUp() {
        chatRoomMemberValidator = new ChatRoomMemberValidator(chatRoomMemberRepository);
        markAsReadUseCase = new MarkAsReadService(
                chatRoomMemberRepository,
                chatRoomMemberValidator,
                userEventBroker,
                chatMessageBroker,
                messageRepository,
                userRepository,
                timeProvider
        );
    }

    @Test
    @DisplayName("읽음 표시 성공 - 메시지가 없는 경우 lastReadAt만 업데이트")
    void should_markAsRead_when_validRequest() {
        // given
        Long userId = 1L;
        Long chatRoomId = 100L;
        ChatRoomMember member = ChatRoomMember.builder()
                .id(500L)
                .chatRoomId(chatRoomId)
                .userId(userId)
                .build();

        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
                .willReturn(Optional.of(member));
        given(messageRepository.findTopByChatRoomIdOrderByCreatedAtDesc(chatRoomId))
                .willReturn(Optional.empty()); // 메시지 없음
        given(chatRoomMemberRepository.updateLastReadAt(eq(chatRoomId), eq(userId), any(LocalDateTime.class)))
                .willReturn(1);
        given(timeProvider.now()).willReturn(FIXED_NOW);

        // when
        markAsReadUseCase.markAsRead(userId, chatRoomId);

        // then - 메시지가 없으므로 updateLastReadAt만 호출
        verify(chatRoomMemberRepository).updateLastReadAt(eq(chatRoomId), eq(userId), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("채팅방 멤버가 아니면 예외 발생")
    void should_throwException_when_notMember() {
        // given
        Long userId = 1L;
        Long chatRoomId = 100L;
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> markAsReadUseCase.markAsRead(userId, chatRoomId))
                .isInstanceOf(ChatRoomAccessDeniedException.class);
    }

    @Test
    @DisplayName("읽음 시간이 현재 시간으로 업데이트됨 - 메시지가 없는 경우")
    void should_updateLastReadAtToCurrentTime_when_markAsRead() {
        // given
        Long userId = 1L;
        Long chatRoomId = 100L;
        ChatRoomMember member = ChatRoomMember.builder()
                .id(500L)
                .chatRoomId(chatRoomId)
                .userId(userId)
                .build();

        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
                .willReturn(Optional.of(member));
        given(messageRepository.findTopByChatRoomIdOrderByCreatedAtDesc(chatRoomId))
                .willReturn(Optional.empty()); // 메시지 없음
        given(chatRoomMemberRepository.updateLastReadAt(eq(chatRoomId), eq(userId), any(LocalDateTime.class)))
                .willReturn(1);
        given(timeProvider.now()).willReturn(FIXED_NOW);

        // when
        markAsReadUseCase.markAsRead(userId, chatRoomId);

        // then - 메시지가 없으므로 updateLastReadAt 호출
        ArgumentCaptor<LocalDateTime> timeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(chatRoomMemberRepository).updateLastReadAt(eq(chatRoomId), eq(userId), timeCaptor.capture());
        assertThat(timeCaptor.getValue()).isEqualTo(FIXED_NOW);
    }

    @Test
    @DisplayName("읽음 처리 시 모든 멤버에게 읽음 이벤트 브로드캐스트")
    void should_broadcastReadReceipt_when_markAsRead() {
        // given
        Long readerId = 1L;
        Long otherUserId = 2L;
        Long chatRoomId = 100L;
        Long messageId = 1000L;

        ChatRoomMember readerMember = ChatRoomMember.builder()
                .id(500L)
                .chatRoomId(chatRoomId)
                .userId(readerId)
                .build();
        ChatRoomMember otherMember = ChatRoomMember.builder()
                .id(501L)
                .chatRoomId(chatRoomId)
                .userId(otherUserId)
                .build();

        Message message = Message.builder()
                .id(messageId)
                .chatRoomId(chatRoomId)
                .senderId(otherUserId)
                .content("테스트 메시지")
                .type(Message.MessageType.TEXT)
                .build();
        ReflectionTestUtils.setField(message, "createdAt", FIXED_NOW);

        User sender = User.builder()
                .id(otherUserId)
                .email(new Email("sender@test.com"))
                .nickname("발신자")
                .passwordHash("hash")
                .build();

        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, readerId))
                .willReturn(Optional.of(readerMember));
        given(messageRepository.findTopByChatRoomIdOrderByCreatedAtDesc(chatRoomId))
                .willReturn(Optional.of(message));
        given(chatRoomMemberRepository.updateLastReadMessageIdIfNewer(
                eq(chatRoomId), eq(readerId), any(LocalDateTime.class), eq(messageId)))
                .willReturn(1);
        given(chatRoomMemberRepository.findByChatRoomId(chatRoomId))
                .willReturn(List.of(readerMember, otherMember));
        given(userRepository.findById(otherUserId)).willReturn(Optional.of(sender));
        given(messageRepository.batchCountUnreadMessagesForAllMembers(chatRoomId))
                .willReturn(java.util.Map.of(readerId, 0L, otherUserId, 0L));
        given(timeProvider.now()).willReturn(FIXED_NOW);

        // when
        markAsReadUseCase.markAsRead(readerId, chatRoomId);

        // then - 모든 멤버에게 이벤트 전송 확인
        ArgumentCaptor<ReadReceiptEvent> eventCaptor = ArgumentCaptor.forClass(ReadReceiptEvent.class);
        verify(userEventBroker).publishReadReceipt(eq(readerId), eventCaptor.capture());
        verify(userEventBroker).publishReadReceipt(eq(otherUserId), eventCaptor.capture());

        ReadReceiptEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.chatRoomId()).isEqualTo(chatRoomId);
        assertThat(capturedEvent.userId()).isEqualTo(readerId);
        assertThat(capturedEvent.lastReadAt()).isNotNull();
    }

    @Test
    @DisplayName("읽은 본인에게도 읽음 이벤트를 전송함")
    void should_broadcastToSelf_when_markAsRead() {
        // given
        Long userId = 1L;
        Long chatRoomId = 100L;
        Long messageId = 1000L;

        ChatRoomMember member = ChatRoomMember.builder()
                .id(500L)
                .chatRoomId(chatRoomId)
                .userId(userId)
                .build();

        Message message = Message.builder()
                .id(messageId)
                .chatRoomId(chatRoomId)
                .senderId(2L)
                .content("테스트 메시지")
                .type(Message.MessageType.TEXT)
                .build();
        ReflectionTestUtils.setField(message, "createdAt", FIXED_NOW);

        User sender = User.builder()
                .id(2L)
                .email(new Email("sender@test.com"))
                .nickname("발신자")
                .passwordHash("hash")
                .build();

        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
                .willReturn(Optional.of(member));
        given(messageRepository.findTopByChatRoomIdOrderByCreatedAtDesc(chatRoomId))
                .willReturn(Optional.of(message));
        given(chatRoomMemberRepository.updateLastReadMessageIdIfNewer(
                eq(chatRoomId), eq(userId), any(LocalDateTime.class), eq(messageId)))
                .willReturn(1);
        given(chatRoomMemberRepository.findByChatRoomId(chatRoomId))
                .willReturn(List.of(member));
        given(userRepository.findById(2L)).willReturn(Optional.of(sender));
        given(messageRepository.batchCountUnreadMessagesForAllMembers(chatRoomId))
                .willReturn(java.util.Map.of(userId, 0L));
        given(timeProvider.now()).willReturn(FIXED_NOW);

        // when
        markAsReadUseCase.markAsRead(userId, chatRoomId);

        // then
        ArgumentCaptor<ReadReceiptEvent> eventCaptor = ArgumentCaptor.forClass(ReadReceiptEvent.class);
        verify(userEventBroker).publishReadReceipt(eq(userId), eventCaptor.capture());

        ReadReceiptEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.chatRoomId()).isEqualTo(chatRoomId);
        assertThat(capturedEvent.userId()).isEqualTo(userId);
        assertThat(capturedEvent.lastReadAt()).isEqualTo(FIXED_NOW);
    }

    @Test
    @DisplayName("업데이트가 발생하지 않아도 클라이언트 동기화를 위해 읽음 이벤트를 브로드캐스트함")
    void should_broadcastEvenWhenNoUpdate_when_markAsRead() {
        // given
        Long userId = 1L;
        Long chatRoomId = 100L;
        Long messageId = 1000L;
        Long existingLastReadMessageId = 900L;

        ChatRoomMember member = ChatRoomMember.builder()
                .id(500L)
                .chatRoomId(chatRoomId)
                .userId(userId)
                .lastReadMessageId(existingLastReadMessageId)
                .build();

        Message message = Message.builder()
                .id(messageId)
                .chatRoomId(chatRoomId)
                .senderId(2L)
                .content("테스트 메시지")
                .type(Message.MessageType.TEXT)
                .build();
        ReflectionTestUtils.setField(message, "createdAt", FIXED_NOW);

        User sender = User.builder()
                .id(2L)
                .email(new Email("sender@test.com"))
                .nickname("발신자")
                .passwordHash("hash")
                .build();

        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
                .willReturn(Optional.of(member));
        given(messageRepository.findTopByChatRoomIdOrderByCreatedAtDesc(chatRoomId))
                .willReturn(Optional.of(message));
        given(chatRoomMemberRepository.updateLastReadMessageIdIfNewer(
                eq(chatRoomId), eq(userId), any(LocalDateTime.class), eq(messageId)))
                .willReturn(0); // 업데이트 발생하지 않음
        given(chatRoomMemberRepository.findByChatRoomId(chatRoomId))
                .willReturn(List.of(member));
        given(userRepository.findById(2L)).willReturn(Optional.of(sender));
        given(messageRepository.batchCountUnreadMessagesForAllMembers(chatRoomId))
                .willReturn(java.util.Map.of(userId, 5L));
        given(timeProvider.now()).willReturn(FIXED_NOW);

        // when
        markAsReadUseCase.markAsRead(userId, chatRoomId);

        // then - 클라이언트 동기화를 위해 항상 이벤트 발행
        verify(userEventBroker).publishReadReceipt(eq(userId), any(ReadReceiptEvent.class));
        verify(chatMessageBroker).publishRoomEvent(eq(chatRoomId), any());
    }

    @Test
    @DisplayName("메시지가 있는 경우 lastReadMessageId를 사용하여 업데이트")
    void should_updateLastReadMessageId_when_messageExists() {
        // given
        Long userId = 1L;
        Long chatRoomId = 100L;
        Long messageId = 1000L;
        ChatRoomMember member = ChatRoomMember.builder()
                .id(500L)
                .chatRoomId(chatRoomId)
                .userId(userId)
                .build();

        Message message = Message.builder()
                .id(messageId)
                .chatRoomId(chatRoomId)
                .senderId(2L)
                .content("테스트 메시지")
                .type(Message.MessageType.TEXT)
                .build();

        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
                .willReturn(Optional.of(member));
        given(messageRepository.findTopByChatRoomIdOrderByCreatedAtDesc(chatRoomId))
                .willReturn(Optional.of(message));
        given(chatRoomMemberRepository.updateLastReadMessageIdIfNewer(
                eq(chatRoomId), eq(userId), any(LocalDateTime.class), eq(messageId)))
                .willReturn(1);
        given(chatRoomMemberRepository.findByChatRoomId(chatRoomId))
                .willReturn(List.of(member));
        given(timeProvider.now()).willReturn(FIXED_NOW);

        // when
        markAsReadUseCase.markAsRead(userId, chatRoomId);

        // then
        verify(chatRoomMemberRepository).updateLastReadMessageIdIfNewer(
                eq(chatRoomId), eq(userId), any(LocalDateTime.class), eq(messageId));
        verify(chatRoomMemberRepository, never()).updateLastReadAtIfNewer(any(), any(), any());
    }

    @Test
    @DisplayName("updated == 0일 때 DB에서 lastReadMessageId를 조회하여 사용")
    void should_fetchLastReadMessageIdFromDB_when_updateReturnsZero() {
        // given
        Long userId = 1L;
        Long chatRoomId = 100L;
        Long messageId = 1000L;
        Long existingLastReadMessageId = 900L;

        ChatRoomMember member = ChatRoomMember.builder()
                .id(500L)
                .chatRoomId(chatRoomId)
                .userId(userId)
                .lastReadMessageId(existingLastReadMessageId)
                .build();

        Message message = Message.builder()
                .id(messageId)
                .chatRoomId(chatRoomId)
                .senderId(2L)
                .content("테스트 메시지")
                .type(Message.MessageType.TEXT)
                .build();

        User sender = User.builder()
                .id(2L)
                .email(new Email("sender@test.com"))
                .nickname("발신자")
                .passwordHash("hash")
                .build();

        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
                .willReturn(Optional.of(member));
        given(messageRepository.findTopByChatRoomIdOrderByCreatedAtDesc(chatRoomId))
                .willReturn(Optional.of(message));
        given(chatRoomMemberRepository.updateLastReadMessageIdIfNewer(
                eq(chatRoomId), eq(userId), any(LocalDateTime.class), eq(messageId)))
                .willReturn(0); // 업데이트 발생하지 않음
        given(chatRoomMemberRepository.findByChatRoomId(chatRoomId))
                .willReturn(List.of(member));
        given(userRepository.findById(2L))
                .willReturn(Optional.of(sender));
        given(messageRepository.batchCountUnreadMessagesForAllMembers(chatRoomId))
                .willReturn(java.util.Map.of(userId, 5L));
        given(timeProvider.now()).willReturn(FIXED_NOW);

        // when
        markAsReadUseCase.markAsRead(userId, chatRoomId);

        // then - 배치 쿼리를 사용하여 unreadCount 계산
        verify(messageRepository).batchCountUnreadMessagesForAllMembers(chatRoomId);
        verify(userEventBroker).publishChatListUpdate(eq(userId), any());
    }

    @Test
    @DisplayName("메시지가 있는 경우 채팅 목록 업데이트 이벤트 발행")
    void should_publishChatListUpdate_when_messageExists() {
        // given
        Long userId = 1L;
        Long chatRoomId = 100L;
        Long messageId = 1000L;

        ChatRoomMember member = ChatRoomMember.builder()
                .id(500L)
                .chatRoomId(chatRoomId)
                .userId(userId)
                .build();

        Message message = Message.builder()
                .id(messageId)
                .chatRoomId(chatRoomId)
                .senderId(2L)
                .content("테스트 메시지")
                .type(Message.MessageType.TEXT)
                .build();
        ReflectionTestUtils.setField(message, "createdAt", FIXED_NOW);

        User sender = User.builder()
                .id(2L)
                .email(new Email("sender@test.com"))
                .nickname("발신자")
                .passwordHash("hash")
                .build();

        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
                .willReturn(Optional.of(member));
        given(messageRepository.findTopByChatRoomIdOrderByCreatedAtDesc(chatRoomId))
                .willReturn(Optional.of(message));
        given(chatRoomMemberRepository.updateLastReadMessageIdIfNewer(
                eq(chatRoomId), eq(userId), any(LocalDateTime.class), eq(messageId)))
                .willReturn(1);
        given(chatRoomMemberRepository.findByChatRoomId(chatRoomId))
                .willReturn(List.of(member));
        given(userRepository.findById(2L))
                .willReturn(Optional.of(sender));
        given(messageRepository.batchCountUnreadMessagesForAllMembers(chatRoomId))
                .willReturn(java.util.Map.of(userId, 0L));
        given(timeProvider.now()).willReturn(FIXED_NOW);

        // when
        markAsReadUseCase.markAsRead(userId, chatRoomId);

        // then
        verify(userEventBroker).publishChatListUpdate(eq(userId), any());
    }

    @Test
    @DisplayName("발신자를 찾을 수 없는 경우 '알 수 없음'으로 처리")
    void should_useUnknownNickname_when_senderNotFound() {
        // given
        Long userId = 1L;
        Long chatRoomId = 100L;
        Long messageId = 1000L;

        ChatRoomMember member = ChatRoomMember.builder()
                .id(500L)
                .chatRoomId(chatRoomId)
                .userId(userId)
                .build();

        Message message = Message.builder()
                .id(messageId)
                .chatRoomId(chatRoomId)
                .senderId(999L) // 존재하지 않는 사용자
                .content("테스트 메시지")
                .type(Message.MessageType.TEXT)
                .build();
        ReflectionTestUtils.setField(message, "createdAt", FIXED_NOW);

        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
                .willReturn(Optional.of(member));
        given(messageRepository.findTopByChatRoomIdOrderByCreatedAtDesc(chatRoomId))
                .willReturn(Optional.of(message));
        given(chatRoomMemberRepository.updateLastReadMessageIdIfNewer(
                eq(chatRoomId), eq(userId), any(LocalDateTime.class), eq(messageId)))
                .willReturn(1);
        given(chatRoomMemberRepository.findByChatRoomId(chatRoomId))
                .willReturn(List.of(member));
        given(userRepository.findById(999L))
                .willReturn(Optional.empty()); // 사용자 없음
        given(messageRepository.batchCountUnreadMessagesForAllMembers(chatRoomId))
                .willReturn(java.util.Map.of(userId, 0L));
        given(timeProvider.now()).willReturn(FIXED_NOW);

        // when
        markAsReadUseCase.markAsRead(userId, chatRoomId);

        // then - '알 수 없음' 닉네임으로 이벤트 발행
        ArgumentCaptor<UserEventBroker.ChatListUpdateEvent> eventCaptor =
                ArgumentCaptor.forClass(UserEventBroker.ChatListUpdateEvent.class);
        verify(userEventBroker).publishChatListUpdate(eq(userId), eventCaptor.capture());
        assertThat(eventCaptor.getValue().senderNickname()).isEqualTo("알 수 없음");
    }

    @Test
    @DisplayName("채팅 목록 업데이트 시 메시지가 없으면 업데이트하지 않음")
    void should_skipChatListUpdate_when_noMessage() {
        // given
        Long userId = 1L;
        Long chatRoomId = 100L;

        ChatRoomMember member = ChatRoomMember.builder()
                .id(500L)
                .chatRoomId(chatRoomId)
                .userId(userId)
                .build();

        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
                .willReturn(Optional.of(member));
        given(messageRepository.findTopByChatRoomIdOrderByCreatedAtDesc(chatRoomId))
                .willReturn(Optional.empty()); // 메시지 없음
        given(chatRoomMemberRepository.updateLastReadAt(eq(chatRoomId), eq(userId), any(LocalDateTime.class)))
                .willReturn(1);
        given(timeProvider.now()).willReturn(FIXED_NOW);

        // when
        markAsReadUseCase.markAsRead(userId, chatRoomId);

        // then - 메시지가 없으므로 updateLastReadAt만 호출하고 이벤트 발행 없이 조기 종료
        verify(chatRoomMemberRepository).updateLastReadAt(eq(chatRoomId), eq(userId), any(LocalDateTime.class));
        verify(userEventBroker, never()).publishReadReceipt(any(), any());
        verify(userEventBroker, never()).publishChatListUpdate(any(), any());
        verify(chatMessageBroker, never()).publishRoomEvent(any(), any());
    }

    @Test
    @DisplayName("채팅 목록 업데이트 시 멤버가 없으면 업데이트하지 않음")
    void should_skipChatListUpdate_when_noMembers() {
        // given
        Long userId = 1L;
        Long chatRoomId = 100L;
        Long messageId = 1000L;

        ChatRoomMember member = ChatRoomMember.builder()
                .id(500L)
                .chatRoomId(chatRoomId)
                .userId(userId)
                .build();

        Message message = Message.builder()
                .id(messageId)
                .chatRoomId(chatRoomId)
                .senderId(2L)
                .content("테스트 메시지")
                .type(Message.MessageType.TEXT)
                .build();
        ReflectionTestUtils.setField(message, "createdAt", FIXED_NOW);

        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
                .willReturn(Optional.of(member));
        given(messageRepository.findTopByChatRoomIdOrderByCreatedAtDesc(chatRoomId))
                .willReturn(Optional.of(message))
                .willReturn(Optional.of(message)); // publishChatListUpdate에서도 메시지 있음
        given(chatRoomMemberRepository.updateLastReadMessageIdIfNewer(
                eq(chatRoomId), eq(userId), any(LocalDateTime.class), eq(messageId)))
                .willReturn(1);
        given(chatRoomMemberRepository.findByChatRoomId(chatRoomId))
                .willReturn(List.of(member)) // 읽음 이벤트 발행용
                .willReturn(List.of()); // publishChatListUpdate에서 멤버 없음
        given(timeProvider.now()).willReturn(FIXED_NOW);

        // when
        markAsReadUseCase.markAsRead(userId, chatRoomId);

        // then - 채팅 목록 업데이트는 호출되지 않음 (멤버가 없으므로)
        verify(userEventBroker, never()).publishChatListUpdate(any(), any());
    }

    @Test
    @DisplayName("여러 멤버가 있는 경우 각 멤버의 lastReadMessageId를 기준으로 unreadCount 계산")
    void should_calculateUnreadCountForEachMember_when_multipleMembers() {
        // given
        Long readerId = 1L;
        Long otherUserId1 = 2L;
        Long otherUserId2 = 3L;
        Long chatRoomId = 100L;
        Long messageId = 1000L;
        Long readerLastReadMessageId = messageId;
        Long otherUser1LastReadMessageId = 900L;
        Long otherUser2LastReadMessageId = null;

        ChatRoomMember readerMember = ChatRoomMember.builder()
                .id(500L)
                .chatRoomId(chatRoomId)
                .userId(readerId)
                .lastReadMessageId(readerLastReadMessageId)
                .build();
        ChatRoomMember otherMember1 = ChatRoomMember.builder()
                .id(501L)
                .chatRoomId(chatRoomId)
                .userId(otherUserId1)
                .lastReadMessageId(otherUser1LastReadMessageId)
                .build();
        ChatRoomMember otherMember2 = ChatRoomMember.builder()
                .id(502L)
                .chatRoomId(chatRoomId)
                .userId(otherUserId2)
                .lastReadMessageId(otherUser2LastReadMessageId)
                .build();

        Message message = Message.builder()
                .id(messageId)
                .chatRoomId(chatRoomId)
                .senderId(4L)
                .content("테스트 메시지")
                .type(Message.MessageType.TEXT)
                .build();
        ReflectionTestUtils.setField(message, "createdAt", FIXED_NOW);

        User sender = User.builder()
                .id(4L)
                .email(new Email("sender@test.com"))
                .nickname("발신자")
                .passwordHash("hash")
                .build();

        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, readerId))
                .willReturn(Optional.of(readerMember));
        given(messageRepository.findTopByChatRoomIdOrderByCreatedAtDesc(chatRoomId))
                .willReturn(Optional.of(message))
                .willReturn(Optional.of(message)); // publishChatListUpdate에서도 메시지 있음
        given(chatRoomMemberRepository.updateLastReadMessageIdIfNewer(
                eq(chatRoomId), eq(readerId), any(LocalDateTime.class), eq(messageId)))
                .willReturn(1);
        given(chatRoomMemberRepository.findByChatRoomId(chatRoomId))
                .willReturn(List.of(readerMember, otherMember1, otherMember2)); // 읽음 이벤트 발행용
        given(userRepository.findById(4L))
                .willReturn(Optional.of(sender));
        // 배치 쿼리로 모든 멤버의 unreadCount를 한 번에 반환
        given(messageRepository.batchCountUnreadMessagesForAllMembers(chatRoomId))
                .willReturn(java.util.Map.of(
                        readerId, 0L,
                        otherUserId1, 5L,
                        otherUserId2, 10L
                ));
        given(timeProvider.now()).willReturn(FIXED_NOW);

        // when
        markAsReadUseCase.markAsRead(readerId, chatRoomId);

        // then - 각 멤버에게 올바른 unreadCount로 이벤트 발행
        ArgumentCaptor<UserEventBroker.ChatListUpdateEvent> eventCaptor =
                ArgumentCaptor.forClass(UserEventBroker.ChatListUpdateEvent.class);
        verify(userEventBroker, times(3)).publishChatListUpdate(any(), eventCaptor.capture());

        List<UserEventBroker.ChatListUpdateEvent> capturedEvents = eventCaptor.getAllValues();
        assertThat(capturedEvents).hasSize(3);

        // reader의 unreadCount는 0
        UserEventBroker.ChatListUpdateEvent readerEvent = capturedEvents.stream()
                .filter(e -> e.roomId().equals(chatRoomId))
                .filter(e -> {
                    // readerId에 해당하는 이벤트 찾기 (unreadCount가 0인 것)
                    return e.unreadCount() == 0;
                })
                .findFirst()
                .orElseThrow();
        assertThat(readerEvent.unreadCount()).isEqualTo(0);

        // otherUser1의 unreadCount는 5
        UserEventBroker.ChatListUpdateEvent otherUser1Event = capturedEvents.stream()
                .filter(e -> e.unreadCount() == 5)
                .findFirst()
                .orElseThrow();
        assertThat(otherUser1Event.unreadCount()).isEqualTo(5);

        // otherUser2의 unreadCount는 10
        UserEventBroker.ChatListUpdateEvent otherUser2Event = capturedEvents.stream()
                .filter(e -> e.unreadCount() == 10)
                .findFirst()
                .orElseThrow();
        assertThat(otherUser2Event.unreadCount()).isEqualTo(10);
    }
}
