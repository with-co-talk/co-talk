package com.cotalk.application.service.chat;

import com.cotalk.common.fixture.ChatRoomTestFixture;
import com.cotalk.common.fixture.MessageTestFixture;
import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.entity.Message;
import com.cotalk.domain.port.outbound.ChatMessageBroker;
import com.cotalk.domain.port.outbound.ChatMessageBroker.ChatBroadcastMessage;
import com.cotalk.domain.port.outbound.FileStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

/**
 * {@link BroadcastChatMessageService} 유닛 테스트.
 * 채팅 메시지 브로드캐스트 로직을 검증한다.
 *
 * @author seunggu.lee
 */
@ExtendWith(MockitoExtension.class)
class BroadcastChatMessageServiceTest {

    @Mock
    private ChatMessageBroker chatMessageBroker;

    @Mock
    private FileStorage fileStorage;

    private BroadcastChatMessageService broadcastChatMessageService;

    @BeforeEach
    void setUp() {
        broadcastChatMessageService = new BroadcastChatMessageService(chatMessageBroker, fileStorage, 10);
        // 첨부파일 URL은 그대로 통과시켜 기존 검증을 유지한다(실제로는 단기 Pre-signed URL로 재발급).
        lenient().when(fileStorage.presignAttachmentUrl(anyString(), anyInt()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(fileStorage.presignAttachmentUrl(eq(null), anyInt())).thenReturn(null);
    }

    @Test
    void should_broadcastMessageWithCorrectUnreadCount_when_multipleMembers() {
        // given
        Message message = MessageTestFixture.createTextMessage(1L, 100L, 1L);
        String senderNickname = "발신자";
        String senderAvatarUrl = "https://example.com/avatar.png";
        List<ChatRoomMember> members = List.of(
                ChatRoomTestFixture.createChatRoomMember(1L, 100L, 1L),
                ChatRoomTestFixture.createChatRoomMember(2L, 100L, 2L),
                ChatRoomTestFixture.createChatRoomMember(3L, 100L, 3L)
        );

        // when
        broadcastChatMessageService.broadcastMessage(message, senderNickname, senderAvatarUrl, members);

        // then
        ArgumentCaptor<ChatBroadcastMessage> captor = ArgumentCaptor.forClass(ChatBroadcastMessage.class);
        verify(chatMessageBroker).publish(eq(100L), captor.capture());

        ChatBroadcastMessage broadcastMessage = captor.getValue();
        assertThat(broadcastMessage.unreadCount()).isEqualTo(2); // 3명 - 발신자 1명 = 2명
        assertThat(broadcastMessage.messageId()).isEqualTo(1L);
        assertThat(broadcastMessage.senderId()).isEqualTo(1L);
        assertThat(broadcastMessage.senderNickname()).isEqualTo(senderNickname);
        assertThat(broadcastMessage.senderAvatarUrl()).isEqualTo(senderAvatarUrl);
        assertThat(broadcastMessage.roomId()).isEqualTo(100L);
    }

    @Test
    void should_broadcastMessageWithZeroUnreadCount_when_singleMember() {
        // given
        Message message = MessageTestFixture.createTextMessage(1L, 100L, 1L);
        String senderNickname = "발신자";
        String senderAvatarUrl = "https://example.com/avatar.png";
        List<ChatRoomMember> members = List.of(
                ChatRoomTestFixture.createChatRoomMember(1L, 100L, 1L)
        );

        // when
        broadcastChatMessageService.broadcastMessage(message, senderNickname, senderAvatarUrl, members);

        // then
        ArgumentCaptor<ChatBroadcastMessage> captor = ArgumentCaptor.forClass(ChatBroadcastMessage.class);
        verify(chatMessageBroker).publish(eq(100L), captor.capture());

        ChatBroadcastMessage broadcastMessage = captor.getValue();
        assertThat(broadcastMessage.unreadCount()).isEqualTo(0); // 1명 - 발신자 1명 = 0명
    }

    @Test
    void should_broadcastImageMessage_when_messageTypeIsImage() {
        // given
        Message imageMessage = MessageTestFixture.createImageMessage(
                1L, 100L, 1L, "https://example.com/image.jpg"
        );
        String senderNickname = "발신자";
        String senderAvatarUrl = "https://example.com/avatar.png";
        List<ChatRoomMember> members = List.of(
                ChatRoomTestFixture.createChatRoomMember(1L, 100L, 1L),
                ChatRoomTestFixture.createChatRoomMember(2L, 100L, 2L)
        );

        // when
        broadcastChatMessageService.broadcastMessage(imageMessage, senderNickname, senderAvatarUrl, members);

        // then
        ArgumentCaptor<ChatBroadcastMessage> captor = ArgumentCaptor.forClass(ChatBroadcastMessage.class);
        verify(chatMessageBroker).publish(eq(100L), captor.capture());

        ChatBroadcastMessage broadcastMessage = captor.getValue();
        assertThat(broadcastMessage.type()).isEqualTo("IMAGE");
        assertThat(broadcastMessage.fileUrl()).isEqualTo("https://example.com/image.jpg");
        assertThat(broadcastMessage.fileName()).isEqualTo("image.jpg");
        assertThat(broadcastMessage.fileSize()).isEqualTo(102400L);
        assertThat(broadcastMessage.thumbnailUrl()).isNotNull();
    }

    @Test
    void should_broadcastMessageWithTimestamp_when_messageHasCreatedAt() {
        // given
        Message message = MessageTestFixture.createTextMessage(1L, 100L, 1L);
        String senderNickname = "발신자";
        String senderAvatarUrl = "https://example.com/avatar.png";
        List<ChatRoomMember> members = List.of(
                ChatRoomTestFixture.createChatRoomMember(1L, 100L, 1L)
        );

        // when
        broadcastChatMessageService.broadcastMessage(message, senderNickname, senderAvatarUrl, members);

        // then
        ArgumentCaptor<ChatBroadcastMessage> captor = ArgumentCaptor.forClass(ChatBroadcastMessage.class);
        verify(chatMessageBroker).publish(eq(100L), captor.capture());

        ChatBroadcastMessage broadcastMessage = captor.getValue();
        long expectedTimestamp = message.getCreatedAt().atZone(ZoneOffset.UTC).toInstant().toEpochMilli();
        assertThat(broadcastMessage.createdAtMillis()).isEqualTo(expectedTimestamp);
    }
}
