package com.cotalk.adapter.inbound.websocket;

import com.cotalk.adapter.inbound.websocket.dto.AddReactionRequest;
import com.cotalk.adapter.inbound.websocket.dto.ChatMessageRequest;
import com.cotalk.adapter.inbound.websocket.dto.FileMessageRequest;
import com.cotalk.adapter.inbound.websocket.dto.ReactionBroadcastMessage;
import com.cotalk.adapter.inbound.websocket.dto.RemoveReactionRequest;
import com.cotalk.domain.entity.BaseEntity;
import com.cotalk.domain.entity.Emoji;
import com.cotalk.domain.entity.Message;
import com.cotalk.domain.entity.MessageReaction;
import com.cotalk.domain.port.inbound.message.AddMessageReactionUseCase;
import com.cotalk.domain.port.inbound.message.RemoveMessageReactionUseCase;
import com.cotalk.domain.port.inbound.message.SendMessageUseCase;
import com.cotalk.domain.port.inbound.message.SendMessageUseCase.FileMessageCommand;
import com.cotalk.domain.port.outbound.ChatMessageBroker;
import com.cotalk.domain.port.outbound.ChatMessageBroker.ChatBroadcastMessage;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.MessageRepository;
import com.cotalk.domain.port.outbound.UserEventBroker;
import com.cotalk.domain.port.outbound.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatWebSocketController 단위 테스트")
class ChatWebSocketControllerTest {

    @Mock
    private SendMessageUseCase sendMessageUseCase;

    @Mock
    private ChatMessageBroker chatMessageBroker;

    @Mock
    private AddMessageReactionUseCase addMessageReactionUseCase;

    @Mock
    private RemoveMessageReactionUseCase removeMessageReactionUseCase;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Mock
    private UserEventBroker userEventBroker;

