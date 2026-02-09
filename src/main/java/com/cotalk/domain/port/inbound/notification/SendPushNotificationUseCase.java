package com.cotalk.domain.port.inbound.notification;

import java.util.List;

/**
 * 푸시 알림 전송 유스케이스.
 * 새 메시지, 친구 요청 등의 푸시 알림을 전송한다.
 *
 * @author seunggu.lee
 */
public interface SendPushNotificationUseCase {

    /**
     * 특정 사용자에게 새 메시지 알림을 전송한다.
     *
     * @param receiverUserId 알림 수신자 ID
     * @param senderNickname 메시지 발신자 닉네임
     * @param messageContent 메시지 내용 (미리보기)
     * @param chatRoomId 채팅방 ID
     * @param senderAvatarUrl 발신자 프로필 이미지 URL (없으면 null)
     */
    void sendNewMessageNotification(Long receiverUserId, String senderNickname, String messageContent, Long chatRoomId, String senderAvatarUrl);

    /**
     * 여러 사용자에게 새 메시지 알림을 벌크 전송한다.
     *
     * @param receiverUserIds 알림 수신자 ID 목록
     * @param senderNickname  메시지 발신자 닉네임
     * @param messageContent  메시지 내용 (미리보기)
     * @param chatRoomId      채팅방 ID
     * @param senderAvatarUrl 발신자 프로필 이미지 URL (없으면 null)
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
