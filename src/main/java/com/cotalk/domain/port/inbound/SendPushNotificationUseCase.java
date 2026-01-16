package com.cotalk.domain.port.inbound;

/**
 * 푸시 알림 전송 유스케이스
 */
public interface SendPushNotificationUseCase {

    /**
     * 특정 사용자에게 새 메시지 알림을 전송합니다.
     * 
     * @param receiverUserId 알림 수신자 ID
     * @param senderNickname 메시지 발신자 닉네임
     * @param messageContent 메시지 내용 (미리보기)
     * @param chatRoomId 채팅방 ID
     */
    void sendNewMessageNotification(Long receiverUserId, String senderNickname, String messageContent, Long chatRoomId);

    /**
     * 특정 사용자에게 친구 요청 알림을 전송합니다.
     * 
     * @param receiverUserId 알림 수신자 ID
     * @param senderNickname 친구 요청 발신자 닉네임
     */
    void sendFriendRequestNotification(Long receiverUserId, String senderNickname);
}
