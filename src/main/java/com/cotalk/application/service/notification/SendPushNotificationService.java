package com.cotalk.application.service.notification;

import com.cotalk.domain.entity.DeviceToken;
import com.cotalk.domain.port.inbound.notification.SendPushNotificationUseCase;
import com.cotalk.domain.port.outbound.DeviceTokenRepository;
import com.cotalk.domain.port.outbound.PushNotificationSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 푸시 알림 전송 유스케이스 구현체.
 * 사용자 디바이스로 푸시 알림을 비동기로 전송한다.
 *
 * @author seunggu.lee
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SendPushNotificationService implements SendPushNotificationUseCase {

    private static final int MAX_MESSAGE_PREVIEW_LENGTH = 100;

    private final DeviceTokenRepository deviceTokenRepository;
    private final PushNotificationSender pushNotificationSender;

    /**
     * 새 메시지 알림을 전송한다.
     * 수신자의 활성화된 모든 디바이스로 비동기로 푸시 알림을 전송한다.
     *
     * @param receiverUserId 알림을 받을 사용자 ID
     * @param senderNickname 메시지를 보낸 사용자 닉네임
     * @param messageContent 메시지 내용 (100자 초과시 잘림)
     * @param chatRoomId     채팅방 ID
     */
    @Override
    @Async
    public void sendNewMessageNotification(Long receiverUserId, String senderNickname, String messageContent, Long chatRoomId) {
        List<DeviceToken> tokens = deviceTokenRepository.findActiveByUserId(receiverUserId);
        
        if (tokens.isEmpty()) {
            log.debug("No active device tokens for user: {}", receiverUserId);
            return;
        }

        List<String> tokenStrings = tokens.stream()
                .map(DeviceToken::getToken)
                .toList();

        String title = senderNickname;
        String body = truncateMessage(messageContent);
        Map<String, String> data = Map.of(
                "type", "NEW_MESSAGE",
                "chatRoomId", chatRoomId.toString()
        );

        int sentCount = pushNotificationSender.sendMultiple(tokenStrings, title, body, data);
        log.info("New message push sent to user {}: {}/{} devices", receiverUserId, sentCount, tokenStrings.size());
    }

    /**
     * 친구 요청 알림을 전송한다.
     * 수신자의 활성화된 모든 디바이스로 비동기로 푸시 알림을 전송한다.
     *
     * @param receiverUserId 알림을 받을 사용자 ID
     * @param senderNickname 친구 요청을 보낸 사용자 닉네임
     */
    @Override
    @Async
    public void sendFriendRequestNotification(Long receiverUserId, String senderNickname) {
        List<DeviceToken> tokens = deviceTokenRepository.findActiveByUserId(receiverUserId);
        
        if (tokens.isEmpty()) {
            log.debug("No active device tokens for user: {}", receiverUserId);
            return;
        }

        List<String> tokenStrings = tokens.stream()
                .map(DeviceToken::getToken)
                .toList();

        String title = "친구 요청";
        String body = senderNickname + "님이 친구 요청을 보냈습니다.";
        Map<String, String> data = Map.of(
                "type", "FRIEND_REQUEST"
        );

        int sentCount = pushNotificationSender.sendMultiple(tokenStrings, title, body, data);
        log.info("Friend request push sent to user {}: {}/{} devices", receiverUserId, sentCount, tokenStrings.size());
    }

    /**
     * 메시지를 최대 길이로 잘라낸다.
     *
     * @param message 원본 메시지
     * @return 잘라낸 메시지 (100자 초과시 "..." 추가)
     */
    private String truncateMessage(String message) {
        if (message == null) {
            return "";
        }
        if (message.length() <= MAX_MESSAGE_PREVIEW_LENGTH) {
            return message;
        }
        return message.substring(0, MAX_MESSAGE_PREVIEW_LENGTH) + "...";
    }
}
