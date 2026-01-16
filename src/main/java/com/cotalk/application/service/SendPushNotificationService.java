package com.cotalk.application.service;

import com.cotalk.domain.entity.DeviceToken;
import com.cotalk.domain.port.inbound.SendPushNotificationUseCase;
import com.cotalk.domain.port.outbound.DeviceTokenRepository;
import com.cotalk.domain.port.outbound.PushNotificationSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SendPushNotificationService implements SendPushNotificationUseCase {

    private static final int MAX_MESSAGE_PREVIEW_LENGTH = 100;

    private final DeviceTokenRepository deviceTokenRepository;
    private final PushNotificationSender pushNotificationSender;

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
