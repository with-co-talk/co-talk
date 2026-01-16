package com.cotalk.adapter.inbound.websocket;

import com.cotalk.domain.entity.Message;
import com.cotalk.domain.port.inbound.SendMessageUseCase;
import com.cotalk.domain.port.outbound.ChatMessageBroker;
import com.cotalk.domain.port.outbound.ChatMessageBroker.ChatBroadcastMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatWebSocketControllerTest {

    @Mock
    private SendMessageUseCase sendMessageUseCase;

    @Mock
    private ChatMessageBroker chatMessageBroker;

    @InjectMocks
    private ChatWebSocketController chatWebSocketController;

    private Message mockMessage;

    @BeforeEach
    void setUp() {
        mockMessage = Message.builder()
                .id(1L)
                .senderId(1L)
                .chatRoomId(100L)
                .content("테스트 메시지")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("메시지 전송 시 저장 후 Redis로 발행")
    void should_saveAndPublishToRedis_when_sendMessage() {
        // given
        ChatWebSocketController.ChatMessageRequest request =
                new ChatWebSocketController.ChatMessageRequest(1L, 100L, "테스트 메시지");

        given(sendMessageUseCase.sendMessage(anyLong(), anyLong(), anyString()))
                .willReturn(mockMessage);

        // when
        chatWebSocketController.sendMessage(request);

        // then
        verify(sendMessageUseCase).sendMessage(1L, 100L, "테스트 메시지");

        ArgumentCaptor<Long> roomIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<ChatBroadcastMessage> messageCaptor =
                ArgumentCaptor.forClass(ChatBroadcastMessage.class);

        verify(chatMessageBroker).publish(roomIdCaptor.capture(), messageCaptor.capture());

        assertThat(roomIdCaptor.getValue()).isEqualTo(100L);
        assertThat(messageCaptor.getValue().content()).isEqualTo("테스트 메시지");
        assertThat(messageCaptor.getValue().senderId()).isEqualTo(1L);
        assertThat(messageCaptor.getValue().messageId()).isEqualTo(1L);
    }
}
