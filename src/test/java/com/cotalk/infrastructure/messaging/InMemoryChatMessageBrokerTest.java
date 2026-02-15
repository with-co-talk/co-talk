package com.cotalk.infrastructure.messaging;

import com.cotalk.domain.port.outbound.ChatMessageBroker.ChatBroadcastMessage;
import com.cotalk.domain.port.outbound.ChatMessageBroker.ReactionBroadcastEvent;
import com.cotalk.infrastructure.messaging.InMemoryChatMessageBroker.WebSocketChatMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * InMemoryChatMessageBroker 단위 테스트.
 *
 * @author seunggu.lee
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InMemoryChatMessageBroker 단위 테스트")
class InMemoryChatMessageBrokerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Captor
    private ArgumentCaptor<WebSocketChatMessage> messageCaptor;

    @Captor
    private ArgumentCaptor<ReactionBroadcastEvent> reactionCaptor;

    private InMemoryChatMessageBroker messageBroker;

    @BeforeEach
    void setUp() {
        messageBroker = new InMemoryChatMessageBroker(messagingTemplate);
    }

    @Test
    @DisplayName("메시지 발행 성공")
    void should_publishMessage_when_validInput() {
        // given
        Long roomId = 1L;
        ChatBroadcastMessage message = new ChatBroadcastMessage(
                100L,           // messageId
                1L,             // senderId
                "테스트유저",   // senderNickname
                "https://example.com/avatar.jpg", // senderAvatarUrl
                roomId,         // roomId
                "안녕하세요",   // content
                "TEXT",         // type
                System.currentTimeMillis(), // createdAtMillis
                null,           // fileUrl
                null,           // fileName
                null,           // fileSize
                null,           // contentType
                null,           // thumbnailUrl
                1,              // unreadCount
                null,           // eventType
                null,           // relatedUserId
                null            // relatedUserNickname
        );

        // when
        messageBroker.publish(roomId, message);

        // then
        String expectedDestination = "/topic/chat/room/" + roomId;
        verify(messagingTemplate).convertAndSend(eq(expectedDestination), messageCaptor.capture());

        WebSocketChatMessage capturedMessage = messageCaptor.getValue();
        assertEquals(100L, capturedMessage.messageId());
        assertEquals(1L, capturedMessage.senderId());
        assertEquals(roomId, capturedMessage.roomId());
        assertEquals("안녕하세요", capturedMessage.content());
        assertEquals("TEXT", capturedMessage.type());
    }

    @Test
    @DisplayName("파일 메시지 발행 성공")
    void should_publishFileMessage_when_fileInfoProvided() {
        // given
        Long roomId = 1L;
        ChatBroadcastMessage message = new ChatBroadcastMessage(
                100L,
                1L,
                "테스트유저",   // senderNickname
                "https://example.com/avatar.jpg", // senderAvatarUrl
                roomId,
                null,
                "IMAGE",
                System.currentTimeMillis(),
                "https://storage.example.com/image.png",
                "image.png",
                1024L,
                "image/png",
                "https://storage.example.com/thumb.png",
                1,   // unreadCount
                null, // eventType
                null, // relatedUserId
                null  // relatedUserNickname
        );

        // when
        messageBroker.publish(roomId, message);

        // then
        verify(messagingTemplate).convertAndSend(eq("/topic/chat/room/" + roomId), messageCaptor.capture());

        WebSocketChatMessage capturedMessage = messageCaptor.getValue();
        assertEquals("IMAGE", capturedMessage.type());
        assertEquals("https://storage.example.com/image.png", capturedMessage.fileUrl());
        assertEquals("image.png", capturedMessage.fileName());
        assertEquals(1024L, capturedMessage.fileSize());
        assertEquals("image/png", capturedMessage.fileContentType());
        assertEquals("https://storage.example.com/thumb.png", capturedMessage.thumbnailUrl());
    }

    @Test
    @DisplayName("리액션 이벤트 발행 성공")
    void should_publishReaction_when_validInput() {
        // given
        Long roomId = 1L;
        ReactionBroadcastEvent reactionEvent = new ReactionBroadcastEvent(
                1, "event:100:1:ADDED", 1L, 100L, 1L, "👍", "ADDED", System.currentTimeMillis());

        // when
        messageBroker.publishReaction(roomId, reactionEvent);

        // then
        String expectedDestination = "/topic/chat/room/" + roomId;
        verify(messagingTemplate).convertAndSend(eq(expectedDestination), reactionCaptor.capture());

        ReactionBroadcastEvent captured = reactionCaptor.getValue();
        assertEquals(100L, captured.messageId());
        assertEquals(1L, captured.userId());
        assertEquals("ADDED", captured.eventType());
    }

    @Test
    @DisplayName("올바른 destination 경로로 메시지 발행")
    void should_sendToCorrectDestination_when_publish() {
        // given
        Long roomId = 123L;
        ChatBroadcastMessage message = new ChatBroadcastMessage(
                1L, 1L, "테스트유저", null, roomId, "test", "TEXT",
                System.currentTimeMillis(), null, null, null, null, null, 1,
                null, null, null
        );

        // when
        messageBroker.publish(roomId, message);

        // then
        verify(messagingTemplate).convertAndSend(eq("/topic/chat/room/123"), messageCaptor.capture());
    }

    @Test
    @DisplayName("올바른 destination 경로로 리액션 발행")
    void should_sendReactionToCorrectDestination_when_publishReaction() {
        // given
        Long roomId = 456L;
        ReactionBroadcastEvent reactionEvent = new ReactionBroadcastEvent(
                1, "event:200:2:ADDED", 2L, 200L, 2L, "❤️", "ADDED", System.currentTimeMillis());

        // when
        messageBroker.publishReaction(roomId, reactionEvent);

        // then
        verify(messagingTemplate).convertAndSend(eq("/topic/chat/room/456"), reactionCaptor.capture());
    }
}
