package com.cotalk.application.service.message;

import com.cotalk.common.fixture.ChatRoomTestFixture;
import com.cotalk.common.fixture.MessageTestFixture;
import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.entity.Message;
import com.cotalk.domain.port.outbound.ChatMessageBroker;
import com.cotalk.domain.port.outbound.MessageRepository;
import com.cotalk.domain.port.outbound.UserEventBroker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * MessageBroadcastService 단위 테스트.
 * Redis Pub/Sub를 통한 메시지 브로드캐스트 로직을 검증한다.
 *
 * @author seunggu.lee
 */
@ExtendWith(MockitoExtension.class)
class MessageBroadcastServiceTest {

    @Mock
    private ChatMessageBroker chatMessageBroker;

    @Mock
    private UserEventBroker userEventBroker;

    @Mock
    private MessageRepository messageRepository;

    @InjectMocks
    private MessageBroadcastService messageBroadcastService;

    @Test
    void should_publishMessageAndChatListUpdate_when_broadcastToRedisCalled() {
        // given
        Long chatRoomId = 10L;
        Long senderId = 1L;
        Message message = MessageTestFixture.builder()
                .id(101L)
                .chatRoomId(chatRoomId)
                .senderId(senderId)
                .content("테스트 메시지")
                .type(Message.MessageType.TEXT)
                .build();

        String senderNickname = "발신자";
        String senderAvatarUrl = "http://example.com/avatar.jpg";

        ChatRoomMember member1 = ChatRoomTestFixture.memberBuilder()
                .id(1L)
                .chatRoomId(chatRoomId)
                .userId(senderId)
                .lastReadAt(LocalDateTime.now())
                .build();

        ChatRoomMember member2 = ChatRoomTestFixture.memberBuilder()
                .id(2L)
                .chatRoomId(chatRoomId)
                .userId(2L)
                .lastReadAt(LocalDateTime.now().minusMinutes(10))
                .build();

        ChatRoomMember member3 = ChatRoomTestFixture.memberBuilder()
                .id(3L)
                .chatRoomId(chatRoomId)
                .userId(3L)
                .lastReadAt(LocalDateTime.now().minusMinutes(5))
                .build();

        List<ChatRoomMember> members = List.of(member1, member2, member3);

        given(messageRepository.countUnreadMessagesByLastReadMessageId(eq(chatRoomId), eq(1L), any()))
                .willReturn(0L);
        given(messageRepository.countUnreadMessagesByLastReadMessageId(eq(chatRoomId), eq(2L), any()))
                .willReturn(5L);
        given(messageRepository.countUnreadMessagesByLastReadMessageId(eq(chatRoomId), eq(3L), any()))
                .willReturn(2L);

        // when
        messageBroadcastService.broadcastToRedis(message, senderNickname, senderAvatarUrl, members);

        // then
        // 1. 채팅 메시지 브로드캐스트 검증
        ArgumentCaptor<ChatMessageBroker.ChatBroadcastMessage> broadcastCaptor =
                ArgumentCaptor.forClass(ChatMessageBroker.ChatBroadcastMessage.class);
        verify(chatMessageBroker, times(1)).publish(eq(chatRoomId), broadcastCaptor.capture());

        ChatMessageBroker.ChatBroadcastMessage broadcastMessage = broadcastCaptor.getValue();
        assertThat(broadcastMessage.messageId()).isEqualTo(101L);
        assertThat(broadcastMessage.senderId()).isEqualTo(senderId);
        assertThat(broadcastMessage.senderNickname()).isEqualTo(senderNickname);
        assertThat(broadcastMessage.senderAvatarUrl()).isEqualTo(senderAvatarUrl);
        assertThat(broadcastMessage.roomId()).isEqualTo(chatRoomId);
        assertThat(broadcastMessage.content()).isEqualTo("테스트 메시지");
        assertThat(broadcastMessage.type()).isEqualTo("TEXT");
        assertThat(broadcastMessage.unreadCount()).isEqualTo(2); // 멤버 3명 - 발신자 1명 = 2

        // 2. 채팅 목록 업데이트 이벤트 브로드캐스트 검증 (각 멤버에게 발송)
        ArgumentCaptor<UserEventBroker.ChatListUpdateEvent> eventCaptor =
                ArgumentCaptor.forClass(UserEventBroker.ChatListUpdateEvent.class);
        verify(userEventBroker, times(3)).publishChatListUpdate(any(Long.class), eventCaptor.capture());

        List<UserEventBroker.ChatListUpdateEvent> events = eventCaptor.getAllValues();
        assertThat(events).hasSize(3);
    }