    @Mock
    private UserRepository userRepository;

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
                .build();
        setCreatedAt(mockMessage, LocalDateTime.now());
    }

    private void setCreatedAt(Object entity, LocalDateTime createdAt) {
        try {
            Field createdAtField = BaseEntity.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(entity, createdAt);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("메시지 전송 시 저장 후 Redis로 발행")
    void should_saveAndPublishToRedis_when_sendMessage() {
        // given
        ChatMessageRequest request = new ChatMessageRequest(1L, 100L, "테스트 메시지");

        given(sendMessageUseCase.sendMessage(anyLong(), anyLong(), anyString()))
                .willReturn(mockMessage);
        given(chatRoomMemberRepository.countUnreadMembers(anyLong(), any(), anyLong()))
                .willReturn(1);
        given(chatRoomMemberRepository.findByChatRoomId(anyLong()))
                .willReturn(Collections.emptyList());

        // when
        chatWebSocketController.sendMessage(request);

        // then
        verify(sendMessageUseCase).sendMessage(100L, 1L, "테스트 메시지");

        ArgumentCaptor<Long> roomIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<ChatBroadcastMessage> messageCaptor =
                ArgumentCaptor.forClass(ChatBroadcastMessage.class);

        verify(chatMessageBroker).publish(roomIdCaptor.capture(), messageCaptor.capture());

        assertThat(roomIdCaptor.getValue()).isEqualTo(100L);
        assertThat(messageCaptor.getValue().content()).isEqualTo("테스트 메시지");
        assertThat(messageCaptor.getValue().senderId()).isEqualTo(1L);
        assertThat(messageCaptor.getValue().messageId()).isEqualTo(1L);
        assertThat(messageCaptor.getValue().unreadCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("파일 메시지 전송 성공")
    void should_sendFileMessage_when_validRequest() {
        // given
        FileMessageRequest request = new FileMessageRequest(
                1L,
                100L,
                "https://storage.example.com/file.pdf",
                "document.pdf",
                1024L,
                "application/pdf",
                null
        );

        Message fileMessage = Message.builder()
                .id(2L)
                .senderId(1L)
                .chatRoomId(100L)
                .type(Message.MessageType.FILE)
                .fileUrl("https://storage.example.com/file.pdf")
                .fileName("document.pdf")
                .fileSize(1024L)
                .fileContentType("application/pdf")
                .build();
        setCreatedAt(fileMessage, LocalDateTime.now());

        given(sendMessageUseCase.sendFileMessage(anyLong(), anyLong(), any(FileMessageCommand.class)))
                .willReturn(fileMessage);
        given(chatRoomMemberRepository.countUnreadMembers(anyLong(), any(), anyLong()))
                .willReturn(1);
        given(chatRoomMemberRepository.findByChatRoomId(anyLong()))
                .willReturn(Collections.emptyList());

        // when
        chatWebSocketController.sendFileMessage(request);

        // then
        verify(sendMessageUseCase).sendFileMessage(eq(100L), eq(1L), any(FileMessageCommand.class));

        ArgumentCaptor<ChatBroadcastMessage> messageCaptor =
                ArgumentCaptor.forClass(ChatBroadcastMessage.class);
        verify(chatMessageBroker).publish(eq(100L), messageCaptor.capture());

        ChatBroadcastMessage captured = messageCaptor.getValue();
        assertThat(captured.fileUrl()).isEqualTo("https://storage.example.com/file.pdf");
        assertThat(captured.fileName()).isEqualTo("document.pdf");
        assertThat(captured.fileSize()).isEqualTo(1024L);
        assertThat(captured.contentType()).isEqualTo("application/pdf");
    }

    @Test
    @DisplayName("이미지 파일 메시지 전송 - 썸네일 포함")
    void should_sendImageMessage_when_thumbnailProvided() {
        // given
        FileMessageRequest request = new FileMessageRequest(
                1L,
                100L,
                "https://storage.example.com/image.jpg",
                "photo.jpg",
                2048L,
                "image/jpeg",
                "https://storage.example.com/thumb.jpg"
        );

        Message imageMessage = Message.builder()
                .id(3L)
                .senderId(1L)
                .chatRoomId(100L)
                .type(Message.MessageType.IMAGE)
                .fileUrl("https://storage.example.com/image.jpg")
                .fileName("photo.jpg")
                .fileSize(2048L)
                .fileContentType("image/jpeg")
                .thumbnailUrl("https://storage.example.com/thumb.jpg")
                .build();
        setCreatedAt(imageMessage, LocalDateTime.now());

        given(sendMessageUseCase.sendFileMessage(anyLong(), anyLong(), any(FileMessageCommand.class)))
                .willReturn(imageMessage);
        given(chatRoomMemberRepository.countUnreadMembers(anyLong(), any(), anyLong()))
                .willReturn(1);
        given(chatRoomMemberRepository.findByChatRoomId(anyLong()))
                .willReturn(Collections.emptyList());

        // when
        chatWebSocketController.sendFileMessage(request);

        // then
        ArgumentCaptor<ChatBroadcastMessage> messageCaptor =
                ArgumentCaptor.forClass(ChatBroadcastMessage.class);
        verify(chatMessageBroker).publish(eq(100L), messageCaptor.capture());

        assertThat(messageCaptor.getValue().thumbnailUrl()).isEqualTo("https://storage.example.com/thumb.jpg");
    }

    @Test
    @DisplayName("리액션 추가 성공")
    void should_addReaction_when_validRequest() {
        // given
        AddReactionRequest request = new AddReactionRequest(1L, 2L, "THUMBS_UP");

        MessageReaction reaction = MessageReaction.builder()
                .id(10L)
                .messageId(1L)
                .userId(2L)
                .emoji(Emoji.THUMBS_UP)
                .build();
        setCreatedAt(reaction, LocalDateTime.now());

        given(addMessageReactionUseCase.addReaction(anyLong(), anyLong(), anyString()))
                .willReturn(reaction);
        given(messageRepository.findById(1L))
                .willReturn(Optional.of(mockMessage));

        // when
        chatWebSocketController.addReaction(request);

        // then
        verify(addMessageReactionUseCase).addReaction(1L, 2L, "THUMBS_UP");

        ArgumentCaptor<ReactionBroadcastMessage> reactionCaptor =
                ArgumentCaptor.forClass(ReactionBroadcastMessage.class);
        verify(chatMessageBroker).publishReaction(eq(100L), reactionCaptor.capture());

        ReactionBroadcastMessage captured = reactionCaptor.getValue();
        assertThat(captured.messageId()).isEqualTo(1L);
        assertThat(captured.userId()).isEqualTo(2L);
        assertThat(captured.emoji()).isEqualTo("THUMBS_UP");
        assertThat(captured.eventType()).isEqualTo("ADDED");
    }

    @Test
    @DisplayName("리액션 제거 성공")
    void should_removeReaction_when_validRequest() {
        // given
        RemoveReactionRequest request = new RemoveReactionRequest(1L, 2L, "THUMBS_UP");

        given(messageRepository.findById(1L))
                .willReturn(Optional.of(mockMessage));

        // when
        chatWebSocketController.removeReaction(request);

        // then
        verify(removeMessageReactionUseCase).removeReaction(1L, 2L, "THUMBS_UP");

        ArgumentCaptor<ReactionBroadcastMessage> reactionCaptor =
                ArgumentCaptor.forClass(ReactionBroadcastMessage.class);
        verify(chatMessageBroker).publishReaction(eq(100L), reactionCaptor.capture());

        ReactionBroadcastMessage captured = reactionCaptor.getValue();
        assertThat(captured.eventType()).isEqualTo("REMOVED");
    }

    @Test
    @DisplayName("리액션 추가 시 메시지를 찾을 수 없으면 브로드캐스트 안 함")
    void should_notBroadcast_when_messageNotFound() {
        // given
        AddReactionRequest request = new AddReactionRequest(999L, 2L, "THUMBS_UP");

        MessageReaction reaction = MessageReaction.builder()
                .id(10L)
                .messageId(999L)
                .userId(2L)
                .emoji(Emoji.THUMBS_UP)
                .build();

        given(addMessageReactionUseCase.addReaction(anyLong(), anyLong(), anyString()))
                .willReturn(reaction);
        given(messageRepository.findById(999L))
                .willReturn(Optional.empty());

        // when
        chatWebSocketController.addReaction(request);

        // then
        verify(chatMessageBroker, never()).publishReaction(anyLong(), any());
    }
}
