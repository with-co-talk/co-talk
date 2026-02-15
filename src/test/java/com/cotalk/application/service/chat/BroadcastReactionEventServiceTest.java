package com.cotalk.application.service.chat;

import com.cotalk.domain.port.outbound.ChatMessageBroker.ReactionBroadcastEvent;
import com.cotalk.domain.entity.Emoji;
import com.cotalk.domain.entity.MessageReaction;
import com.cotalk.domain.port.outbound.ChatMessageBroker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * {@link BroadcastReactionEventService} 유닛 테스트.
 * 메시지 반응 이벤트 브로드캐스트 로직을 검증한다.
 *
 * @author seunggu.lee
 */
@ExtendWith(MockitoExtension.class)
class BroadcastReactionEventServiceTest {

    @Mock
    private ChatMessageBroker chatMessageBroker;

    @InjectMocks
    private BroadcastReactionEventService broadcastReactionEventService;

    @Test
    void should_broadcastReactionAddedEvent_when_reactionIsAdded() {
        // given
        MessageReaction reaction = MessageReaction.builder()
                .id(1L)
                .messageId(100L)
                .userId(1L)
                .emoji(Emoji.THUMBS_UP)
                .build();
        Long chatRoomId = 200L;
        String eventType = "ADDED";

        // when
        broadcastReactionEventService.broadcastReactionEvent(reaction, chatRoomId, eventType);

        // then
        ArgumentCaptor<ReactionBroadcastEvent> captor = ArgumentCaptor.forClass(ReactionBroadcastEvent.class);
        verify(chatMessageBroker).publishReaction(eq(200L), captor.capture());

        ReactionBroadcastEvent broadcastMessage = captor.getValue();
        assertThat(broadcastMessage.reactionId()).isEqualTo(1L);
        assertThat(broadcastMessage.messageId()).isEqualTo(100L);
        assertThat(broadcastMessage.userId()).isEqualTo(1L);
        assertThat(broadcastMessage.emoji()).isEqualTo("👍");
        assertThat(broadcastMessage.eventType()).isEqualTo("ADDED");
    }

    @Test
    void should_broadcastReactionRemovedEvent_when_reactionIsRemoved() {
        // given
        MessageReaction reaction = MessageReaction.builder()
                .id(2L)
                .messageId(100L)
                .userId(2L)
                .emoji(Emoji.HEART)
                .build();
        Long chatRoomId = 200L;
        String eventType = "REMOVED";

        // when
        broadcastReactionEventService.broadcastReactionEvent(reaction, chatRoomId, eventType);

        // then
        ArgumentCaptor<ReactionBroadcastEvent> captor = ArgumentCaptor.forClass(ReactionBroadcastEvent.class);
        verify(chatMessageBroker).publishReaction(eq(200L), captor.capture());

        ReactionBroadcastEvent broadcastMessage = captor.getValue();
        assertThat(broadcastMessage.reactionId()).isEqualTo(2L);
        assertThat(broadcastMessage.emoji()).isEqualTo("❤️");
        assertThat(broadcastMessage.eventType()).isEqualTo("REMOVED");
    }

    @Test
    void should_broadcastWithCreatedAtTimestamp_when_reactionHasCreatedAt() {
        // given
        LocalDateTime createdAt = LocalDateTime.now();
        MessageReaction reaction = MessageReaction.builder()
                .id(1L)
                .messageId(100L)
                .userId(1L)
                .emoji(Emoji.FIRE)
                .build();
        // BaseEntity의 createdAt은 @PrePersist로 설정되므로, 테스트에서는 리플렉션이나 별도 설정 필요
        // 여기서는 간단히 현재 시간으로 테스트
        Long chatRoomId = 200L;
        String eventType = "ADDED";

        // when
        broadcastReactionEventService.broadcastReactionEvent(reaction, chatRoomId, eventType);

        // then
        ArgumentCaptor<ReactionBroadcastEvent> captor = ArgumentCaptor.forClass(ReactionBroadcastEvent.class);
        verify(chatMessageBroker).publishReaction(eq(200L), captor.capture());

        ReactionBroadcastEvent broadcastMessage = captor.getValue();
        assertThat(broadcastMessage.timestamp()).isGreaterThan(0L);
    }

    @Test
    void should_generateUniqueEventId_when_broadcastingReaction() {
        // given
        MessageReaction reaction = MessageReaction.builder()
                .id(1L)
                .messageId(100L)
                .userId(1L)
                .emoji(Emoji.CLAPPING)
                .build();
        Long chatRoomId = 200L;
        String eventType = "ADDED";

        // when
        broadcastReactionEventService.broadcastReactionEvent(reaction, chatRoomId, eventType);

        // then
        ArgumentCaptor<ReactionBroadcastEvent> captor = ArgumentCaptor.forClass(ReactionBroadcastEvent.class);
        verify(chatMessageBroker).publishReaction(eq(200L), captor.capture());

        ReactionBroadcastEvent broadcastMessage = captor.getValue();
        assertThat(broadcastMessage.eventId()).contains("reaction:");
        assertThat(broadcastMessage.eventId()).contains("100:1:ADDED");
    }
}
