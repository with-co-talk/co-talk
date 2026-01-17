package com.cotalk.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MessageReaction 엔티티")
class MessageReactionTest {

    @Nested
    @DisplayName("create 메서드")
    class Create {

        @Test
        @DisplayName("메시지 ID, 사용자 ID, 이모지로 반응을 생성할 수 있다")
        void should_createReaction_when_validInputsProvided() {
            // given
            Long messageId = 1L;
            Long userId = 2L;
            Emoji emoji = Emoji.THUMBS_UP;

            // when
            MessageReaction reaction = MessageReaction.create(messageId, userId, emoji);

            // then
            assertThat(reaction.getMessageId()).isEqualTo(messageId);
            assertThat(reaction.getUserId()).isEqualTo(userId);
            assertThat(reaction.getEmoji()).isEqualTo(emoji);
        }

        @Test
        @DisplayName("다양한 이모지로 반응을 생성할 수 있다")
        void should_createReaction_with_differentEmojis() {
            // given
            Long messageId = 1L;
            Long userId = 2L;

            // when & then
            for (Emoji emoji : Emoji.values()) {
                MessageReaction reaction = MessageReaction.create(messageId, userId, emoji);
                assertThat(reaction.getEmoji()).isEqualTo(emoji);
            }
        }
    }

    @Nested
    @DisplayName("isFromUser 메서드")
    class IsFromUser {

        @Test
        @DisplayName("해당 사용자가 남긴 반응이면 true를 반환한다")
        void should_returnTrue_when_reactionFromUser() {
            // given
            Long messageId = 1L;
            Long userId = 2L;
            MessageReaction reaction = MessageReaction.create(messageId, userId, Emoji.HEART);

            // when
            boolean result = reaction.isFromUser(userId);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("다른 사용자가 남긴 반응이면 false를 반환한다")
        void should_returnFalse_when_reactionFromDifferentUser() {
            // given
            Long messageId = 1L;
            Long userId = 2L;
            Long otherUserId = 3L;
            MessageReaction reaction = MessageReaction.create(messageId, userId, Emoji.HEART);

            // when
            boolean result = reaction.isFromUser(otherUserId);

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("빌더")
    class Builder {

        @Test
        @DisplayName("빌더를 통해 모든 필드를 설정할 수 있다")
        void should_setAllFields_when_usingBuilder() {
            // given
            Long id = 1L;
            Long messageId = 2L;
            Long userId = 3L;
            Emoji emoji = Emoji.FIRE;

            // when
            MessageReaction reaction = MessageReaction.builder()
                    .id(id)
                    .messageId(messageId)
                    .userId(userId)
                    .emoji(emoji)
                    .build();

            // then
            assertThat(reaction.getId()).isEqualTo(id);
            assertThat(reaction.getMessageId()).isEqualTo(messageId);
            assertThat(reaction.getUserId()).isEqualTo(userId);
            assertThat(reaction.getEmoji()).isEqualTo(emoji);
        }
    }
}
