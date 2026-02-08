package com.cotalk.domain.port.inbound.message;

import com.cotalk.domain.entity.MessageReaction;

/**
 * 메시지 반응 추가 유스케이스.
 * 메시지에 이모지 반응을 추가한다.
 *
 * @author seunggu.lee
 */
public interface AddMessageReactionUseCase {

    /**
     * 메시지에 반응을 추가한다.
     *
     * @param messageId 메시지 ID
     * @param userId 사용자 ID
     * @param emoji 이모지
     * @return 추가된 반응
     */
    MessageReaction addReaction(Long messageId, Long userId, String emoji);

    /**
     * 메시지에 반응을 추가하고, 브로드캐스트에 필요한 chatRoomId를 함께 반환한다.
     * WebSocket 컨트롤러에서 추가 DB 쿼리 없이 반응 이벤트를 발행할 수 있도록 한다.
     *
     * @param messageId 메시지 ID
     * @param userId    사용자 ID
     * @param emoji     이모지
     * @return 반응 결과 (반응 + 채팅방 ID)
     */
    ReactionResult addReactionWithContext(Long messageId, Long userId, String emoji);

    /**
     * 반응 추가 결과. 반응과 함께 브로드캐스트에 필요한 채팅방 ID를 포함한다.
     *
     * @param reaction   추가된 반응
     * @param chatRoomId 메시지가 속한 채팅방 ID
     */
    record ReactionResult(
            MessageReaction reaction,
            Long chatRoomId
    ) {}
}
