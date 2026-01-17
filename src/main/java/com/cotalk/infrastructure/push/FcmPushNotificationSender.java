package com.cotalk.infrastructure.push;

import com.cotalk.domain.port.outbound.PushNotificationSender;
import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Firebase Cloud Messaging(FCM)을 통한 푸시 알림 전송 구현체.
 * {@link PushNotificationSender} 포트를 구현하여 FCM을 통해 모바일 기기로 푸시 알림을 전송한다.
 *
 * <p>Android와 iOS 모두 지원하며, 각 플랫폼에 맞는 설정이 자동으로 적용된다.
 * Firebase가 구성되지 않은 경우 알림 전송을 건너뛰고 로그만 출력한다.
 *
 * <p>대량 전송 시 FCM의 제한(500개/요청)에 맞춰 자동으로 배치 처리된다.
 *
 * @author seunggu.lee
 * @see PushNotificationSender
 * @see FcmConfig
 */
@Slf4j
@Component
public class FcmPushNotificationSender implements PushNotificationSender {

    private final FirebaseMessaging firebaseMessaging;

    /**
     * FcmPushNotificationSender를 생성한다.
     *
     * @param firebaseMessaging FirebaseMessaging 인스턴스, Firebase가 비활성화된 경우 {@code null}일 수 있음
     */
    public FcmPushNotificationSender(@org.springframework.lang.Nullable FirebaseMessaging firebaseMessaging) {
        this.firebaseMessaging = firebaseMessaging;
    }

    /**
     * 단일 디바이스에 푸시 알림을 전송한다.
     *
     * <p>Android는 HIGH 우선순위로, iOS는 기본 사운드와 배지 1로 설정된다.
     *
     * @param token 대상 디바이스의 FCM 토큰
     * @param title 알림 제목
     * @param body  알림 본문
     * @param data  추가 데이터 맵
     * @return 전송 성공 시 {@code true}, 실패 시 {@code false}
     */
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

    /**
     * 여러 디바이스에 동일한 푸시 알림을 전송한다.
     *
     * <p>FCM의 제한(500개/요청)에 맞춰 자동으로 배치 처리된다.
     *
     * @param tokens 대상 디바이스들의 FCM 토큰 목록
     * @param title  알림 제목
     * @param body   알림 본문
     * @param data   추가 데이터 맵
     * @return 성공적으로 전송된 알림 수
     */
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

    /**
     * 토큰 배치에 대해 멀티캐스트 메시지를 전송한다.
     *
     * @param tokens 전송할 토큰 목록 (최대 500개)
     * @param title  알림 제목
     * @param body   알림 본문
     * @param data   추가 데이터 맵
     * @return 성공적으로 전송된 알림 수
     */
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

    /**
     * FCM 오류를 처리한다.
     *
     * <p>토큰이 유효하지 않거나 등록 해제된 경우 경고 로그를 출력한다.
     *
     * @param token 오류가 발생한 토큰
     * @param e     발생한 FCM 예외
     */
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
