package com.cotalk.adapter.inbound.websocket;

import com.cotalk.adapter.inbound.websocket.dto.AddReactionRequest;
import com.cotalk.adapter.inbound.websocket.dto.ChatMessageRequest;
import com.cotalk.adapter.inbound.websocket.dto.FileMessageRequest;
import com.cotalk.adapter.inbound.websocket.dto.PresencePingRequest;
import com.cotalk.adapter.inbound.websocket.dto.RemoveReactionRequest;
import com.cotalk.adapter.inbound.websocket.dto.TypingStatusRequest;
import com.cotalk.domain.entity.BaseEntity;
import com.cotalk.domain.entity.Emoji;
import com.cotalk.domain.entity.Message;
import com.cotalk.domain.entity.MessageReaction;
import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.exception.ChatRoomAccessDeniedException;
import com.cotalk.domain.port.inbound.chat.BroadcastChatMessageUseCase;
import com.cotalk.domain.port.inbound.chat.BroadcastReactionEventUseCase;
import com.cotalk.domain.port.inbound.chat.PublishChatListUpdateUseCase;
import com.cotalk.domain.port.inbound.chat.PublishTypingStatusUseCase;
import com.cotalk.domain.port.inbound.chat.UpdatePresenceStatusUseCase;
import com.cotalk.domain.port.inbound.message.AddMessageReactionUseCase;
import com.cotalk.domain.port.inbound.message.AddMessageReactionUseCase.ReactionResult;
import com.cotalk.domain.port.inbound.message.RemoveMessageReactionUseCase;
import com.cotalk.domain.port.inbound.message.SendMessageUseCase;
import com.cotalk.domain.port.inbound.message.SendMessageUseCase.FileMessageCommand;
import com.cotalk.domain.validator.ChatRoomMemberValidator;
import com.cotalk.infrastructure.metrics.CustomMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

import java.lang.reflect.Field;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatWebSocketController 단위 테스트")
class ChatWebSocketControllerTest {

    @Mock
    private SendMessageUseCase sendMessageUseCase;

    @Mock
    private AddMessageReactionUseCase addMessageReactionUseCase;

    @Mock
    private RemoveMessageReactionUseCase removeMessageReactionUseCase;

    @Mock
    private BroadcastChatMessageUseCase broadcastChatMessageUseCase;

    @Mock
    private BroadcastReactionEventUseCase broadcastReactionEventUseCase;

    @Mock
    private PublishTypingStatusUseCase publishTypingStatusUseCase;

    @Mock
    private UpdatePresenceStatusUseCase updatePresenceStatusUseCase;

    @Mock
    private PublishChatListUpdateUseCase publishChatListUpdateUseCase;

    @Mock
    private ChatRoomMemberValidator chatRoomMemberValidator;

    @Mock
    private CustomMetrics customMetrics;

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

