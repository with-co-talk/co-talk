package com.cotalk.application.service.chat;

import com.cotalk.adapter.inbound.websocket.dto.ReactionBroadcastMessage;
import com.cotalk.domain.entity.MessageReaction;
import com.cotalk.domain.port.inbound.chat.BroadcastReactionEventUseCase;
import com.cotalk.domain.port.outbound.ChatMessageBroker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;

/**
 * 메시지 반응 이벤트 브로드캐스트 유스케이스 구현체.
 * 반응 추가/제거 이벤트를 Redis Pub/Sub을 통해 채팅방 참여자들에게 브로드캐스트한다.
 *
 * @author seunggu.lee
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BroadcastReactionEventService implements BroadcastReactionEventUseCase {

    private final ChatMessageBroker chatMessageBroker;

    /**
     * {@inheritDoc}
     */
    @Override
    public void broadcastReactionEvent(MessageReaction reaction, Long chatRoomId, String eventType) {
        ReactionBroadcastMessage broadcastMessage = new ReactionBroadcastMessage(
                1,
                "reaction:" + reaction.getMessageId() + ":" + reaction.getUserId() + ":" + eventType,
                reaction.getId(),
                reaction.getMessageId(),
                reaction.getUserId(),
                reaction.getEmoji().getCharacter(), // 유니코드 이모지 문자 전송
                eventType,
                reaction.getCreatedAt() != null
                        ? reaction.getCreatedAt().atZone(ZoneOffset.UTC).toInstant().toEpochMilli()
                        : System.currentTimeMillis()
        );

        // 메시지가 속한 채팅방으로 브로드캐스트
        chatMessageBroker.publishReaction(chatRoomId, broadcastMessage);
    }
}
