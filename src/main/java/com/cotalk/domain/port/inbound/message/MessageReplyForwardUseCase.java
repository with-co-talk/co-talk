package com.cotalk.domain.port.inbound.message;

import com.cotalk.domain.entity.Message;

/**
 * 메시지 답장 및 전달 유스케이스.
 * 메시지에 답장하거나 다른 채팅방으로 전달한다.
 *
 * @author seunggu.lee
 */
public interface MessageReplyForwardUseCase {

    /**
     * 메시지에 답장한다.
     *
     * @param senderId 발신자 ID
     * @param originalMessageId 답장할 원본 메시지 ID
     * @param content 답장 내용
     * @return 생성된 답장 메시지
     */
    Message replyToMessage(Long senderId, Long originalMessageId, String content);

    /**
     * 메시지를 다른 채팅방으로 전달한다.
     *
     * @param senderId 발신자 ID
     * @param originalMessageId 전달할 원본 메시지 ID
     * @param targetChatRoomId 전달 대상 채팅방 ID
     * @return 생성된 전달 메시지
     */
    Message forwardMessage(Long senderId, Long originalMessageId, Long targetChatRoomId);
}
