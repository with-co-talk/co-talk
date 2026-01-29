package com.cotalk.domain.port.outbound;

import java.time.LocalDateTime;

/**
 * 사용자별 이벤트 브로커 아웃바운드 포트.
 * 특정 사용자에게 실시간 이벤트를 발행하기 위한 인터페이스를 정의한다.
 * 채팅 목록 업데이트, 읽음 상태 변경 등의 이벤트를 전달한다.
 *
 * @author seunggu.lee
 */
public interface UserEventBroker {

    /**
     * 특정 사용자에게 채팅 목록 업데이트 이벤트를 발행한다.
     *
     * @param userId 대상 사용자 ID
     * @param event  채팅 목록 업데이트 이벤트
     */
    void publishChatListUpdate(Long userId, ChatListUpdateEvent event);

    /**
     * 특정 사용자에게 읽음 상태 변경 이벤트를 발행한다.
     *
     * @param userId 대상 사용자 ID
     * @param event  읽음 상태 이벤트
     */
    void publishReadReceipt(Long userId, ReadReceiptEvent event);

    /**
     * 특정 사용자에게 온라인 상태 변경 이벤트를 발행한다.
     *
     * @param userId 대상 사용자 ID
     * @param event  온라인 상태 이벤트
     */
    void publishOnlineStatus(Long userId, OnlineStatusEvent event);

    /**
     * 채팅 목록 업데이트 이벤트.
     * 새 메시지가 도착하거나 채팅방 정보가 변경되었을 때 발행된다.
     *
     * @param eventType       이벤트 유형 (NEW_MESSAGE, READ_UPDATE, ROOM_UPDATE)
     * @param roomId          채팅방 ID
     * @param lastMessage     마지막 메시지 내용
     * @param lastMessageType 마지막 메시지 유형
     * @param lastMessageAt   마지막 메시지 시간
     * @param senderId        발신자 ID
     * @param senderNickname  발신자 닉네임
     * @param unreadCount     읽지 않은 메시지 수
     */
    record ChatListUpdateEvent(
            Integer schemaVersion,
            String eventId,
            String eventType,
            Long roomId,
            String lastMessage,
            String lastMessageType,
            LocalDateTime lastMessageAt,
            Long senderId,
            String senderNickname,
            Integer unreadCount
    ) {}

    /**
     * 읽음 상태 변경 이벤트.
     * 메시지를 읽었을 때 채팅방의 모든 멤버에게 전달된다.
     *
     * @param chatRoomId 채팅방 ID
     * @param userId     읽은 사용자 ID
     * @param lastReadMessageId 마지막으로 읽은 메시지 ID (optional)
     * @param lastReadAt 마지막 읽은 시간 (optional)
     */
    record ReadReceiptEvent(
            Integer schemaVersion,
            String eventId,
            Long chatRoomId,
            Long userId,
            Long lastReadMessageId,
            LocalDateTime lastReadAt
    ) {}

    /**
     * 온라인 상태 변경 이벤트.
     * 사용자의 온라인/오프라인 상태가 변경되었을 때 발행된다.
     *
     * @param userId       상태가 변경된 사용자 ID
     * @param isOnline     온라인 여부
     * @param lastActiveAt 마지막 활동 시간
     */
    record OnlineStatusEvent(
            Integer schemaVersion,
            String eventId,
            Long userId,
            boolean isOnline,
            LocalDateTime lastActiveAt
    ) {}
}
