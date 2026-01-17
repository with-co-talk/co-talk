package com.cotalk.application.service.message;

import com.cotalk.domain.entity.Message;
import com.cotalk.domain.exception.ChatRoomAccessDeniedException;
import com.cotalk.domain.exception.MessageNotFoundException;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.MessageRepository;
import com.cotalk.infrastructure.id.SnowflakeIdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MessageReplyForwardServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Mock
    private SnowflakeIdGenerator idGenerator;

    @InjectMocks
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

        originalMessage = Message.builder()
                .id(originalMessageId)
                .chatRoomId(chatRoomId)
                .senderId(200L)
                .content("원본 메시지입니다.")
                .type(Message.MessageType.TEXT)
                .build();
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
            given(messageRepository.save(any(Message.class))).willAnswer(invocation -> invocation.getArgument(0));

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
            given(messageRepository.save(any(Message.class))).willAnswer(invocation -> invocation.getArgument(0));

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
    }
}
