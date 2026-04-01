package com.cotalk.domain.port.outbound;

import java.util.List;

/**
 * 알림 전송을 요청하는 도메인 간 호출 포트.
 *
 * <p>다른 application 서비스는 notification 인바운드 유스케이스 대신
 * 이 포트를 통해 알림 전송을 위임한다.</p>
 *
 * @author seunggu.lee
 */
public interface NotificationCommandPort {

    /**
     * 특정 사용자에게 새 메시지 알림을 전송한다.
     *
     * @param receiverUserId 알림 수신자 ID
     * @param senderNickname 메시지 발신자 닉네임
     * @param messageContent 메시지 내용
     * @param chatRoomId 채팅방 ID
     * @param senderAvatarUrl 발신자 프로필 이미지 URL
     */
    void sendNewMessageNotification(Long receiverUserId, String senderNickname, String messageContent, Long chatRoomId, String senderAvatarUrl);

    /**
     * 여러 사용자에게 새 메시지 알림을 벌크 전송한다.
     *
     * @param receiverUserIds 알림 수신자 ID 목록
     * @param senderNickname 메시지 발신자 닉네임
     * @param messageContent 메시지 내용
     * @param chatRoomId 채팅방 ID
     * @param senderAvatarUrl 발신자 프로필 이미지 URL
     */
    void sendNewMessageNotificationBulk(List<Long> receiverUserIds, String senderNickname, String messageContent, Long chatRoomId, String senderAvatarUrl);

    /**
     * 특정 사용자에게 친구 요청 알림을 전송한다.
     *
     * @param receiverUserId 알림 수신자 ID
     * @param senderNickname 친구 요청 발신자 닉네임
     */
    void sendFriendRequestNotification(Long receiverUserId, String senderNickname);
}
