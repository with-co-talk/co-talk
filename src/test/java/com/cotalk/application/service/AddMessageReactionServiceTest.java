package com.cotalk.application.service;

import com.cotalk.domain.entity.Message;
import com.cotalk.domain.entity.MessageReaction;
import com.cotalk.domain.exception.InvalidEmojiException;
import com.cotalk.domain.exception.MessageNotFoundException;
import com.cotalk.domain.port.outbound.MessageReactionRepository;
import com.cotalk.domain.port.outbound.MessageRepository;
import com.cotalk.domain.validator.MessageValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class AddMessageReactionServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private MessageReactionRepository reactionRepository;

    private MessageValidator messageValidator;

    private AddMessageReactionService service;

    @BeforeEach
    void setUp() {
        messageValidator = new MessageValidator();
        service = new AddMessageReactionService(messageRepository, reactionRepository, messageValidator);
    }

    @Test
    @DisplayName("메시지에 반응 추가 성공")
    void should_addReaction_when_validRequest() {
        // given
        Long messageId = 100L;
        Long userId = 1L;
        String emoji = "👍";

        Message message = Message.builder()
                .id(messageId)
                .chatRoomId(10L)
                .senderId(2L)
                .content("테스트 메시지")
                .type(Message.MessageType.TEXT)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        MessageReaction reaction = MessageReaction.create(messageId, userId, emoji);

        given(messageRepository.findById(messageId)).willReturn(Optional.of(message));
        given(reactionRepository.findByMessageIdAndUserIdAndEmoji(messageId, userId, emoji))
                .willReturn(Optional.empty());
        given(reactionRepository.save(any(MessageReaction.class))).willReturn(reaction);

        // when
        MessageReaction result = service.addReaction(messageId, userId, emoji);

        // then
        assertThat(result.getEmoji()).isEqualTo(emoji);
        assertThat(result.getMessageId()).isEqualTo(messageId);
        assertThat(result.getUserId()).isEqualTo(userId);
        verify(reactionRepository).save(any(MessageReaction.class));
    }

    @Test
    @DisplayName("이미 같은 반응이 있으면 기존 반응 반환")
    void should_returnExistingReaction_when_alreadyExists() {
        // given
        Long messageId = 100L;
        Long userId = 1L;
        String emoji = "👍";

        Message message = Message.builder()
                .id(messageId)
                .chatRoomId(10L)
                .senderId(2L)
                .content("테스트 메시지")
                .type(Message.MessageType.TEXT)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        MessageReaction existingReaction = MessageReaction.builder()
                .id(1L)
                .messageId(messageId)
                .userId(userId)
                .emoji(emoji)
                .createdAt(LocalDateTime.now())
                .build();

        given(messageRepository.findById(messageId)).willReturn(Optional.of(message));
        given(reactionRepository.findByMessageIdAndUserIdAndEmoji(messageId, userId, emoji))
                .willReturn(Optional.of(existingReaction));

        // when
        MessageReaction result = service.addReaction(messageId, userId, emoji);

        // then
        assertThat(result).isEqualTo(existingReaction);
        verify(reactionRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 메시지에 반응 추가 시 예외")
    void should_throwException_when_messageNotFound() {
        // given
        Long messageId = 999L;
        given(messageRepository.findById(messageId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.addReaction(messageId, 1L, "👍"))
                .isInstanceOf(MessageNotFoundException.class);
    }

    @Test
    @DisplayName("이모지가 너무 길면 예외")
    void should_throwException_when_emojiTooLong() {
        // given
        Long messageId = 100L;
        String tooLongEmoji = "a".repeat(51); // 51자

        Message message = Message.builder()
                .id(messageId)
                .chatRoomId(10L)
                .senderId(2L)
                .content("테스트 메시지")
                .type(Message.MessageType.TEXT)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        given(messageRepository.findById(messageId)).willReturn(Optional.of(message));

        // when & then
        assertThatThrownBy(() -> service.addReaction(messageId, 1L, tooLongEmoji))
                .isInstanceOf(InvalidEmojiException.class)
                .hasMessageContaining("50자 이하여야 합니다");
    }

    @Test
    @DisplayName("빈 이모지 추가 시 예외")
    void should_throwException_when_emojiEmpty() {
        // given
        Long messageId = 100L;
        Message message = Message.builder()
                .id(messageId)
                .chatRoomId(10L)
                .senderId(2L)
                .content("테스트 메시지")
                .type(Message.MessageType.TEXT)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        given(messageRepository.findById(messageId)).willReturn(Optional.of(message));

        // when & then
        assertThatThrownBy(() -> service.addReaction(messageId, 1L, ""))
                .isInstanceOf(InvalidEmojiException.class);
    }
}
