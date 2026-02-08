package com.cotalk.domain.port.inbound.chat;

import com.cotalk.domain.entity.MessageReaction;

/**
 * 메시지 반응 이벤트 브로드캐스트 유스케이스.
 * 반응 추가/제거 이벤트를 Redis Pub/Sub을 통해 채팅방 참여자들에게 브로드캐스트한다.
 *
 * <p>WebSocket 컨트롤러가 아웃바운드 포트({@code ChatMessageBroker})에
 * 직접 의존하지 않도록 인바운드 포트로 캡슐화한다.</p>
 *
 * @author seunggu.lee
 */
public interface BroadcastReactionEventUseCase {

    /**
     * 반응 이벤트를 채팅방 참여자들에게 브로드캐스트한다.
     *
     * @param reaction   반응 정보
     * @param chatRoomId 채팅방 ID
     * @param eventType  이벤트 타입 ("ADDED" 또는 "REMOVED")
     */
    void broadcastReactionEvent(MessageReaction reaction, Long chatRoomId, String eventType);
}