    @Test
    void should_calculateUnreadCountForEachMember_when_broadcastToRedisCalled() {
        // given
        Long chatRoomId = 10L;
        Long senderId = 1L;
        Message message = MessageTestFixture.createMessage(101L, chatRoomId, senderId, "새 메시지");

        ChatRoomMember sender = ChatRoomTestFixture.createChatRoomMember(1L, chatRoomId, senderId);
        ChatRoomMember receiver1 = ChatRoomTestFixture.createChatRoomMember(2L, chatRoomId, 2L);
        ChatRoomMember receiver2 = ChatRoomTestFixture.createChatRoomMember(3L, chatRoomId, 3L);
        List<ChatRoomMember> members = List.of(sender, receiver1, receiver2);

        given(messageRepository.countUnreadMessagesByLastReadMessageId(eq(chatRoomId), eq(senderId), any()))
                .willReturn(0L);
        given(messageRepository.countUnreadMessagesByLastReadMessageId(eq(chatRoomId), eq(2L), any()))
                .willReturn(10L);
        given(messageRepository.countUnreadMessagesByLastReadMessageId(eq(chatRoomId), eq(3L), any()))
                .willReturn(3L);

        // when
        messageBroadcastService.broadcastToRedis(message, "발신자", null, members);

        // then
        ArgumentCaptor<UserEventBroker.ChatListUpdateEvent> eventCaptor =
                ArgumentCaptor.forClass(UserEventBroker.ChatListUpdateEvent.class);
        verify(userEventBroker, times(3)).publishChatListUpdate(any(Long.class), eventCaptor.capture());

        List<UserEventBroker.ChatListUpdateEvent> events = eventCaptor.getAllValues();

        // 각 멤버별 읽지 않은 메시지 수 검증
        UserEventBroker.ChatListUpdateEvent receiver1Event = events.stream()
                .filter(e -> e.unreadCount() == 10)
                .findFirst().orElse(null);

        UserEventBroker.ChatListUpdateEvent receiver2Event = events.stream()
                .filter(e -> e.unreadCount() == 3)
                .findFirst().orElse(null);

        assertThat(receiver1Event).isNotNull();
        assertThat(receiver1Event.unreadCount()).isEqualTo(10);

        assertThat(receiver2Event).isNotNull();
        assertThat(receiver2Event.unreadCount()).isEqualTo(3);
    }

    @Test
    void should_publishFileMessage_when_messageTypeIsImage() {
        // given
        Long chatRoomId = 10L;
        Long senderId = 1L;
        Message imageMessage = MessageTestFixture.builder()
                .id(101L)
                .chatRoomId(chatRoomId)
                .senderId(senderId)
                .content("[이미지]")
                .type(Message.MessageType.IMAGE)
                .fileUrl("http://example.com/image.jpg")
                .fileName("image.jpg")
                .fileSize(102400L)
                .fileContentType("image/jpeg")
                .thumbnailUrl("http://example.com/thumbnail.jpg")
                .build();

        List<ChatRoomMember> members = List.of(
                ChatRoomTestFixture.createChatRoomMember(1L, chatRoomId, senderId)
        );

        given(messageRepository.countUnreadMessagesByLastReadMessageId(any(), any(), any())).willReturn(0L);

        // when
        messageBroadcastService.broadcastToRedis(imageMessage, "발신자", null, members);

        // then
        ArgumentCaptor<ChatMessageBroker.ChatBroadcastMessage> broadcastCaptor =
                ArgumentCaptor.forClass(ChatMessageBroker.ChatBroadcastMessage.class);
        verify(chatMessageBroker, times(1)).publish(eq(chatRoomId), broadcastCaptor.capture());

        ChatMessageBroker.ChatBroadcastMessage broadcastMessage = broadcastCaptor.getValue();
        assertThat(broadcastMessage.type()).isEqualTo("IMAGE");
        assertThat(broadcastMessage.fileUrl()).isEqualTo("http://example.com/image.jpg");
        assertThat(broadcastMessage.fileName()).isEqualTo("image.jpg");
        assertThat(broadcastMessage.fileSize()).isEqualTo(102400L);
        assertThat(broadcastMessage.fileContentType()).isEqualTo("image/jpeg");
        assertThat(broadcastMessage.thumbnailUrl()).isEqualTo("http://example.com/thumbnail.jpg");
    }
}
