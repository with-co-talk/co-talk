package com.cotalk.infrastructure.messaging;

import com.cotalk.infrastructure.messaging.RedisChatMessageSubscriber.WebSocketChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * RedisChatMessageSubscriber 단위 테스트.
 *
 * @author seunggu.lee
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RedisChatMessageSubscriber")
class RedisChatMessageSubscriberTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Captor
    private ArgumentCaptor<WebSocketChatMessage> messageCaptor;

    private ObjectMapper objectMapper;
    private RedisChatMessageSubscriber subscriber;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        subscriber = new RedisChatMessageSubscriber(messagingTemplate, objectMapper);
    }

    @Nested
    @DisplayName("메시지 수신 시")
    class OnMessage {

        @Test
        @DisplayName("Redis 메시지를 수신하여 WebSocket으로 브로드캐스트한다")
        void should_broadcastToWebSocket_when_messageReceived() {
            // given
            String jsonMessage = """
                {
                    "messageId": 100,
                    "senderId": 1,
                    "roomId": 10,
                    "content": "안녕하세요",
                    "type": "TEXT",
                    "createdAtMillis": 1704067200000,
                    "fileUrl": null,
                    "fileName": null,
                    "fileSize": null,
                    "contentType": null,
                    "thumbnailUrl": null
                }
                """;
            Message redisMessage = createRedisMessage(jsonMessage);

            // when
            subscriber.onMessage(redisMessage, null);

            // then
            verify(messagingTemplate).convertAndSend(eq("/topic/chat/room/10"), messageCaptor.capture());
            WebSocketChatMessage captured = messageCaptor.getValue();
            assertThat(captured.messageId()).isEqualTo(100L);
            assertThat(captured.senderId()).isEqualTo(1L);
            assertThat(captured.roomId()).isEqualTo(10L);
            assertThat(captured.content()).isEqualTo("안녕하세요");
            assertThat(captured.type()).isEqualTo("TEXT");
        }

        @Test
        @DisplayName("파일 메시지를 수신하여 WebSocket으로 브로드캐스트한다")
        void should_broadcastFileMessage_when_fileMessageReceived() {
            // given
            String jsonMessage = """
                {
                    "messageId": 200,
                    "senderId": 2,
                    "roomId": 20,
                    "content": null,
                    "type": "IMAGE",
                    "createdAtMillis": 1704067200000,
                    "fileUrl": "https://storage.example.com/image.png",
                    "fileName": "image.png",
                    "fileSize": 1024,
                    "contentType": "image/png",
                    "thumbnailUrl": "https://storage.example.com/thumb.png"
                }
                """;
            Message redisMessage = createRedisMessage(jsonMessage, 20L);

            // when
            subscriber.onMessage(redisMessage, null);

            // then
            verify(messagingTemplate).convertAndSend(eq("/topic/chat/room/20"), messageCaptor.capture());
            WebSocketChatMessage captured = messageCaptor.getValue();
            assertThat(captured.type()).isEqualTo("IMAGE");
            assertThat(captured.fileUrl()).isEqualTo("https://storage.example.com/image.png");
            assertThat(captured.fileName()).isEqualTo("image.png");
            assertThat(captured.fileSize()).isEqualTo(1024L);
            assertThat(captured.contentType()).isEqualTo("image/png");
            assertThat(captured.thumbnailUrl()).isEqualTo("https://storage.example.com/thumb.png");
        }

        @Test
        @DisplayName("잘못된 JSON 메시지 수신 시 예외를 삼킨다")
        void should_handleGracefully_when_invalidJsonReceived() {
            // given
            String invalidJson = "{ invalid json }";
            Message redisMessage = createRedisMessage(invalidJson, 10L);

            // when
            subscriber.onMessage(redisMessage, null);

            // then
            verify(messagingTemplate, never()).convertAndSend(anyString(), any(WebSocketChatMessage.class));
        }

        @Test
        @DisplayName("올바른 destination으로 메시지를 전송한다")
        void should_sendToCorrectDestination() {
            // given
            String jsonMessage = """
                {
                    "messageId": 300,
                    "senderId": 3,
                    "roomId": 999,
                    "content": "test",
                    "type": "TEXT",
                    "createdAtMillis": 1704067200000
                }
                """;
            Message redisMessage = createRedisMessage(jsonMessage);

            // when
            subscriber.onMessage(redisMessage, null);

            // then
            verify(messagingTemplate).convertAndSend(eq("/topic/chat/room/999"), any(WebSocketChatMessage.class));
        }

        @Test
        @DisplayName("createdAtMillis를 LocalDateTime으로 변환한다")
        void should_convertCreatedAtMillis_toLocalDateTime() {
            // given
            String jsonMessage = """
                {
                    "messageId": 400,
                    "senderId": 4,
                    "roomId": 40,
                    "content": "test",
                    "type": "TEXT",
                    "createdAtMillis": 1704067200000
                }
                """;
            Message redisMessage = createRedisMessage(jsonMessage, 40L);

            // when
            subscriber.onMessage(redisMessage, null);

            // then
            verify(messagingTemplate).convertAndSend(eq("/topic/chat/room/40"), messageCaptor.capture());
            WebSocketChatMessage captured = messageCaptor.getValue();
            assertThat(captured.createdAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("WebSocketChatMessage record 테스트")
    class WebSocketChatMessageTest {

        @Test
        @DisplayName("WebSocketChatMessage record가 올바르게 생성된다")
        void should_createRecord_when_validArguments() {
            // when
            WebSocketChatMessage message = new WebSocketChatMessage(
                    1,
                    "message:1",
                    1L, 2L, "테스트유저", 3L, "content", "TEXT",
                    java.time.LocalDateTime.now(),
                    "fileUrl", "fileName", 100L, "text/plain", "thumbUrl", 1,
                    null, null, null  // eventType, relatedUserId, relatedUserNickname
            );

            // then
            assertThat(message.schemaVersion()).isEqualTo(1);
            assertThat(message.eventId()).isEqualTo("message:1");
            assertThat(message.messageId()).isEqualTo(1L);
            assertThat(message.senderId()).isEqualTo(2L);
            assertThat(message.senderNickname()).isEqualTo("테스트유저");
            assertThat(message.roomId()).isEqualTo(3L);
            assertThat(message.content()).isEqualTo("content");
            assertThat(message.type()).isEqualTo("TEXT");
            assertThat(message.fileUrl()).isEqualTo("fileUrl");
            assertThat(message.fileName()).isEqualTo("fileName");
            assertThat(message.fileSize()).isEqualTo(100L);
            assertThat(message.contentType()).isEqualTo("text/plain");
            assertThat(message.thumbnailUrl()).isEqualTo("thumbUrl");
            assertThat(message.unreadCount()).isEqualTo(1);
        }
    }

    private Message createRedisMessage(String body) {
        return createRedisMessage(body, 10L);
    }

    private Message createRedisMessage(String body, Long roomId) {
        return new Message() {
            @Override
            public byte[] getBody() {
                return body.getBytes(StandardCharsets.UTF_8);
            }

            @Override
            public byte[] getChannel() {
                return ("chat:room:" + roomId).getBytes(StandardCharsets.UTF_8);
            }
        };
    }
}
