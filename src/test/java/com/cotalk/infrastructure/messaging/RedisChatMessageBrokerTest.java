package com.cotalk.infrastructure.messaging;

import com.cotalk.domain.port.outbound.ChatMessageBroker.ChatBroadcastMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RedisChatMessageBrokerTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    private ObjectMapper objectMapper;
    private RedisChatMessageBroker broker;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        broker = new RedisChatMessageBroker(redisTemplate, objectMapper);
    }

    @Test
    @DisplayName("메시지 발행 시 Redis 채널로 전송")
    void should_publishToRedisChannel_when_publish() {
        // given
        Long roomId = 100L;
        ChatBroadcastMessage message = new ChatBroadcastMessage(
                1L,           // messageId
                10L,          // senderId
                roomId,       // roomId
                "Hello!",     // content
                "TEXT",       // type
                System.currentTimeMillis(),
                null, null, null, null, null  // file fields
        );

        // when
        broker.publish(roomId, message);

        // then
        ArgumentCaptor<String> channelCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

        verify(redisTemplate).convertAndSend(channelCaptor.capture(), messageCaptor.capture());

        assertThat(channelCaptor.getValue()).isEqualTo("chat:room:100");
        assertThat(messageCaptor.getValue()).contains("Hello!");
        assertThat(messageCaptor.getValue()).contains("\"messageId\":1");
    }

    @Test
    @DisplayName("파일 메시지 발행")
    void should_publishFileMessage_when_fileMessagePublish() {
        // given
        Long roomId = 100L;
        ChatBroadcastMessage message = new ChatBroadcastMessage(
                1L,
                10L,
                roomId,
                "photo.jpg",
                "IMAGE",
                System.currentTimeMillis(),
                "https://storage.example.com/photo.jpg",
                "photo.jpg",
                102400L,
                "image/jpeg",
                "https://storage.example.com/thumb.jpg"
        );

        // when
        broker.publish(roomId, message);

        // then
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate).convertAndSend(eq("chat:room:100"), messageCaptor.capture());

        assertThat(messageCaptor.getValue()).contains("IMAGE");
        assertThat(messageCaptor.getValue()).contains("photo.jpg");
        assertThat(messageCaptor.getValue()).contains("https://storage.example.com/photo.jpg");
    }
}
