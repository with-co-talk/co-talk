package com.cotalk.application.service.message;

import com.cotalk.domain.entity.Emoji;
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
import org.springframework.dao.DataIntegrityViolationException;

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

    @Mock
    private MessageValidator messageValidator;

    private AddMessageReactionService service;

    @BeforeEach
    void setUp() {
        service = new AddMessageReactionService(messageRepository, reactionRepository, messageValidator);
    }

    @Test
    @DisplayName("메시지에 반응 추가 성공")
    void should_addReaction_when_validRequest() {
        // given
        Long messageId = 100L;
        Long userId = 1L;
        String emojiString = "👍";
        Emoji emoji = Emoji.THUMBS_UP;

        Message message = Message.builder()
                .id(messageId)
                .chatRoomId(10L)
                .senderId(2L)
                .content("테스트 메시지")
                .type(Message.MessageType.TEXT)
                .build();

        MessageReaction reaction = MessageReaction.create(messageId, userId, emoji);

        given(messageRepository.findById(messageId)).willReturn(Optional.of(message));
        given(messageValidator.validateAndParseEmoji(emojiString)).willReturn(emoji);
        given(reactionRepository.findByMessageIdAndUserIdAndEmoji(messageId, userId, emoji))
                .willReturn(Optional.empty());
        given(reactionRepository.save(any(MessageReaction.class))).willReturn(reaction);

        // when
        MessageReaction result = service.addReaction(messageId, userId, emojiString);

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
        String emojiString = "👍";
        Emoji emoji = Emoji.THUMBS_UP;

        Message message = Message.builder()
                .id(messageId)
                .chatRoomId(10L)
                .senderId(2L)
                .content("테스트 메시지")
                .type(Message.MessageType.TEXT)
                .build();

        MessageReaction existingReaction = MessageReaction.builder()
                .id(1L)
                .messageId(messageId)
                .userId(userId)
                .emoji(emoji)
                .build();

        given(messageRepository.findById(messageId)).willReturn(Optional.of(message));
        given(messageValidator.validateAndParseEmoji(emojiString)).willReturn(emoji);
        given(reactionRepository.findByMessageIdAndUserIdAndEmoji(messageId, userId, emoji))
                .willReturn(Optional.of(existingReaction));

        // when
        MessageReaction result = service.addReaction(messageId, userId, emojiString);

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
    @DisplayName("유효하지 않은 이모지 형식이면 예외")
    void should_throwException_when_invalidEmoji() {
        // given
        Long messageId = 100L;
        String invalidEmoji = "invalid-emoji";

        Message message = Message.builder()
                .id(messageId)
                .chatRoomId(10L)
                .senderId(2L)
                .content("테스트 메시지")
                .type(Message.MessageType.TEXT)
                .build();

        given(messageRepository.findById(messageId)).willReturn(Optional.of(message));
        given(messageValidator.validateAndParseEmoji(invalidEmoji))
                .willThrow(InvalidEmojiException.invalidFormat(invalidEmoji));

        // when & then
        assertThatThrownBy(() -> service.addReaction(messageId, 1L, invalidEmoji))
                .isInstanceOf(InvalidEmojiException.class);
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
                .build();

        given(messageRepository.findById(messageId)).willReturn(Optional.of(message));
        given(messageValidator.validateAndParseEmoji(""))
                .willThrow(InvalidEmojiException.invalidFormat(""));

        // when & then
        assertThatThrownBy(() -> service.addReaction(messageId, 1L, ""))
                .isInstanceOf(InvalidEmojiException.class);
    }

    @Test
    @DisplayName("동시성으로 인한 중복 반응 추가 시 기존 반응 반환")
    void should_returnExistingReaction_when_concurrentDuplicateReaction() {
        // given
        Long messageId = 100L;
        Long userId = 1L;
        String emojiString = "👍";
        Emoji emoji = Emoji.THUMBS_UP;

        Message message = Message.builder()
                .id(messageId)
                .chatRoomId(10L)
                .senderId(2L)
                .content("테스트 메시지")
                .type(Message.MessageType.TEXT)
                .build();

        MessageReaction existingReaction = MessageReaction.builder()
                .id(1L)
                .messageId(messageId)
                .userId(userId)
                .emoji(emoji)
                .build();

        given(messageRepository.findById(messageId)).willReturn(Optional.of(message));
        given(messageValidator.validateAndParseEmoji(emojiString)).willReturn(emoji);
        given(reactionRepository.findByMessageIdAndUserIdAndEmoji(messageId, userId, emoji))
                .willReturn(Optional.empty()) // 첫 번째 조회: 없음
                .willReturn(Optional.of(existingReaction)); // 예외 후 조회: 있음
        given(reactionRepository.save(any(MessageReaction.class)))
                .willThrow(new DataIntegrityViolationException("Duplicate key"));

        // when
        MessageReaction result = service.addReaction(messageId, userId, emojiString);

        // then
        assertThat(result).isEqualTo(existingReaction);
        verify(reactionRepository).save(any(MessageReaction.class));
    }

    @Test
    @DisplayName("DataIntegrityViolationException 후 반응을 찾을 수 없으면 IllegalStateException 발생")
    void should_throwIllegalStateException_when_reactionNotFoundAfterDataIntegrityViolation() {
        // given
        Long messageId = 100L;
        Long userId = 1L;
        String emojiString = "👍";
        Emoji emoji = Emoji.THUMBS_UP;

        Message message = Message.builder()
                .id(messageId)
                .chatRoomId(10L)
                .senderId(2L)
                .content("테스트 메시지")
                .type(Message.MessageType.TEXT)
                .build();

        given(messageRepository.findById(messageId)).willReturn(Optional.of(message));
        given(messageValidator.validateAndParseEmoji(emojiString)).willReturn(emoji);
        given(reactionRepository.findByMessageIdAndUserIdAndEmoji(messageId, userId, emoji))
                .willReturn(Optional.empty()) // 첫 번째 조회: 없음
                .willReturn(Optional.empty()); // 예외 후 조회: 여전히 없음 (비정상 상황)
        given(reactionRepository.save(any(MessageReaction.class)))
                .willThrow(new DataIntegrityViolationException("Duplicate key"));

        // when & then
        assertThatThrownBy(() -> service.addReaction(messageId, userId, emojiString))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Reaction should exist after DataIntegrityViolationException");
    }
}