    private StompHeaderAccessor createMockHeaderAccessor(Long userId) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.create(org.springframework.messaging.simp.stomp.StompCommand.SEND);
        headerAccessor.setUser(new Principal() {
            @Override
            public String getName() {
                return userId.toString();
            }
        });
        return headerAccessor;
    }

    @Test
    @DisplayName("메시지 전송 시 저장 후 브로드캐스트 유스케이스 호출")
    void should_saveAndBroadcast_when_sendMessage() {
        // given
        ChatMessageRequest request = new ChatMessageRequest(100L, "테스트 메시지");

        ChatRoomMember member1 = ChatRoomMember.builder()
                .id(1L)
                .chatRoomId(100L)
                .userId(1L)
                .build();
        ChatRoomMember member2 = ChatRoomMember.builder()
                .id(2L)
                .chatRoomId(100L)
                .userId(2L)
                .build();
        List<ChatRoomMember> members = List.of(member1, member2);

        SendMessageUseCase.SendResult sendResult = new SendMessageUseCase.SendResult(
                mockMessage, "발신자", null, members);

        given(sendMessageUseCase.sendMessageWithContext(anyLong(), anyLong(), anyString()))
                .willReturn(sendResult);

        // when
        chatWebSocketController.sendMessage(request, createMockHeaderAccessor(1L));

        // then
        verify(sendMessageUseCase).sendMessageWithContext(100L, 1L, "테스트 메시지");
        verify(broadcastChatMessageUseCase).broadcastMessage(mockMessage, "발신자", null, members);
        verify(publishChatListUpdateUseCase).publishChatListUpdate(mockMessage, members, "발신자");
    }

    @Test
    @DisplayName("파일 메시지 전송 성공")
    void should_sendFileMessage_when_validRequest() {
        // given
        FileMessageRequest request = new FileMessageRequest(
                100L,
                null,
                null,
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

        ChatRoomMember member1 = ChatRoomMember.builder()
                .id(1L)
                .chatRoomId(100L)
                .userId(1L)
                .build();
        ChatRoomMember member2 = ChatRoomMember.builder()
                .id(2L)
                .chatRoomId(100L)
                .userId(2L)
                .build();
        List<ChatRoomMember> members = List.of(member1, member2);

        SendMessageUseCase.SendResult sendResult = new SendMessageUseCase.SendResult(
                fileMessage, "발신자", null, members);

        given(sendMessageUseCase.sendFileMessageWithContext(anyLong(), anyLong(), any(FileMessageCommand.class)))
                .willReturn(sendResult);

        // when
        chatWebSocketController.sendFileMessage(request, createMockHeaderAccessor(1L));

        // then
        verify(sendMessageUseCase).sendFileMessageWithContext(eq(100L), eq(1L), any(FileMessageCommand.class));
        verify(broadcastChatMessageUseCase).broadcastMessage(fileMessage, "발신자", null, members);
        verify(publishChatListUpdateUseCase).publishChatListUpdate(fileMessage, members, "발신자");
    }

    @Test
    @DisplayName("이미지 파일 메시지 전송 - 썸네일 포함")
    void should_sendImageMessage_when_thumbnailProvided() {
        // given
        FileMessageRequest request = new FileMessageRequest(
                100L,
                null,
                null,
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

        ChatRoomMember member1 = ChatRoomMember.builder()
                .id(1L)
                .chatRoomId(100L)
                .userId(1L)
                .build();
        ChatRoomMember member2 = ChatRoomMember.builder()
                .id(2L)
                .chatRoomId(100L)
                .userId(2L)
                .build();
        List<ChatRoomMember> members = List.of(member1, member2);

        SendMessageUseCase.SendResult sendResult = new SendMessageUseCase.SendResult(
                imageMessage, "발신자", null, members);

        given(sendMessageUseCase.sendFileMessageWithContext(anyLong(), anyLong(), any(FileMessageCommand.class)))
                .willReturn(sendResult);

        // when
        chatWebSocketController.sendFileMessage(request, createMockHeaderAccessor(1L));

        // then
        verify(broadcastChatMessageUseCase).broadcastMessage(imageMessage, "발신자", null, members);
        verify(publishChatListUpdateUseCase).publishChatListUpdate(imageMessage, members, "발신자");
    }

    @Test
    @DisplayName("리액션 추가 성공")
    void should_addReaction_when_validRequest() {
        // given
        AddReactionRequest request = new AddReactionRequest(1L, "THUMBS_UP");

        MessageReaction reaction = MessageReaction.builder()
                .id(10L)
                .messageId(1L)
                .userId(2L)
                .emoji(Emoji.THUMBS_UP)
                .build();
        setCreatedAt(reaction, LocalDateTime.now());

        ReactionResult result = new ReactionResult(reaction, 100L);

        given(addMessageReactionUseCase.addReactionWithContext(anyLong(), anyLong(), anyString()))
                .willReturn(result);

        // when
        chatWebSocketController.addReaction(request, createMockHeaderAccessor(2L));

        // then
        verify(addMessageReactionUseCase).addReactionWithContext(1L, 2L, "THUMBS_UP");
        verify(broadcastReactionEventUseCase).broadcastReactionEvent(reaction, 100L, "ADDED");
    }

    @Test
    @DisplayName("리액션 제거 성공")
    void should_removeReaction_when_validRequest() {
        // given
        RemoveReactionRequest request = new RemoveReactionRequest(1L, "THUMBS_UP");

        given(removeMessageReactionUseCase.removeReactionWithContext(1L, 2L, "THUMBS_UP"))
                .willReturn(100L);

        // when
        chatWebSocketController.removeReaction(request, createMockHeaderAccessor(2L));

        // then
        verify(removeMessageReactionUseCase).removeReactionWithContext(1L, 2L, "THUMBS_UP");
        verify(broadcastReactionEventUseCase).broadcastReactionEvent(any(MessageReaction.class), eq(100L), eq("REMOVED"));
    }

    @Test
    @DisplayName("리액션 제거 시 채팅방을 찾을 수 없으면 브로드캐스트 안 함")
    void should_notBroadcast_when_chatRoomNotFoundForRemoveReaction() {
        // given
        RemoveReactionRequest request = new RemoveReactionRequest(999L, "THUMBS_UP");

        given(removeMessageReactionUseCase.removeReactionWithContext(999L, 2L, "THUMBS_UP"))
                .willReturn(null);

        // when
        chatWebSocketController.removeReaction(request, createMockHeaderAccessor(2L));

        // then
        verify(broadcastReactionEventUseCase, never()).broadcastReactionEvent(any(), anyLong(), anyString());
    }

    @Test
    @DisplayName("파일 메시지 전송 시 fileName이 255자를 초과하면 거부된다")
    void should_rejectFileMessage_when_fileNameTooLong() {
        // given
        String tooLongFileName = "a".repeat(256) + ".jpg";
        FileMessageRequest request = new FileMessageRequest(
                100L,
                null,
                null,
                "https://storage.example.com/image.jpg",
                tooLongFileName,
                1024L,
                "image/jpeg",
                null
        );

        // when
        chatWebSocketController.sendFileMessage(request, createMockHeaderAccessor(1L));

        // then: 서비스 호출 안 함
        verify(sendMessageUseCase, never()).sendFileMessageWithContext(anyLong(), anyLong(), any());
    }

    @Test
    @DisplayName("파일 메시지 전송 시 fileName이 빈 문자열이면 거부된다")
    void should_rejectFileMessage_when_fileNameEmpty() {
        // given
        FileMessageRequest request = new FileMessageRequest(
                100L,
                null,
                null,
                "https://storage.example.com/image.jpg",
                "",
                1024L,
                "image/jpeg",
                null
        );

        // when
        chatWebSocketController.sendFileMessage(request, createMockHeaderAccessor(1L));

        // then
        verify(sendMessageUseCase, never()).sendFileMessageWithContext(anyLong(), anyLong(), any());
    }

    @Test
    @DisplayName("파일 메시지 전송 시 contentType이 허용 목록에 없으면 거부된다")
    void should_rejectFileMessage_when_contentTypeNotAllowed() {
        // given
        FileMessageRequest request = new FileMessageRequest(
                100L,
                null,
                null,
                "https://storage.example.com/malicious.exe",
                "malicious.exe",
                1024L,
                "application/x-msdownload",
                null
        );

        // when
        chatWebSocketController.sendFileMessage(request, createMockHeaderAccessor(1L));

        // then
        verify(sendMessageUseCase, never()).sendFileMessageWithContext(anyLong(), anyLong(), any());
    }

    @Test
    @DisplayName("채팅방 멤버가 아닌 사용자의 메시지 전송은 거부된다")
    void should_rejectMessage_when_userNotMember() {
        // given
        ChatMessageRequest request = new ChatMessageRequest(100L, "테스트 메시지");
        Long unauthorizedUserId = 999L;

        doThrow(new ChatRoomAccessDeniedException(100L, unauthorizedUserId))
                .when(chatRoomMemberValidator).validateMembership(100L, unauthorizedUserId);

        // when & then
        assertThrows(
                ChatRoomAccessDeniedException.class,
                () -> chatWebSocketController.sendMessage(request, createMockHeaderAccessor(unauthorizedUserId))
        );

        verify(sendMessageUseCase, never()).sendMessageWithContext(anyLong(), anyLong(), anyString());
    }

    @Test
    @DisplayName("채팅방 멤버가 아닌 사용자의 파일 메시지 전송은 거부된다")
    void should_rejectFileMessage_when_userNotMember() {
        // given
        FileMessageRequest request = new FileMessageRequest(
                100L,
                null,
                null,
                "https://storage.example.com/file.pdf",
                "document.pdf",
                1024L,
                "application/pdf",
                null
        );
        Long unauthorizedUserId = 999L;

        doThrow(new ChatRoomAccessDeniedException(100L, unauthorizedUserId))
                .when(chatRoomMemberValidator).validateMembership(100L, unauthorizedUserId);

        // when & then
        assertThrows(
                ChatRoomAccessDeniedException.class,
                () -> chatWebSocketController.sendFileMessage(request, createMockHeaderAccessor(unauthorizedUserId))
        );

        verify(sendMessageUseCase, never()).sendFileMessageWithContext(anyLong(), anyLong(), any());
    }

    @Test
    @DisplayName("채팅방 멤버가 아닌 사용자의 타이핑 상태 전송은 거부된다")
    void should_rejectTypingStatus_when_userNotMember() {
        // given
        TypingStatusRequest request = new TypingStatusRequest(100L, true);
        Long unauthorizedUserId = 999L;

        doThrow(new ChatRoomAccessDeniedException(100L, unauthorizedUserId))
                .when(chatRoomMemberValidator).validateMembership(100L, unauthorizedUserId);

        // when & then
        assertThrows(
                ChatRoomAccessDeniedException.class,
                () -> chatWebSocketController.typingStatus(request, createMockHeaderAccessor(unauthorizedUserId))
        );

        verify(publishTypingStatusUseCase, never()).publishTypingStatus(anyLong(), anyLong(), anyBoolean());
    }

    @Test
    @DisplayName("채팅방 멤버가 아닌 사용자의 presence ping은 거부된다")
    void should_rejectPresencePing_when_userNotMember() {
        // given
        PresencePingRequest request = new PresencePingRequest(100L);
        Long unauthorizedUserId = 999L;

        doThrow(new ChatRoomAccessDeniedException(100L, unauthorizedUserId))
                .when(chatRoomMemberValidator).validateMembership(100L, unauthorizedUserId);

        StompHeaderAccessor headerAccessor = createMockHeaderAccessor(unauthorizedUserId);
        headerAccessor.setSessionId("test-session");

        // when & then
        assertThrows(
                ChatRoomAccessDeniedException.class,
                () -> chatWebSocketController.presencePing(request, headerAccessor)
        );

        verify(updatePresenceStatusUseCase, never()).markActive(anyLong(), anyLong(), anyString());
    }
}
