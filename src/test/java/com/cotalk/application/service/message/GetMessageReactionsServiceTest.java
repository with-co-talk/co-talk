package com.cotalk.application.service.message;

import com.cotalk.adapter.inbound.rest.dto.message.GroupedReactionResponse;
import com.cotalk.domain.entity.Emoji;
import com.cotalk.domain.entity.MessageReaction;
import com.cotalk.domain.port.outbound.MessageReactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetMessageReactionsService")
class GetMessageReactionsServiceTest {

    @Mock
    private MessageReactionRepository reactionRepository;

    private GetMessageReactionsService service;

    @BeforeEach
    void setUp() {
        service = new GetMessageReactionsService(reactionRepository);
    }

    @Nested
    @DisplayName("반응 목록 조회")
    class GetReactions {

        @Test
        @DisplayName("메시지의 모든 반응을 조회한다")
        void should_ReturnAllReactions_when_ReactionsExist() {
            // given
            Long messageId = 1L;
            List<MessageReaction> reactions = List.of(
                    MessageReaction.builder().id(1L).messageId(messageId).userId(10L).emoji(Emoji.HEART).build(),
                    MessageReaction.builder().id(2L).messageId(messageId).userId(20L).emoji(Emoji.THUMBS_UP).build()
            );

            given(reactionRepository.findByMessageId(messageId)).willReturn(reactions);

            // when
            List<MessageReaction> result = service.getReactions(messageId);

            // then
            assertThat(result).hasSize(2);
            assertThat(result).extracting(MessageReaction::getEmoji)
                    .containsExactly(Emoji.HEART, Emoji.THUMBS_UP);
        }

        @Test
        @DisplayName("반응이 없으면 빈 목록을 반환한다")
        void should_ReturnEmptyList_when_NoReactions() {
            // given
            Long messageId = 1L;
            given(reactionRepository.findByMessageId(messageId)).willReturn(List.of());

            // when
            List<MessageReaction> result = service.getReactions(messageId);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("그룹핑된 반응 조회")
    class GetGroupedReactions {

        @Test
        @DisplayName("같은 이모지는 그룹핑된다")
        void should_GroupByEmoji_when_SameEmojiUsed() {
            // given
            Long messageId = 1L;
            Long currentUserId = 10L;
            List<MessageReaction> reactions = List.of(
                    MessageReaction.builder().id(1L).messageId(messageId).userId(10L).emoji(Emoji.HEART).build(),
                    MessageReaction.builder().id(2L).messageId(messageId).userId(20L).emoji(Emoji.HEART).build(),
                    MessageReaction.builder().id(3L).messageId(messageId).userId(30L).emoji(Emoji.THUMBS_UP).build()
            );

            given(reactionRepository.findByMessageId(messageId)).willReturn(reactions);

            // when
            List<GroupedReactionResponse> result = service.getGroupedReactions(messageId, currentUserId);

            // then
            assertThat(result).hasSize(2);

            GroupedReactionResponse heartResponse = result.stream()
                    .filter(r -> r.emoji().equals("❤️"))
                    .findFirst()
                    .orElseThrow();
            assertThat(heartResponse.count()).isEqualTo(2);
            assertThat(heartResponse.userIds()).containsExactlyInAnyOrder(10L, 20L);
            assertThat(heartResponse.currentUserReacted()).isTrue();

            GroupedReactionResponse thumbsUpResponse = result.stream()
                    .filter(r -> r.emoji().equals("👍"))
                    .findFirst()
                    .orElseThrow();
            assertThat(thumbsUpResponse.count()).isEqualTo(1);
            assertThat(thumbsUpResponse.currentUserReacted()).isFalse();
        }

        @Test
        @DisplayName("반응 수가 많은 순으로 정렬된다")
        void should_SortByCountDescending_when_MultipleEmojis() {
            // given
            Long messageId = 1L;
            List<MessageReaction> reactions = List.of(
                    MessageReaction.builder().id(1L).messageId(messageId).userId(10L).emoji(Emoji.THUMBS_UP).build(),
                    MessageReaction.builder().id(2L).messageId(messageId).userId(20L).emoji(Emoji.HEART).build(),
                    MessageReaction.builder().id(3L).messageId(messageId).userId(30L).emoji(Emoji.HEART).build(),
                    MessageReaction.builder().id(4L).messageId(messageId).userId(40L).emoji(Emoji.HEART).build(),
                    MessageReaction.builder().id(5L).messageId(messageId).userId(50L).emoji(Emoji.FIRE).build(),
                    MessageReaction.builder().id(6L).messageId(messageId).userId(60L).emoji(Emoji.FIRE).build()
            );

            given(reactionRepository.findByMessageId(messageId)).willReturn(reactions);

            // when
            List<GroupedReactionResponse> result = service.getGroupedReactions(messageId, null);

            // then
            assertThat(result).hasSize(3);
            assertThat(result.get(0).emoji()).isEqualTo("❤️");
            assertThat(result.get(0).count()).isEqualTo(3);
            assertThat(result.get(1).emoji()).isEqualTo("🔥");
            assertThat(result.get(1).count()).isEqualTo(2);
            assertThat(result.get(2).emoji()).isEqualTo("👍");
            assertThat(result.get(2).count()).isEqualTo(1);
        }

        @Test
        @DisplayName("현재 사용자가 null이면 currentUserReacted는 false")
        void should_ReturnFalseForCurrentUser_when_CurrentUserIsNull() {
            // given
            Long messageId = 1L;
            List<MessageReaction> reactions = List.of(
                    MessageReaction.builder().id(1L).messageId(messageId).userId(10L).emoji(Emoji.HEART).build()
            );

            given(reactionRepository.findByMessageId(messageId)).willReturn(reactions);

            // when
            List<GroupedReactionResponse> result = service.getGroupedReactions(messageId, null);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).currentUserReacted()).isFalse();
        }

        @Test
        @DisplayName("반응이 없으면 빈 목록을 반환한다")
        void should_ReturnEmptyList_when_NoReactionsForGrouped() {
            // given
            Long messageId = 1L;
            given(reactionRepository.findByMessageId(messageId)).willReturn(List.of());

            // when
            List<GroupedReactionResponse> result = service.getGroupedReactions(messageId, 1L);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("이모지 정보가 올바르게 포함된다")
        void should_IncludeEmojiInfo_when_GroupingReactions() {
            // given
            Long messageId = 1L;
            List<MessageReaction> reactions = List.of(
                    MessageReaction.builder().id(1L).messageId(messageId).userId(10L).emoji(Emoji.PARTY).build()
            );

            given(reactionRepository.findByMessageId(messageId)).willReturn(reactions);

            // when
            List<GroupedReactionResponse> result = service.getGroupedReactions(messageId, null);

            // then
            assertThat(result).hasSize(1);
            GroupedReactionResponse response = result.get(0);
            assertThat(response.emoji()).isEqualTo("🎉");
            assertThat(response.emojiCharacter()).isEqualTo("🎉");
            assertThat(response.emojiName()).isEqualTo("party");
        }
    }
}
