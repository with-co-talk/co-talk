package com.cotalk.application.service.message;

import com.cotalk.domain.entity.Message;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.ChatRoomAccessDeniedException;
import com.cotalk.domain.exception.MessageNotFoundException;
import com.cotalk.domain.port.outbound.ChatMessageBroker;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.FileStorage;
import com.cotalk.domain.port.outbound.MessageRepository;
import com.cotalk.domain.port.outbound.UserEventBroker;
import com.cotalk.domain.port.outbound.UserRepository;
import com.cotalk.infrastructure.id.SnowflakeIdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MessageReplyForwardServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Mock
    private SnowflakeIdGenerator idGenerator;

    @Mock
    private ChatMessageBroker chatMessageBroker;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserEventBroker userEventBroker;

    @Mock
    private FileStorage fileStorage;

    private MessageReplyForwardService messageReplyForwardService;

    private Long senderId;
    private Long chatRoomId;
    private Long originalMessageId;
    private Message originalMessage;

    @BeforeEach
    void setUp() {
        senderId = 100L;
        chatRoomId = 1L;
        originalMessageId = 500L;

        messageReplyForwardService = new MessageReplyForwardService(
                messageRepository, chatRoomMemberRepository, idGenerator, chatMessageBroker,
                userRepository, userEventBroker, fileStorage, 10);

        // 첨부파일 URL은 기본적으로 그대로 통과시켜 기존 검증을 유지한다(실제로는 단기 Pre-signed URL로 재발급).
        lenient().when(fileStorage.presignAttachmentUrl(anyString(), anyInt()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(fileStorage.presignAttachmentUrl(eq(null), anyInt())).thenReturn(null);

        originalMessage = Message.builder()
                .id(originalMessageId)
                .chatRoomId(chatRoomId)
                .senderId(200L)
                .content("원본 메시지입니다.")
                .type(Message.MessageType.TEXT)
                .build();

        // broadcastMessage()에서 사용하는 공통 스텁 (에러 테스트에서는 불필요하므로 lenient)
        lenient().when(userRepository.findById(anyLong()))
                .thenReturn(Optional.of(User.builder().id(senderId).nickname("테스트유저").build()));
        lenient().when(chatRoomMemberRepository.findByChatRoomId(anyLong()))
                .thenReturn(List.of());
    }

    @Nested
    @DisplayName("메시지 답장")
    class ReplyMessage {

        @Test
        @DisplayName("메시지에 답장을 성공적으로 보낸다")
        void should_replyToMessage_when_validRequest() {
            // given
            String replyContent = "답장 내용입니다.";
            Long newMessageId = 600L;

            given(messageRepository.findById(originalMessageId)).willReturn(Optional.of(originalMessage));
            given(chatRoomMemberRepository.existsByChatRoomIdAndUserId(chatRoomId, senderId)).willReturn(true);
            given(idGenerator.nextId()).willReturn(newMessageId);
            given(messageRepository.save(any(Message.class))).willAnswer(invocation -> {
                Message msg = invocation.getArgument(0);
                ReflectionTestUtils.setField(msg, "createdAt", LocalDateTime.now());
                return msg;
            });

            // when
            Message replyMessage = messageReplyForwardService.replyToMessage(senderId, originalMessageId, replyContent);

            // then
            assertThat(replyMessage.getContent()).isEqualTo(replyContent);
            assertThat(replyMessage.getReplyToMessageId()).isEqualTo(originalMessageId);
            assertThat(replyMessage.getSenderId()).isEqualTo(senderId);
            assertThat(replyMessage.getChatRoomId()).isEqualTo(chatRoomId);
            verify(messageRepository).save(any(Message.class));
        }

        @Test
        @DisplayName("존재하지 않는 메시지에 답장 시 실패한다")
        void should_throwException_when_originalMessageNotFound() {
            // given
            given(messageRepository.findById(originalMessageId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    messageReplyForwardService.replyToMessage(senderId, originalMessageId, "답장"))
                    .isInstanceOf(MessageNotFoundException.class);
        }

        @Test
        @DisplayName("채팅방 멤버가 아닌 사용자가 답장 시 실패한다")
        void should_throwException_when_userNotInChatRoom() {
            // given
            given(messageRepository.findById(originalMessageId)).willReturn(Optional.of(originalMessage));
            given(chatRoomMemberRepository.existsByChatRoomIdAndUserId(chatRoomId, senderId)).willReturn(false);

            // when & then
            assertThatThrownBy(() ->
                    messageReplyForwardService.replyToMessage(senderId, originalMessageId, "답장"))
                    .isInstanceOf(ChatRoomAccessDeniedException.class);
        }
    }

    @Nested
    @DisplayName("메시지 전달")
    class ForwardMessage {

        @Test
        @DisplayName("메시지를 다른 채팅방으로 전달한다")
        void should_forwardMessage_when_validRequest() {
            // given
            Long targetChatRoomId = 2L;
            Long newMessageId = 700L;

            given(messageRepository.findById(originalMessageId)).willReturn(Optional.of(originalMessage));
            given(chatRoomMemberRepository.existsByChatRoomIdAndUserId(chatRoomId, senderId)).willReturn(true);
            given(chatRoomMemberRepository.existsByChatRoomIdAndUserId(targetChatRoomId, senderId)).willReturn(true);
            given(idGenerator.nextId()).willReturn(newMessageId);
            given(messageRepository.save(any(Message.class))).willAnswer(invocation -> {
                Message msg = invocation.getArgument(0);
                ReflectionTestUtils.setField(msg, "createdAt", LocalDateTime.now());
                return msg;
            });

            // when
            Message forwardedMessage = messageReplyForwardService.forwardMessage(senderId, originalMessageId, targetChatRoomId);

            // then
            assertThat(forwardedMessage.getContent()).isEqualTo(originalMessage.getContent());
            assertThat(forwardedMessage.getForwardedFromMessageId()).isEqualTo(originalMessageId);
            assertThat(forwardedMessage.getChatRoomId()).isEqualTo(targetChatRoomId);
            assertThat(forwardedMessage.getSenderId()).isEqualTo(senderId);
            verify(messageRepository).save(any(Message.class));
        }

        @Test
        @DisplayName("원본 채팅방 멤버가 아니면 전달 실패")
        void should_throwException_when_notMemberOfOriginalRoom() {
            // given
            Long targetChatRoomId = 2L;

            given(messageRepository.findById(originalMessageId)).willReturn(Optional.of(originalMessage));
            given(chatRoomMemberRepository.existsByChatRoomIdAndUserId(chatRoomId, senderId)).willReturn(false);

            // when & then
            assertThatThrownBy(() ->
                    messageReplyForwardService.forwardMessage(senderId, originalMessageId, targetChatRoomId))
                    .isInstanceOf(ChatRoomAccessDeniedException.class);
        }

        @Test
        @DisplayName("대상 채팅방 멤버가 아니면 전달 실패")
        void should_throwException_when_notMemberOfTargetRoom() {
            // given
            Long targetChatRoomId = 2L;

            given(messageRepository.findById(originalMessageId)).willReturn(Optional.of(originalMessage));
            given(chatRoomMemberRepository.existsByChatRoomIdAndUserId(chatRoomId, senderId)).willReturn(true);
            given(chatRoomMemberRepository.existsByChatRoomIdAndUserId(targetChatRoomId, senderId)).willReturn(false);

            // when & then
            assertThatThrownBy(() ->
                    messageReplyForwardService.forwardMessage(senderId, originalMessageId, targetChatRoomId))
                    .isInstanceOf(ChatRoomAccessDeniedException.class);
        }

        @Test
        @DisplayName("존재하지 않는 메시지를 전달 시 실패한다")
        void should_throwException_when_originalMessageNotFound() {
            // given
            Long targetChatRoomId = 2L;
            given(messageRepository.findById(originalMessageId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    messageReplyForwardService.forwardMessage(senderId, originalMessageId, targetChatRoomId))
                    .isInstanceOf(MessageNotFoundException.class);
        }

        @Test
        @DisplayName("이미지 메시지를 전달할 때 파일 정보가 복사된다")
        void should_forwardImageMessage_withFileInfo() {
            // given
            Long targetChatRoomId = 2L;
            Long newMessageId = 700L;

            Message imageMessage = Message.builder()
                    .id(originalMessageId)
                    .chatRoomId(chatRoomId)
                    .senderId(200L)
                    .content("이미지 설명")
                    .type(Message.MessageType.IMAGE)
                    .fileUrl("https://example.com/image.png")
                    .fileName("image.png")
                    .fileSize(1024L)
                    .fileContentType("image/png")
                    .thumbnailUrl("https://example.com/thumbnail.png")
                    .build();

            given(messageRepository.findById(originalMessageId)).willReturn(Optional.of(imageMessage));
            given(chatRoomMemberRepository.existsByChatRoomIdAndUserId(chatRoomId, senderId)).willReturn(true);
            given(chatRoomMemberRepository.existsByChatRoomIdAndUserId(targetChatRoomId, senderId)).willReturn(true);
            given(idGenerator.nextId()).willReturn(newMessageId);
            given(messageRepository.save(any(Message.class))).willAnswer(invocation -> {
                Message msg = invocation.getArgument(0);
                ReflectionTestUtils.setField(msg, "createdAt", LocalDateTime.now());
                return msg;
            });

            // when
            Message forwardedMessage = messageReplyForwardService.forwardMessage(senderId, originalMessageId, targetChatRoomId);

            // then
            assertThat(forwardedMessage.getType()).isEqualTo(Message.MessageType.IMAGE);
            assertThat(forwardedMessage.getFileUrl()).isEqualTo("https://example.com/image.png");
            assertThat(forwardedMessage.getFileName()).isEqualTo("image.png");
            assertThat(forwardedMessage.getFileSize()).isEqualTo(1024L);
            assertThat(forwardedMessage.getFileContentType()).isEqualTo("image/png");
            assertThat(forwardedMessage.getThumbnailUrl()).isEqualTo("https://example.com/thumbnail.png");
            assertThat(forwardedMessage.getForwardedFromMessageId()).isEqualTo(originalMessageId);
        }

        @Test
        @DisplayName("파일 메시지를 전달할 때 파일 정보가 복사된다")
        void should_forwardFileMessage_withFileInfo() {
            // given
            Long targetChatRoomId = 2L;
            Long newMessageId = 700L;

            Message fileMessage = Message.builder()
                    .id(originalMessageId)
                    .chatRoomId(chatRoomId)
                    .senderId(200L)
                    .content("파일 설명")
                    .type(Message.MessageType.FILE)
                    .fileUrl("https://example.com/file.pdf")
                    .fileName("document.pdf")
                    .fileSize(2048L)
                    .fileContentType("application/pdf")
                    .build();

            given(messageRepository.findById(originalMessageId)).willReturn(Optional.of(fileMessage));
            given(chatRoomMemberRepository.existsByChatRoomIdAndUserId(chatRoomId, senderId)).willReturn(true);
            given(chatRoomMemberRepository.existsByChatRoomIdAndUserId(targetChatRoomId, senderId)).willReturn(true);
            given(idGenerator.nextId()).willReturn(newMessageId);
            given(messageRepository.save(any(Message.class))).willAnswer(invocation -> {
                Message msg = invocation.getArgument(0);
                ReflectionTestUtils.setField(msg, "createdAt", LocalDateTime.now());
                return msg;
            });

            // when
            Message forwardedMessage = messageReplyForwardService.forwardMessage(senderId, originalMessageId, targetChatRoomId);

            // then
            assertThat(forwardedMessage.getType()).isEqualTo(Message.MessageType.FILE);
            assertThat(forwardedMessage.getFileUrl()).isEqualTo("https://example.com/file.pdf");
            assertThat(forwardedMessage.getFileName()).isEqualTo("document.pdf");
            assertThat(forwardedMessage.getFileSize()).isEqualTo(2048L);
            assertThat(forwardedMessage.getFileContentType()).isEqualTo("application/pdf");
            assertThat(forwardedMessage.getForwardedFromMessageId()).isEqualTo(originalMessageId);
        }

        @Test
        @DisplayName("이미지 메시지를 전달할 때 브로드캐스트 첨부파일 URL은 원본이 아닌 단기 Pre-signed URL이다 (H-1)")
        void should_broadcastPresignedAttachmentUrl_when_forwardImageMessage() {
            // given
            Long targetChatRoomId = 2L;
            Long newMessageId = 700L;
            String storedFileUrl = "https://minio.example.com/cotalk/uploads/1/image.png";
            String storedThumbnailUrl = "https://minio.example.com/cotalk/uploads/1/thumb.png";
            String presignedFileUrl = "https://minio.example.com/cotalk/uploads/1/image.png?X-Amz-Signature=file";
            String presignedThumbnailUrl = "https://minio.example.com/cotalk/uploads/1/thumb.png?X-Amz-Signature=thumb";

            Message imageMessage = Message.builder()
                    .id(originalMessageId)
                    .chatRoomId(chatRoomId)
                    .senderId(200L)
                    .content("이미지 설명")
                    .type(Message.MessageType.IMAGE)
                    .fileUrl(storedFileUrl)
                    .fileName("image.png")
                    .fileSize(1024L)
                    .fileContentType("image/png")
                    .thumbnailUrl(storedThumbnailUrl)
                    .build();

            given(messageRepository.findById(originalMessageId)).willReturn(Optional.of(imageMessage));
            given(chatRoomMemberRepository.existsByChatRoomIdAndUserId(chatRoomId, senderId)).willReturn(true);
            given(chatRoomMemberRepository.existsByChatRoomIdAndUserId(targetChatRoomId, senderId)).willReturn(true);
            given(idGenerator.nextId()).willReturn(newMessageId);
            given(messageRepository.save(any(Message.class))).willAnswer(invocation -> {
                Message msg = invocation.getArgument(0);
                ReflectionTestUtils.setField(msg, "createdAt", LocalDateTime.now());
                return msg;
            });
            given(fileStorage.presignAttachmentUrl(storedFileUrl, 10)).willReturn(presignedFileUrl);
            given(fileStorage.presignAttachmentUrl(storedThumbnailUrl, 10)).willReturn(presignedThumbnailUrl);

            // when
            messageReplyForwardService.forwardMessage(senderId, originalMessageId, targetChatRoomId);

            // then: 브로드캐스트된 첨부파일 URL은 저장 원본이 아니라 단기 Pre-signed URL이어야 한다
            ArgumentCaptor<ChatMessageBroker.ChatBroadcastMessage> broadcastCaptor =
                    ArgumentCaptor.forClass(ChatMessageBroker.ChatBroadcastMessage.class);
            verify(chatMessageBroker, times(1)).publish(eq(targetChatRoomId), broadcastCaptor.capture());

            ChatMessageBroker.ChatBroadcastMessage broadcast = broadcastCaptor.getValue();
            assertThat(broadcast.fileUrl()).isEqualTo(presignedFileUrl);
            assertThat(broadcast.thumbnailUrl()).isEqualTo(presignedThumbnailUrl);
            assertThat(broadcast.fileUrl()).isNotEqualTo(storedFileUrl);
        }
    }
}
