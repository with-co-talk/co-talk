package com.cotalk.infrastructure.push;

import com.cotalk.domain.port.outbound.PushNotificationSender;
import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Firebase Cloud Messaging을 통한 푸시 알림 전송 구현체.
 */
@Slf4j
@Component
public class FcmPushNotificationSender implements PushNotificationSender {

    private final FirebaseMessaging firebaseMessaging;

    public FcmPushNotificationSender(@org.springframework.lang.Nullable FirebaseMessaging firebaseMessaging) {
        this.firebaseMessaging = firebaseMessaging;
    }

    @Override
    public boolean send(String token, String title, String body, Map<String, String> data) {
        if (firebaseMessaging == null) {
            log.debug("Firebase is not configured. Skipping push notification.");
            return false;
        }

        try {
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putAllData(data)
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .setNotification(AndroidNotification.builder()
                                    .setSound("default")
                                    .build())
                            .build())
                    .setApnsConfig(ApnsConfig.builder()
                            .setAps(Aps.builder()
                                    .setSound("default")
                                    .setBadge(1)
                                    .build())
                            .build())
                    .build();

            String response = firebaseMessaging.send(message);
            log.debug("FCM message sent successfully: {}", response);
            return true;

        } catch (FirebaseMessagingException e) {
            handleFcmError(token, e);
            return false;
        }
    }

    @Override
    public int sendMultiple(List<String> tokens, String title, String body, Map<String, String> data) {
        if (firebaseMessaging == null) {
            log.debug("Firebase is not configured. Skipping push notifications.");
            return 0;
        }

        if (tokens.isEmpty()) {
            return 0;
        }

        // 500개씩 배치 처리 (FCM 제한)
        int successCount = 0;
        for (int i = 0; i < tokens.size(); i += 500) {
            List<String> batch = tokens.subList(i, Math.min(i + 500, tokens.size()));
            successCount += sendBatch(batch, title, body, data);
        }
        return successCount;
    }

    private int sendBatch(List<String> tokens, String title, String body, Map<String, String> data) {
        try {
            MulticastMessage message = MulticastMessage.builder()
                    .addAllTokens(tokens)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putAllData(data)
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .setNotification(AndroidNotification.builder()
                                    .setSound("default")
                                    .build())
                            .build())
                    .setApnsConfig(ApnsConfig.builder()
                            .setAps(Aps.builder()
                                    .setSound("default")
                                    .setBadge(1)
                                    .build())
                            .build())
                    .build();

            BatchResponse response = firebaseMessaging.sendEachForMulticast(message);
            
            if (response.getFailureCount() > 0) {
                log.warn("FCM batch send partial failure: {}/{} failed", 
                        response.getFailureCount(), tokens.size());
                // TODO: 실패한 토큰 처리 (비활성화 등)
            }
            
            return response.getSuccessCount();

        } catch (FirebaseMessagingException e) {
            log.error("FCM batch send failed", e);
            return 0;
        }
    }

    private void handleFcmError(String token, FirebaseMessagingException e) {
        MessagingErrorCode errorCode = e.getMessagingErrorCode();
        
        if (errorCode == MessagingErrorCode.UNREGISTERED || 
            errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
            log.warn("FCM token is invalid or unregistered: {}", token);
            // TODO: 토큰 비활성화 이벤트 발행
        } else {
            log.error("FCM send failed for token {}: {}", token, e.getMessage());
        }
    }
}
