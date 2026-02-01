package com.cotalk.application.service.message;

import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.entity.Message;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.ChatRoomAccessDeniedException;
import com.cotalk.domain.port.inbound.notification.SendPushNotificationUseCase;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.ChatRoomPresenceTracker;
import com.cotalk.domain.port.outbound.IdGenerator;
import com.cotalk.domain.port.outbound.MessageRepository;
import com.cotalk.domain.port.outbound.UserRepository;
import com.cotalk.domain.validator.ChatRoomMemberValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.cotalk.domain.port.inbound.message.SendMessageUseCase;

@ExtendWith(MockitoExtension.class)
@DisplayName("SendMessageService")
class SendMessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private IdGenerator idGenerator;

    @Mock
    private SendPushNotificationUseCase sendPushNotificationUseCase;

    @Mock
    private ChatRoomPresenceTracker chatRoomPresenceTracker;

    private ChatRoomMemberValidator chatRoomMemberValidator;

    private SendMessageService sendMessageService;

    @BeforeEach
    void setUp() {
        chatRoomMemberValidator = new ChatRoomMemberValidator(chatRoomMemberRepository);
        sendMessageService = new SendMessageService(
                messageRepository, chatRoomMemberRepository, userRepository, idGenerator, sendPushNotificationUseCase, chatRoomMemberValidator, chatRoomPresenceTracker);

        // Default mock behavior (lenient to avoid UnnecessaryStubbingException)
        lenient().when(chatRoomMemberRepository.updateLastReadMessageIdIfNewer(anyLong(), anyLong(), any(), anyLong()))
                .thenReturn(1);
        lenient().when(chatRoomPresenceTracker.isActive(anyLong(), anyLong())).thenReturn(false);
    }

    @Nested
    @DisplayName("메시지 전송 성공 시")
    class SendMessageSuccess {

        @Test
        @DisplayName("유효한 입력으로 메시지를 전송하면 저장된 메시지를 반환한다")
        void should_ReturnMessage_when_ValidInputsProvided() {
            // given
            Long chatRoomId = 1L;
            Long senderId = 2L;
            Long receiverId = 3L;
            String content = "안녕하세요!";
            Long messageId = 100L;

            ChatRoomMember senderMember = ChatRoomMember.builder()
                    .id(10L)
                    .chatRoomId(chatRoomId)
                    .userId(senderId)
                    .build();

            ChatRoomMember receiverMember = ChatRoomMember.builder()
                    .id(11L)
                    .chatRoomId(chatRoomId)
                    .userId(receiverId)
                    .build();

            User sender = User.builder()
                    .id(senderId)
                    .email("sender@test.com")
                    .nickname("발신자")
                    .passwordHash("hash")
                    .build();

            given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, senderId))
                    .willReturn(Optional.of(senderMember));
            given(chatRoomMemberRepository.findByChatRoomId(chatRoomId))
                    .willReturn(List.of(senderMember, receiverMember));
            given(userRepository.findById(senderId)).willReturn(Optional.of(sender));
            given(idGenerator.nextId()).willReturn(messageId);
            given(messageRepository.save(any(Message.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            Message result = sendMessageService.sendMessage(chatRoomId, senderId, content);

            // then
            assertThat(result.getId()).isEqualTo(messageId);
            assertThat(result.getChatRoomId()).isEqualTo(chatRoomId);
            assertThat(result.getSenderId()).isEqualTo(senderId);
            assertThat(result.getContent()).isEqualTo(content);

            // 벌크 푸시 알림이 수신자에게 전송되었는지 검증
            verify(sendPushNotificationUseCase).sendNewMessageNotificationBulk(
                    List.of(receiverId), "발신자", content, chatRoomId);
        }

        @Test
        @DisplayName("Snowflake ID가 메시지 ID로 설정된다")
        void should_UseSnowflakeId_when_SendingMessage() {
            // given
            Long chatRoomId = 1L;
            Long senderId = 2L;
            Long snowflakeId = 999L;

            ChatRoomMember member = ChatRoomMember.builder()
                    .id(10L)
                    .chatRoomId(chatRoomId)
                    .userId(senderId)
                    .build();

            User sender = User.builder()
                    .id(senderId)
                    .email("sender@test.com")
                    .nickname("발신자")
                    .passwordHash("hash")
                    .build();

            given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, senderId))
                    .willReturn(Optional.of(member));
            given(chatRoomMemberRepository.findByChatRoomId(chatRoomId))
                    .willReturn(List.of(member));
            given(userRepository.findById(senderId)).willReturn(Optional.of(sender));
            given(idGenerator.nextId()).willReturn(snowflakeId);
            given(messageRepository.save(any(Message.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            sendMessageService.sendMessage(chatRoomId, senderId, "테스트");

            // then
            ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
            verify(messageRepository).save(messageCaptor.capture());
            assertThat(messageCaptor.getValue().getId()).isEqualTo(snowflakeId);
        }
    }

    @Nested
    @DisplayName("메시지 전송 실패 시")
    class SendMessageFailure {

        @Test
        @DisplayName("채팅방 멤버가 아니면 예외가 발생한다")
        void should_ThrowException_when_NotChatRoomMember() {
            // given
            Long chatRoomId = 1L;
            Long senderId = 2L;

            given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, senderId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sendMessageService.sendMessage(chatRoomId, senderId, "메시지"))
                    .isInstanceOf(ChatRoomAccessDeniedException.class);
        }

        @Test
        @DisplayName("빈 메시지 내용이면 예외가 발생한다")
        void should_ThrowException_when_EmptyContent() {
            // given
            Long chatRoomId = 1L;
            Long senderId = 2L;

            ChatRoomMember member = ChatRoomMember.builder()
                    .id(10L)
                    .chatRoomId(chatRoomId)
                    .userId(senderId)
                    .build();

            given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, senderId))
                    .willReturn(Optional.of(member));

            // when & then
            assertThatThrownBy(() -> sendMessageService.sendMessage(chatRoomId, senderId, ""))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("파일 메시지 전송 시")
    class SendFileMessage {

        @Test
        @DisplayName("이미지 파일을 전송하면 IMAGE 타입 메시지가 생성된다")
        void should_CreateImageMessage_when_ImageFileProvided() {
            // given
            Long chatRoomId = 1L;
            Long senderId = 2L;
            Long messageId = 100L;

            ChatRoomMember member = ChatRoomMember.builder()
                    .id(10L)
                    .chatRoomId(chatRoomId)
                    .userId(senderId)
                    .build();

            User sender = User.builder()
                    .id(senderId)
                    .email("sender@test.com")
                    .nickname("발신자")
                    .passwordHash("hash")
                    .build();

            SendMessageUseCase.FileMessageCommand command = new SendMessageUseCase.FileMessageCommand(
                    "https://storage.example.com/image.jpg",
                    "photo.jpg",
                    1024L,
                    "image/jpeg",
                    "https://storage.example.com/thumb.jpg"
            );

            given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, senderId))
                    .willReturn(Optional.of(member));
            given(chatRoomMemberRepository.findByChatRoomId(chatRoomId))
                    .willReturn(List.of(member));
            given(userRepository.findById(senderId)).willReturn(Optional.of(sender));
            given(idGenerator.nextId()).willReturn(messageId);
            given(messageRepository.save(any(Message.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            Message result = sendMessageService.sendFileMessage(chatRoomId, senderId, command);

            // then
            assertThat(result.getType()).isEqualTo(Message.MessageType.IMAGE);
            assertThat(result.getFileUrl()).isEqualTo("https://storage.example.com/image.jpg");
            assertThat(result.getFileName()).isEqualTo("photo.jpg");
            assertThat(result.getThumbnailUrl()).isEqualTo("https://storage.example.com/thumb.jpg");
        }

        @Test
        @DisplayName("일반 파일을 전송하면 FILE 타입 메시지가 생성된다")
        void should_CreateFileMessage_when_NormalFileProvided() {
            // given
            Long chatRoomId = 1L;
            Long senderId = 2L;
            Long messageId = 100L;

            ChatRoomMember member = ChatRoomMember.builder()
                    .id(10L)
                    .chatRoomId(chatRoomId)
                    .userId(senderId)
                    .build();

            User sender = User.builder()
                    .id(senderId)
                    .email("sender@test.com")
                    .nickname("발신자")
                    .passwordHash("hash")
                    .build();

            SendMessageUseCase.FileMessageCommand command = new SendMessageUseCase.FileMessageCommand(
                    "https://storage.example.com/doc.pdf",
                    "document.pdf",
                    2048L,
                    "application/pdf",
                    null
            );

            given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, senderId))
                    .willReturn(Optional.of(member));
            given(chatRoomMemberRepository.findByChatRoomId(chatRoomId))
                    .willReturn(List.of(member));
            given(userRepository.findById(senderId)).willReturn(Optional.of(sender));
            given(idGenerator.nextId()).willReturn(messageId);
            given(messageRepository.save(any(Message.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            Message result = sendMessageService.sendFileMessage(chatRoomId, senderId, command);

            // then
            assertThat(result.getType()).isEqualTo(Message.MessageType.FILE);
            assertThat(result.getFileUrl()).isEqualTo("https://storage.example.com/doc.pdf");
            assertThat(result.getFileName()).isEqualTo("document.pdf");
        }

        @Test
        @DisplayName("contentType이 null인 경우 FILE 타입으로 처리")
        void should_CreateFileMessage_when_contentTypeIsNull() {
            // given
            Long chatRoomId = 1L;
            Long senderId = 2L;
            Long messageId = 100L;

            ChatRoomMember member = ChatRoomMember.builder()
                    .id(10L)
                    .chatRoomId(chatRoomId)
                    .userId(senderId)
                    .build();

            User sender = User.builder()
                    .id(senderId)
                    .email("sender@test.com")
                    .nickname("발신자")
                    .passwordHash("hash")
                    .build();

            SendMessageUseCase.FileMessageCommand command = new SendMessageUseCase.FileMessageCommand(
                    "https://storage.example.com/file",
                    "unknown.file",
                    1024L,
                    null, // contentType이 null
                    null
            );

            given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, senderId))
                    .willReturn(Optional.of(member));
            given(chatRoomMemberRepository.findByChatRoomId(chatRoomId))
                    .willReturn(List.of(member));
            given(userRepository.findById(senderId)).willReturn(Optional.of(sender));
            given(idGenerator.nextId()).willReturn(messageId);
            given(messageRepository.save(any(Message.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            Message result = sendMessageService.sendFileMessage(chatRoomId, senderId, command);

            // then
            assertThat(result.getType()).isEqualTo(Message.MessageType.FILE);
        }

        @Test
        @DisplayName("이미지 파일 전송 시 사진 알림이 전송된다")
        void should_SendImageNotification_when_ImageFileSent() {
            // given
            Long chatRoomId = 1L;
            Long senderId = 2L;
            Long receiverId = 3L;
            Long messageId = 100L;

            ChatRoomMember senderMember = ChatRoomMember.builder()
                    .id(10L).chatRoomId(chatRoomId).userId(senderId).build();
            ChatRoomMember receiverMember = ChatRoomMember.builder()
                    .id(11L).chatRoomId(chatRoomId).userId(receiverId).build();

            User sender = User.builder()
                    .id(senderId).email("sender@test.com").nickname("발신자").passwordHash("hash").build();

            SendMessageUseCase.FileMessageCommand command = new SendMessageUseCase.FileMessageCommand(
                    "https://storage.example.com/image.jpg",
                    "photo.jpg",
                    1024L,
                    "image/jpeg",
                    null
            );

            given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, senderId))
                    .willReturn(Optional.of(senderMember));
            given(chatRoomMemberRepository.findByChatRoomId(chatRoomId))
                    .willReturn(List.of(senderMember, receiverMember));
            given(userRepository.findById(senderId)).willReturn(Optional.of(sender));
            given(idGenerator.nextId()).willReturn(messageId);
            given(messageRepository.save(any(Message.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            sendMessageService.sendFileMessage(chatRoomId, senderId, command);

            // then
            verify(sendPushNotificationUseCase).sendNewMessageNotificationBulk(
                    List.of(receiverId), "발신자", "📷 사진을 보냈습니다.", chatRoomId);
        }

        @Test
        @DisplayName("채팅방 멤버가 아니면 예외가 발생한다")
        void should_ThrowException_when_NotChatRoomMemberForFile() {
            // given
            Long chatRoomId = 1L;
            Long senderId = 2L;

            SendMessageUseCase.FileMessageCommand command = new SendMessageUseCase.FileMessageCommand(
                    "https://storage.example.com/image.jpg",
                    "photo.jpg",
                    1024L,
                    "image/jpeg",
                    null
            );

            given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, senderId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sendMessageService.sendFileMessage(chatRoomId, senderId, command))
                    .isInstanceOf(ChatRoomAccessDeniedException.class);
        }
    }

    @Nested
    @DisplayName("푸시 알림 전송 시")
    class PushNotification {

        @Test
        @DisplayName("발신자를 제외한 멤버들에게만 알림이 전송된다")
        void should_ExcludeSender_when_SendingPushNotification() {
            // given
            Long chatRoomId = 1L;
            Long senderId = 2L;
            Long receiver1Id = 3L;
            Long receiver2Id = 4L;
            Long messageId = 100L;

            ChatRoomMember senderMember = ChatRoomMember.builder()
                    .id(10L).chatRoomId(chatRoomId).userId(senderId).build();
            ChatRoomMember receiver1Member = ChatRoomMember.builder()
                    .id(11L).chatRoomId(chatRoomId).userId(receiver1Id).build();
            ChatRoomMember receiver2Member = ChatRoomMember.builder()
                    .id(12L).chatRoomId(chatRoomId).userId(receiver2Id).build();

            User sender = User.builder()
                    .id(senderId).email("sender@test.com").nickname("발신자").passwordHash("hash").build();

            given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, senderId))
                    .willReturn(Optional.of(senderMember));
            given(chatRoomMemberRepository.findByChatRoomId(chatRoomId))
                    .willReturn(List.of(senderMember, receiver1Member, receiver2Member));
            given(userRepository.findById(senderId)).willReturn(Optional.of(sender));
            given(idGenerator.nextId()).willReturn(messageId);
            given(messageRepository.save(any(Message.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            sendMessageService.sendMessage(chatRoomId, senderId, "테스트 메시지");

            // then
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Long>> receiverIdsCaptor = ArgumentCaptor.forClass(List.class);
            verify(sendPushNotificationUseCase).sendNewMessageNotificationBulk(
                    receiverIdsCaptor.capture(), anyString(), anyString(), anyLong());

            assertThat(receiverIdsCaptor.getValue())
                    .containsExactlyInAnyOrder(receiver1Id, receiver2Id)
                    .doesNotContain(senderId);
        }

        @Test
        @DisplayName("발신자 정보가 없으면 알 수 없음으로 표시된다")
        void should_UseUnknownNickname_when_SenderNotFound() {
            // given
            Long chatRoomId = 1L;
            Long senderId = 2L;
            Long receiverId = 3L;
            Long messageId = 100L;

            ChatRoomMember senderMember = ChatRoomMember.builder()
                    .id(10L).chatRoomId(chatRoomId).userId(senderId).build();
            ChatRoomMember receiverMember = ChatRoomMember.builder()
                    .id(11L).chatRoomId(chatRoomId).userId(receiverId).build();

            given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, senderId))
                    .willReturn(Optional.of(senderMember));
            given(chatRoomMemberRepository.findByChatRoomId(chatRoomId))
                    .willReturn(List.of(senderMember, receiverMember));
            given(userRepository.findById(senderId)).willReturn(Optional.empty());
            given(idGenerator.nextId()).willReturn(messageId);
            given(messageRepository.save(any(Message.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            sendMessageService.sendMessage(chatRoomId, senderId, "테스트");

            // then
            ArgumentCaptor<String> nicknameCaptor = ArgumentCaptor.forClass(String.class);
            verify(sendPushNotificationUseCase).sendNewMessageNotificationBulk(
                    anyList(), nicknameCaptor.capture(), anyString(), anyLong());

            assertThat(nicknameCaptor.getValue()).isEqualTo("알 수 없음");
        }

        @Test
        @DisplayName("혼자 있는 채팅방에서는 알림을 보내지 않는다")
        void should_NotSendNotification_when_AloneInChatRoom() {
            // given
            Long chatRoomId = 1L;
            Long senderId = 2L;
            Long messageId = 100L;

            ChatRoomMember senderMember = ChatRoomMember.builder()
                    .id(10L).chatRoomId(chatRoomId).userId(senderId).build();

            User sender = User.builder()
                    .id(senderId).email("sender@test.com").nickname("발신자").passwordHash("hash").build();

            given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, senderId))
                    .willReturn(Optional.of(senderMember));
            given(chatRoomMemberRepository.findByChatRoomId(chatRoomId))
                    .willReturn(List.of(senderMember)); // 발신자만 있음
            given(userRepository.findById(senderId)).willReturn(Optional.of(sender));
            given(idGenerator.nextId()).willReturn(messageId);
            given(messageRepository.save(any(Message.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            sendMessageService.sendMessage(chatRoomId, senderId, "테스트");

            // then
            verify(sendPushNotificationUseCase, never()).sendNewMessageNotificationBulk(
                    anyList(), anyString(), anyString(), anyLong());
        }
    }
}
