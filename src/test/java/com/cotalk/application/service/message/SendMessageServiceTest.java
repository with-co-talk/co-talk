package com.cotalk.application.service.message;

import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.entity.Message;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.ChatRoomAccessDeniedException;
import com.cotalk.domain.port.inbound.notification.SendPushNotificationUseCase;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.MessageRepository;
import com.cotalk.domain.port.outbound.UserRepository;
import com.cotalk.domain.validator.ChatRoomMemberValidator;
import com.cotalk.infrastructure.id.SnowflakeIdGenerator;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

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
    private SnowflakeIdGenerator idGenerator;

    @Mock
    private SendPushNotificationUseCase sendPushNotificationUseCase;

    private ChatRoomMemberValidator chatRoomMemberValidator;

    private SendMessageService sendMessageService;

    @BeforeEach
    void setUp() {
        chatRoomMemberValidator = new ChatRoomMemberValidator(chatRoomMemberRepository);
        sendMessageService = new SendMessageService(
                messageRepository, chatRoomMemberRepository, userRepository, idGenerator, sendPushNotificationUseCase, chatRoomMemberValidator);
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

            // 푸시 알림이 수신자에게 전송되었는지 검증
            verify(sendPushNotificationUseCase).sendNewMessageNotification(receiverId, "발신자", content, chatRoomId);
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
}
