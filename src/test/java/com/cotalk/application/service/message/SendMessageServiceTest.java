package com.cotalk.application.service.message;

import com.cotalk.domain.entity.ChatRoom;
import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.entity.Message;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.model.Email;
import com.cotalk.domain.exception.BlockedRelationshipException;
import com.cotalk.domain.exception.ChatRoomAccessDeniedException;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.ChatRoomRepository;
import com.cotalk.domain.port.outbound.ChatRoomPresenceTracker;
import com.cotalk.domain.port.outbound.IdGenerator;
import com.cotalk.domain.port.outbound.MessageRepository;
import com.cotalk.domain.port.outbound.NotificationCommandPort;
import com.cotalk.domain.port.outbound.TimeProvider;
import com.cotalk.domain.port.outbound.UserRepository;
import com.cotalk.domain.port.outbound.MetricsPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;
import java.util.Set;

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
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private IdGenerator idGenerator;

    @Mock
    private NotificationCommandPort notificationCommandPort;

    @Mock
    private ChatRoomPresenceTracker chatRoomPresenceTracker;

    @Mock
    private MessageLinkPreviewService messageLinkPreviewService;

    @Mock
    private MessageBroadcastService messageBroadcastService;

    @Mock
    private MetricsPort customMetrics;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private TimeProvider timeProvider;

    @Mock
    private com.cotalk.domain.validator.BlockValidator blockValidator;

    private SendMessageService sendMessageService;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        sendMessageService = new SendMessageService(
                messageRepository, chatRoomMemberRepository, chatRoomRepository, userRepository, idGenerator,
                notificationCommandPort, chatRoomPresenceTracker, customMetrics,
                messageLinkPreviewService, messageBroadcastService, transactionTemplate, timeProvider,
                new com.cotalk.domain.validator.FileMessageValidator(), blockValidator);

        // TransactionTemplate: 콜백을 즉시 실행 (트랜잭션 없이 동기 실행)
        lenient().when(transactionTemplate.execute(any(TransactionCallback.class)))
                .thenAnswer(invocation -> {
                    TransactionCallback<?> callback = invocation.getArgument(0);
                    return callback.doInTransaction(null);
                });

        // Default mock behavior (lenient to avoid UnnecessaryStubbingException)
        lenient().when(chatRoomMemberRepository.updateLastReadMessageIdIfNewer(anyLong(), anyLong(), any(), anyLong()))
                .thenReturn(1);
        lenient().when(chatRoomPresenceTracker.getActiveUserIds(anyLong(), anyList())).thenReturn(Set.of());
        lenient().when(messageLinkPreviewService.extractFirstUrl(anyString())).thenReturn(Optional.empty());
        lenient().when(timeProvider.now()).thenReturn(java.time.LocalDateTime.now());
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
                    .email(new Email("sender@test.com"))
                    .nickname("발신자")
                    .passwordHash("hash")
                    .build();

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
            verify(notificationCommandPort).sendNewMessageNotificationBulk(
                    eq(List.of(receiverId)), eq("발신자"), eq(content), eq(chatRoomId), nullable(String.class));
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
                    .email(new Email("sender@test.com"))
                    .nickname("발신자")
                    .passwordHash("hash")
                    .build();

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

            // 채팅방에 멤버가 없거나 senderId가 포함되지 않은 경우
            given(chatRoomMemberRepository.findByChatRoomId(chatRoomId))
                    .willReturn(List.of()); // 빈 리스트 or 다른 사용자만 있음

            // when & then
            assertThatThrownBy(() -> sendMessageService.sendMessage(chatRoomId, senderId, "메시지"))
                    .isInstanceOf(ChatRoomAccessDeniedException.class);
        }

        @Test
        @DisplayName("1:1 채팅방에서 상대와 차단 관계면 메시지 전송이 거부된다 (양방향)")
        void should_ThrowException_when_BlockedInDirectChat() {
            // given
            Long chatRoomId = 1L;
            Long senderId = 2L;
            Long receiverId = 3L;

            ChatRoomMember senderMember = ChatRoomMember.builder()
                    .id(10L).chatRoomId(chatRoomId).userId(senderId).build();
            ChatRoomMember receiverMember = ChatRoomMember.builder()
                    .id(11L).chatRoomId(chatRoomId).userId(receiverId).build();

            User sender = User.builder()
                    .id(senderId).email(new Email("sender@test.com")).nickname("발신자").passwordHash("hash").build();

            ChatRoom directRoom = ChatRoom.builder()
                    .id(chatRoomId).type(ChatRoom.ChatRoomType.DIRECT).build();

            given(chatRoomMemberRepository.findByChatRoomId(chatRoomId))
                    .willReturn(List.of(senderMember, receiverMember));
            given(userRepository.findById(senderId)).willReturn(Optional.of(sender));
            given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.of(directRoom));
            org.mockito.BDDMockito.willThrow(new BlockedRelationshipException())
                    .given(blockValidator).validateNotBlocked(senderId, receiverId);

            // when & then
            assertThatThrownBy(() -> sendMessageService.sendMessage(chatRoomId, senderId, "메시지"))
                    .isInstanceOf(BlockedRelationshipException.class);
            verify(messageRepository, never()).save(any(Message.class));
        }

        @Test
        @DisplayName("그룹 채팅방(멤버 3명)에서는 차단 검사를 하지 않는다")
        void should_NotCheckBlock_when_GroupChat() {
            // given
            Long chatRoomId = 1L;
            Long senderId = 2L;
            Long messageId = 100L;

            ChatRoomMember m1 = ChatRoomMember.builder().id(10L).chatRoomId(chatRoomId).userId(senderId).build();
            ChatRoomMember m2 = ChatRoomMember.builder().id(11L).chatRoomId(chatRoomId).userId(3L).build();
            ChatRoomMember m3 = ChatRoomMember.builder().id(12L).chatRoomId(chatRoomId).userId(4L).build();

            User sender = User.builder()
                    .id(senderId).email(new Email("sender@test.com")).nickname("발신자").passwordHash("hash").build();

            given(chatRoomMemberRepository.findByChatRoomId(chatRoomId))
                    .willReturn(List.of(m1, m2, m3));
            given(userRepository.findById(senderId)).willReturn(Optional.of(sender));
            given(idGenerator.nextId()).willReturn(messageId);
            given(messageRepository.save(any(Message.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            sendMessageService.sendMessage(chatRoomId, senderId, "메시지");

            // then - 멤버 3명이므로 차단 검사/방 조회 없이 정상 전송
            verify(blockValidator, never()).validateNotBlocked(anyLong(), anyLong());
            verify(chatRoomRepository, never()).findById(anyLong());
            verify(messageRepository).save(any(Message.class));
        }

        @Test
        @DisplayName("1:1 채팅방에서 상대가 나가 발신자만 남은 경우 차단 검사 없이 전송된다(검사 대상 없음)")
        void should_NotCheckBlock_when_DirectChatHasOnlySender() {
            // given: 1:1(DIRECT) 방이지만 상대가 나가 발신자 1명만 남은 상태(재초대 전)
            Long chatRoomId = 1L;
            Long senderId = 2L;
            Long messageId = 100L;

            ChatRoomMember senderMember = ChatRoomMember.builder()
                    .id(10L).chatRoomId(chatRoomId).userId(senderId).build();

            User sender = User.builder()
                    .id(senderId).email(new Email("sender@test.com")).nickname("발신자").passwordHash("hash").build();

            given(chatRoomMemberRepository.findByChatRoomId(chatRoomId))
                    .willReturn(List.of(senderMember)); // 발신자만 남음
            given(userRepository.findById(senderId)).willReturn(Optional.of(sender));
            given(idGenerator.nextId()).willReturn(messageId);
            given(messageRepository.save(any(Message.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            sendMessageService.sendMessage(chatRoomId, senderId, "메시지");

            // then - 상대(검사 대상)가 없으므로 차단 검사/방 조회 없이 정상 전송
            // (상대가 다시 들어오는 재초대 경로에서 차단을 검증하므로 우회가 아님)
            verify(blockValidator, never()).validateNotBlocked(anyLong(), anyLong());
            verify(chatRoomRepository, never()).findById(anyLong());
            verify(messageRepository).save(any(Message.class));
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

            User sender = User.builder()
                    .id(senderId)
                    .email(new Email("sender@test.com"))
                    .nickname("발신자")
                    .passwordHash("hash")
                    .build();

            given(chatRoomMemberRepository.findByChatRoomId(chatRoomId))
                    .willReturn(List.of(member));
            given(userRepository.findById(senderId)).willReturn(Optional.of(sender));

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
                    .email(new Email("sender@test.com"))
                    .nickname("발신자")
                    .passwordHash("hash")
                    .build();

            SendMessageUseCase.FileMessageCommand command = new SendMessageUseCase.FileMessageCommand(
                    "http://localhost:8080/files/uploads/2/abc.jpg",
                    "photo.jpg",
                    1024L,
                    "image/jpeg",
                    "http://localhost:8080/files/uploads/2/thumb.jpg"
            );

            given(chatRoomMemberRepository.findByChatRoomId(chatRoomId))
                    .willReturn(List.of(member));
            given(userRepository.findById(senderId)).willReturn(Optional.of(sender));
            given(idGenerator.nextId()).willReturn(messageId);
            given(messageRepository.save(any(Message.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            Message result = sendMessageService.sendFileMessage(chatRoomId, senderId, command);

            // then
            assertThat(result.getType()).isEqualTo(Message.MessageType.IMAGE);
            assertThat(result.getFileUrl()).isEqualTo("http://localhost:8080/files/uploads/2/abc.jpg");
            assertThat(result.getFileName()).isEqualTo("photo.jpg");
            assertThat(result.getThumbnailUrl()).isEqualTo("http://localhost:8080/files/uploads/2/thumb.jpg");
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
                    .email(new Email("sender@test.com"))
                    .nickname("발신자")
                    .passwordHash("hash")
                    .build();

            SendMessageUseCase.FileMessageCommand command = new SendMessageUseCase.FileMessageCommand(
                    "http://localhost:8080/files/uploads/2/doc.pdf",
                    "document.pdf",
                    2048L,
                    "application/pdf",
                    null
            );

            given(chatRoomMemberRepository.findByChatRoomId(chatRoomId))
                    .willReturn(List.of(member));
            given(userRepository.findById(senderId)).willReturn(Optional.of(sender));
            given(idGenerator.nextId()).willReturn(messageId);
            given(messageRepository.save(any(Message.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            Message result = sendMessageService.sendFileMessage(chatRoomId, senderId, command);

            // then
            assertThat(result.getType()).isEqualTo(Message.MessageType.FILE);
            assertThat(result.getFileUrl()).isEqualTo("http://localhost:8080/files/uploads/2/doc.pdf");
            assertThat(result.getFileName()).isEqualTo("document.pdf");
        }

        @Test
        @DisplayName("contentType이 null이면 허용 목록에 없으므로 거부한다")
        void should_RejectFile_when_contentTypeIsNull() {
            // given
            Long chatRoomId = 1L;
            Long senderId = 2L;

            SendMessageUseCase.FileMessageCommand command = new SendMessageUseCase.FileMessageCommand(
                    "http://localhost:8080/files/uploads/2/file.bin",
                    "unknown.file",
                    1024L,
                    null, // contentType이 null → 허용 목록 밖
                    null
            );

            // when & then
            assertThatThrownBy(() -> sendMessageService.sendFileMessage(chatRoomId, senderId, command))
                    .isInstanceOf(com.cotalk.domain.exception.FileUploadException.class);
            verify(messageRepository, never()).save(any(Message.class));
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
                    .id(senderId).email(new Email("sender@test.com")).nickname("발신자").passwordHash("hash").build();

            SendMessageUseCase.FileMessageCommand command = new SendMessageUseCase.FileMessageCommand(
                    "http://localhost:8080/files/uploads/2/abc.jpg",
                    "photo.jpg",
                    1024L,
                    "image/jpeg",
                    null
            );

            given(chatRoomMemberRepository.findByChatRoomId(chatRoomId))
                    .willReturn(List.of(senderMember, receiverMember));
            given(userRepository.findById(senderId)).willReturn(Optional.of(sender));
            given(idGenerator.nextId()).willReturn(messageId);
            given(messageRepository.save(any(Message.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            sendMessageService.sendFileMessage(chatRoomId, senderId, command);

            // then
            verify(notificationCommandPort).sendNewMessageNotificationBulk(
                    eq(List.of(receiverId)), eq("발신자"), eq("📷 사진을 보냈습니다."), eq(chatRoomId), nullable(String.class));
        }

        @Test
        @DisplayName("채팅방 멤버가 아니면 예외가 발생한다")
        void should_ThrowException_when_NotChatRoomMemberForFile() {
            // given
            Long chatRoomId = 1L;
            Long senderId = 2L;

            SendMessageUseCase.FileMessageCommand command = new SendMessageUseCase.FileMessageCommand(
                    "http://localhost:8080/files/uploads/2/abc.jpg",
                    "photo.jpg",
                    1024L,
                    "image/jpeg",
                    null
            );

            // 채팅방에 멤버가 없거나 senderId가 포함되지 않은 경우
            given(chatRoomMemberRepository.findByChatRoomId(chatRoomId))
                    .willReturn(List.of()); // 빈 리스트

            // when & then
            assertThatThrownBy(() -> sendMessageService.sendFileMessage(chatRoomId, senderId, command))
                    .isInstanceOf(ChatRoomAccessDeniedException.class);
        }

        @Test
        @DisplayName("허용 목록 밖 contentType이면 거부한다")
        void should_RejectFile_when_contentTypeNotAllowed() {
            // given
            Long chatRoomId = 1L;
            Long senderId = 2L;

            SendMessageUseCase.FileMessageCommand command = new SendMessageUseCase.FileMessageCommand(
                    "http://localhost:8080/files/uploads/2/evil.exe",
                    "evil.exe",
                    1024L,
                    "application/x-msdownload", // 허용 목록 밖
                    null
            );

            // when & then
            assertThatThrownBy(() -> sendMessageService.sendFileMessage(chatRoomId, senderId, command))
                    .isInstanceOf(com.cotalk.domain.exception.FileUploadException.class);
            verify(messageRepository, never()).save(any(Message.class));
        }

        @Test
        @DisplayName("uploads 경로가 없는 외부/위조 fileUrl이면 거부한다")
        void should_RejectFile_when_externalFileUrl() {
            // given
            Long chatRoomId = 1L;
            Long senderId = 2L;

            // 본 서버 업로드 객체 경로(uploads/{senderId}/...)가 전혀 없는 외부 URL
            SendMessageUseCase.FileMessageCommand external = new SendMessageUseCase.FileMessageCommand(
                    "https://evil.example.com/malware.jpg",
                    "photo.jpg",
                    1024L,
                    "image/jpeg",
                    null
            );

            // when & then
            assertThatThrownBy(() -> sendMessageService.sendFileMessage(chatRoomId, senderId, external))
                    .isInstanceOf(com.cotalk.domain.exception.FileUploadException.class);
            verify(messageRepository, never()).save(any(Message.class));
        }

        @Test
        @DisplayName("타인 소유 업로드 경로(fileUrl)면 거부한다")
        void should_RejectFile_when_otherUserUploadPath() {
            // given
            Long chatRoomId = 1L;
            Long senderId = 2L;

            SendMessageUseCase.FileMessageCommand command = new SendMessageUseCase.FileMessageCommand(
                    "http://localhost:8080/files/uploads/999/abc.jpg", // 타인(999) 경로
                    "photo.jpg",
                    1024L,
                    "image/jpeg",
                    null
            );

            // when & then
            assertThatThrownBy(() -> sendMessageService.sendFileMessage(chatRoomId, senderId, command))
                    .isInstanceOf(com.cotalk.domain.exception.FileUploadException.class);
            verify(messageRepository, never()).save(any(Message.class));
        }

        @Test
        @DisplayName("경로 탈출(..)이 포함된 fileUrl이면 거부한다")
        void should_RejectFile_when_pathTraversal() {
            // given
            Long chatRoomId = 1L;
            Long senderId = 2L;

            SendMessageUseCase.FileMessageCommand command = new SendMessageUseCase.FileMessageCommand(
                    "http://localhost:8080/files/uploads/2/../999/secret.jpg",
                    "secret.jpg",
                    1024L,
                    "image/jpeg",
                    null
            );

            // when & then
            assertThatThrownBy(() -> sendMessageService.sendFileMessage(chatRoomId, senderId, command))
                    .isInstanceOf(com.cotalk.domain.exception.FileUploadException.class);
            verify(messageRepository, never()).save(any(Message.class));
        }

        @Test
        @DisplayName("정상 업로드 URL과 허용 contentType이면 통과한다")
        void should_AcceptFile_when_validUploadUrlAndContentType() {
            // given
            Long chatRoomId = 1L;
            Long senderId = 2L;
            Long messageId = 100L;

            ChatRoomMember member = ChatRoomMember.builder()
                    .id(10L).chatRoomId(chatRoomId).userId(senderId).build();
            User sender = User.builder()
                    .id(senderId).email(new Email("sender@test.com")).nickname("발신자").passwordHash("hash").build();

            // InMemoryFileStorage가 반환하는 실제 형식: {baseUrl}/uploads/{userId}/{uuid}.ext
            SendMessageUseCase.FileMessageCommand command = new SendMessageUseCase.FileMessageCommand(
                    "http://localhost:8080/files/uploads/2/3f9-uuid.jpg",
                    "photo.jpg",
                    1024L,
                    "image/jpeg",
                    "http://localhost:8080/files/uploads/2/3f9-thumb.jpg"
            );

            given(chatRoomMemberRepository.findByChatRoomId(chatRoomId)).willReturn(List.of(member));
            given(userRepository.findById(senderId)).willReturn(Optional.of(sender));
            given(idGenerator.nextId()).willReturn(messageId);
            given(messageRepository.save(any(Message.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            Message result = sendMessageService.sendFileMessage(chatRoomId, senderId, command);

            // then
            assertThat(result.getType()).isEqualTo(Message.MessageType.IMAGE);
            assertThat(result.getFileUrl()).isEqualTo("http://localhost:8080/files/uploads/2/3f9-uuid.jpg");
            verify(messageRepository).save(any(Message.class));
        }
    }

    @Nested
    @DisplayName("TransactionTemplate 반환값 검증")
    class TransactionResultValidation {

        @Test
        @DisplayName("트랜잭션 실행 결과가 null이면 예외가 발생한다")
        @SuppressWarnings("unchecked")
        void should_throwException_when_transactionReturnsNull() {
            // given
            Long chatRoomId = 1L;
            Long senderId = 2L;
            String content = "테스트 메시지";

            // TransactionTemplate이 null을 반환하도록 설정 (any(TransactionCallback.class)는 제네릭이라 unchecked)
            given(transactionTemplate.execute(any(TransactionCallback.class)))
                    .willReturn(null);

            // when & then
            assertThatThrownBy(() -> sendMessageService.sendMessage(chatRoomId, senderId, content))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("트랜잭션");
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
                    .id(senderId).email(new Email("sender@test.com")).nickname("발신자").passwordHash("hash").build();

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
            verify(notificationCommandPort).sendNewMessageNotificationBulk(
                    receiverIdsCaptor.capture(), anyString(), anyString(), anyLong(), nullable(String.class));

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

            given(chatRoomMemberRepository.findByChatRoomId(chatRoomId))
                    .willReturn(List.of(senderMember, receiverMember));
            given(userRepository.findById(senderId)).willReturn(Optional.empty());
            given(idGenerator.nextId()).willReturn(messageId);
            given(messageRepository.save(any(Message.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            sendMessageService.sendMessage(chatRoomId, senderId, "테스트");

            // then
            ArgumentCaptor<String> nicknameCaptor = ArgumentCaptor.forClass(String.class);
            verify(notificationCommandPort).sendNewMessageNotificationBulk(
                    anyList(), nicknameCaptor.capture(), anyString(), anyLong(), nullable(String.class));

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
                    .id(senderId).email(new Email("sender@test.com")).nickname("발신자").passwordHash("hash").build();

            given(chatRoomMemberRepository.findByChatRoomId(chatRoomId))
                    .willReturn(List.of(senderMember)); // 발신자만 있음
            given(userRepository.findById(senderId)).willReturn(Optional.of(sender));
            given(idGenerator.nextId()).willReturn(messageId);
            given(messageRepository.save(any(Message.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            sendMessageService.sendMessage(chatRoomId, senderId, "테스트");

            // then
            verify(notificationCommandPort, never()).sendNewMessageNotificationBulk(
                    anyList(), anyString(), anyString(), anyLong(), nullable(String.class));
        }
    }
}
