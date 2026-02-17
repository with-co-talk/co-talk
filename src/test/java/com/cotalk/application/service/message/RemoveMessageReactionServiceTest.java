package com.cotalk.application.service.message;

import com.cotalk.domain.entity.Emoji;
import com.cotalk.domain.entity.Message;
import com.cotalk.domain.entity.MessageReaction;
import com.cotalk.domain.exception.ChatRoomAccessDeniedException;
import com.cotalk.domain.exception.MessageReactionNotFoundException;
import com.cotalk.domain.port.outbound.MessageReactionRepository;
import com.cotalk.domain.port.outbound.MessageRepository;
import com.cotalk.domain.validator.ChatRoomMemberValidator;
import com.cotalk.domain.validator.MessageValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RemoveMessageReactionServiceTest {

    @Mock
    private MessageReactionRepository reactionRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private MessageValidator messageValidator;

    @Mock
    private ChatRoomMemberValidator chatRoomMemberValidator;

    private RemoveMessageReactionService service;

    @BeforeEach
    void setUp() {
        service = new RemoveMessageReactionService(reactionRepository, messageRepository, messageValidator, chatRoomMemberValidator);
    }

    @Test
    @DisplayName("메시지 반응 제거 성공")
    void should_removeReaction_when_validRequest() {
        // given
        Long messageId = 100L;
        Long userId = 1L;
        String emojiString = "👍";
        Emoji emoji = Emoji.THUMBS_UP;

        MessageReaction reaction = MessageReaction.builder()
                .id(1L)
                .messageId(messageId)
                .userId(userId)
                .emoji(emoji)
                .build();

        given(messageValidator.validateAndParseEmoji(emojiString)).willReturn(emoji);
        given(reactionRepository.findByMessageIdAndUserIdAndEmoji(messageId, userId, emoji))
                .willReturn(Optional.of(reaction));

        // when
        service.removeReaction(messageId, userId, emojiString);

        // then
        verify(reactionRepository).delete(reaction);
    }

    @Test
    @DisplayName("존재하지 않는 반응 제거 시 예외")
    void should_throwException_when_reactionNotFound() {
        // given
        Long messageId = 100L;
        Long userId = 1L;
        String emojiString = "👍";
        Emoji emoji = Emoji.THUMBS_UP;

        given(messageValidator.validateAndParseEmoji(emojiString)).willReturn(emoji);
        given(reactionRepository.findByMessageIdAndUserIdAndEmoji(messageId, userId, emoji))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.removeReaction(messageId, userId, emojiString))
                .isInstanceOf(MessageReactionNotFoundException.class);
    }

    @Test
    @DisplayName("채팅방 멤버가 아닌 사용자의 반응 제거는 거부된다")
    void should_rejectReactionRemoval_when_userNotMember() {
        // given
        Long messageId = 100L;
        Long chatRoomId = 10L;
        Long unauthorizedUserId = 999L;
        String emojiString = "👍";

        Message message = Message.builder()
                .id(messageId)
                .chatRoomId(chatRoomId)
                .senderId(2L)
                .content("테스트 메시지")
                .type(Message.MessageType.TEXT)
                .build();

        given(messageRepository.findById(messageId)).willReturn(Optional.of(message));
        org.mockito.Mockito.doThrow(new ChatRoomAccessDeniedException(chatRoomId, unauthorizedUserId))
                .when(chatRoomMemberValidator).validateMembership(chatRoomId, unauthorizedUserId);

        // when & then
        assertThatThrownBy(() -> service.removeReactionWithContext(messageId, unauthorizedUserId, emojiString))
                .isInstanceOf(ChatRoomAccessDeniedException.class);

        verify(reactionRepository, org.mockito.Mockito.never()).delete(org.mockito.ArgumentMatchers.any());
    }
}
