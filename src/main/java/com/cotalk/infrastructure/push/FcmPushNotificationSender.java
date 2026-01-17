package com.cotalk.infrastructure.push;

import com.cotalk.domain.entity.DeviceToken;
import com.cotalk.domain.port.outbound.DeviceTokenRepository;
import com.cotalk.domain.port.outbound.PushNotificationSender;
import com.google.firebase.messaging.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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

    private static final int FCM_BATCH_SIZE = 500;

    private final FirebaseMessaging firebaseMessaging;
    private final DeviceTokenRepository deviceTokenRepository;

    /**
     * FcmPushNotificationSender를 생성한다.
     *
     * @param firebaseMessaging     FirebaseMessaging 인스턴스, Firebase가 비활성화된 경우 {@code null}일 수 있음
     * @param deviceTokenRepository 디바이스 토큰 레포지토리
     */
    public FcmPushNotificationSender(
            @Nullable FirebaseMessaging firebaseMessaging,
            DeviceTokenRepository deviceTokenRepository) {
        this.firebaseMessaging = firebaseMessaging;
        this.deviceTokenRepository = deviceTokenRepository;
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
    @Transactional
    public boolean send(String token, String title, String body, Map<String, String> data) {
        if (firebaseMessaging == null) {
            log.debug("Firebase is not configured. Skipping push notification.");
            return false;
        }

        try {
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(createNotification(title, body))
                    .putAllData(data)
                    .setAndroidConfig(createAndroidConfig())
                    .setApnsConfig(createApnsConfig())
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
    @Transactional
    public int sendMultiple(List<String> tokens, String title, String body, Map<String, String> data) {
        if (firebaseMessaging == null) {
            log.debug("Firebase is not configured. Skipping push notifications.");
            return 0;
        }

        if (tokens.isEmpty()) {
            return 0;
        }

        int successCount = 0;
        for (int i = 0; i < tokens.size(); i += FCM_BATCH_SIZE) {
            List<String> batch = tokens.subList(i, Math.min(i + FCM_BATCH_SIZE, tokens.size()));
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
                    .setNotification(createNotification(title, body))
                    .putAllData(data)
                    .setAndroidConfig(createAndroidConfig())
                    .setApnsConfig(createApnsConfig())
                    .build();

            BatchResponse response = firebaseMessaging.sendEachForMulticast(message);

            if (response.getFailureCount() > 0) {
                log.warn("FCM batch send partial failure: {}/{} failed",
                        response.getFailureCount(), tokens.size());
                handleBatchFailures(tokens, response);
            }

            return response.getSuccessCount();

        } catch (FirebaseMessagingException e) {
            log.error("FCM batch send failed", e);
            return 0;
        }
    }

    /**
     * 배치 전송 실패를 처리한다.
     * 유효하지 않은 토큰을 찾아 비활성화한다.
     *
     * @param tokens   전송된 토큰 목록
     * @param response FCM 배치 응답
     */
    private void handleBatchFailures(List<String> tokens, BatchResponse response) {
        List<SendResponse> responses = response.getResponses();
        List<String> invalidTokens = new ArrayList<>();

        for (int i = 0; i < responses.size(); i++) {
            SendResponse sendResponse = responses.get(i);
            if (!sendResponse.isSuccessful()) {
                FirebaseMessagingException exception = sendResponse.getException();
                if (exception != null && isInvalidTokenError(exception.getMessagingErrorCode())) {
                    invalidTokens.add(tokens.get(i));
                }
            }
        }

        if (!invalidTokens.isEmpty()) {
            deactivateTokens(invalidTokens);
        }
    }

    /**
     * FCM 오류를 처리한다.
     *
     * <p>토큰이 유효하지 않거나 등록 해제된 경우 토큰을 비활성화한다.
     *
     * @param token 오류가 발생한 토큰
     * @param e     발생한 FCM 예외
     */
    private void handleFcmError(String token, FirebaseMessagingException e) {
        MessagingErrorCode errorCode = e.getMessagingErrorCode();

        if (isInvalidTokenError(errorCode)) {
            log.warn("FCM token is invalid or unregistered: {}", token);
            deactivateToken(token);
        } else {
            log.error("FCM send failed for token {}: {}", token, e.getMessage());
        }
    }

    /**
     * 토큰 오류가 토큰 자체의 문제인지 확인한다.
     *
     * @param errorCode FCM 에러 코드
     * @return 토큰이 유효하지 않은 경우 {@code true}
     */
    private boolean isInvalidTokenError(MessagingErrorCode errorCode) {
        return errorCode == MessagingErrorCode.UNREGISTERED ||
               errorCode == MessagingErrorCode.INVALID_ARGUMENT;
    }

    /**
     * 단일 토큰을 비활성화한다.
     *
     * @param token 비활성화할 토큰
     */
    private void deactivateToken(String token) {
        deviceTokenRepository.findByToken(token)
                .ifPresent(deviceToken -> {
                    deviceToken.deactivate();
                    deviceTokenRepository.save(deviceToken);
                    log.info("Deactivated invalid FCM token: {}", token);
                });
    }

    /**
     * 여러 토큰을 비활성화한다.
     *
     * @param tokens 비활성화할 토큰 목록
     */
    private void deactivateTokens(List<String> tokens) {
        for (String token : tokens) {
            deactivateToken(token);
        }
        log.info("Deactivated {} invalid FCM tokens", tokens.size());
    }

    /**
     * FCM 알림 객체를 생성한다.
     *
     * @param title 알림 제목
     * @param body  알림 본문
     * @return Notification 객체
     */
    private Notification createNotification(String title, String body) {
        return Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();
    }

    /**
     * Android 플랫폼용 설정을 생성한다.
     *
     * @return AndroidConfig 객체
     */
    private AndroidConfig createAndroidConfig() {
        return AndroidConfig.builder()
                .setPriority(AndroidConfig.Priority.HIGH)
                .setNotification(AndroidNotification.builder()
                        .setSound("default")
                        .build())
                .build();
    }

    /**
     * iOS 플랫폼용 APNs 설정을 생성한다.
     *
     * @return ApnsConfig 객체
     */
    private ApnsConfig createApnsConfig() {
        return ApnsConfig.builder()
                .setAps(Aps.builder()
                        .setSound("default")
                        .setBadge(1)
                        .build())
                .build();
    }
}
