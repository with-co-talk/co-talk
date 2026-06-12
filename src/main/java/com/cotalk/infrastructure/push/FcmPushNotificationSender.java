package com.cotalk.infrastructure.push;

import com.cotalk.domain.port.outbound.PushNotificationSender;
import com.cotalk.domain.port.outbound.PushNotificationSender.PushTarget;
import com.cotalk.domain.util.TokenMasker;
import com.google.firebase.messaging.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

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
 * <p>성능: FCM 원격 호출(네트워크, 수백ms~수초)은 트랜잭션 밖에서 수행하고,
 * 전송 실패로 무효화된 토큰의 비활성화(DB 쓰기)만 {@link InvalidTokenDeactivator}의
 * 짧은 별도 트랜잭션으로 위임하여 DB 커넥션 점유 시간을 최소화한다.
 *
 * @author seunggu.lee
 * @see PushNotificationSender
 * @see FcmConfig
 * @see InvalidTokenDeactivator
 */
@Slf4j
@Component
public class FcmPushNotificationSender implements PushNotificationSender {

    private static final int FCM_BATCH_SIZE = 500;

    private final FirebaseMessaging firebaseMessaging;
    private final InvalidTokenDeactivator invalidTokenDeactivator;

    /**
     * FcmPushNotificationSender를 생성한다.
     *
     * @param firebaseMessaging      FirebaseMessaging 인스턴스, Firebase가 비활성화된 경우 {@code null}일 수 있음
     * @param invalidTokenDeactivator 무효 토큰 비활성화 컴포넌트(별도 트랜잭션)
     */
    public FcmPushNotificationSender(
            @Nullable FirebaseMessaging firebaseMessaging,
            InvalidTokenDeactivator invalidTokenDeactivator) {
        this.firebaseMessaging = firebaseMessaging;
        this.invalidTokenDeactivator = invalidTokenDeactivator;
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
     * @param imageUrl 알림에 표시할 이미지 URL (없으면 null)
     * @return 전송 성공 시 {@code true}, 실패 시 {@code false}
     */
    @Override
    public boolean send(String token, String title, String body, Map<String, String> data, String imageUrl) {
        if (firebaseMessaging == null) {
            log.debug("Firebase is not configured. Skipping push notification.");
            return false;
        }

        try {
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(createNotification(title, body, imageUrl))
                    .putAllData(data)
                    .setAndroidConfig(createAndroidConfig(null))
                    .setApnsConfig(createApnsConfig(null))
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
     * @param imageUrl 알림에 표시할 이미지 URL (없으면 null)
     * @return 성공적으로 전송된 알림 수
     */
    @Override
    public int sendMultiple(List<String> tokens, String title, String body, Map<String, String> data, String imageUrl) {
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
            successCount += sendBatch(batch, title, body, data, imageUrl);
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
     * @param imageUrl 알림에 표시할 이미지 URL (없으면 null)
     * @return 성공적으로 전송된 알림 수
     */
    private int sendBatch(List<String> tokens, String title, String body, Map<String, String> data, String imageUrl) {
        try {
            MulticastMessage message = MulticastMessage.builder()
                    .addAllTokens(tokens)
                    .setNotification(createNotification(title, body, imageUrl))
                    .putAllData(data)
                    .setAndroidConfig(createAndroidConfig(null))
                    .setApnsConfig(createApnsConfig(null))
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
            invalidTokenDeactivator.deactivateTokens(invalidTokens);
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
            log.warn("FCM token is invalid or unregistered: {}", TokenMasker.mask(token));
            invalidTokenDeactivator.deactivateToken(token);
        } else {
            log.error("FCM send failed for token {}: {}", TokenMasker.mask(token), e.getMessage());
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
     * FCM 알림 객체를 생성한다.
     *
     * @param title 알림 제목
     * @param body  알림 본문
     * @param imageUrl 알림에 표시할 이미지 URL (없으면 null)
     * @return Notification 객체
     */
    private Notification createNotification(String title, String body, String imageUrl) {
        Notification.Builder builder = Notification.builder()
                .setTitle(title)
                .setBody(body);
        if (imageUrl != null && !imageUrl.isBlank()) {
            builder.setImage(imageUrl);
        }
        return builder.build();
    }

    /**
     * Android 플랫폼용 설정을 생성한다.
     *
     * <p>채널 ID는 Flutter 앱의 NotificationService에서 생성한 채널과 일치해야 한다.
     * Android 8.0(API 26) 이상에서는 채널 ID가 필수이며, 지정하지 않으면
     * 백그라운드 알림이 제대로 표시되지 않을 수 있다.
     *
     * <p>{@code badge}가 {@code null}이 아니면 알림 개수(notification count)를
     * best-effort로 설정한다.
     *
     * @param badge 알림 개수로 설정할 값 ({@code null}이면 설정하지 않음)
     * @return AndroidConfig 객체
     */
    private AndroidConfig createAndroidConfig(Integer badge) {
        AndroidNotification.Builder notificationBuilder = AndroidNotification.builder()
                .setChannelId("chat_messages")
                .setSound("default");
        if (badge != null) {
            notificationBuilder.setNotificationCount(badge);
        }
        return AndroidConfig.builder()
                .setPriority(AndroidConfig.Priority.HIGH)
                .setNotification(notificationBuilder.build())
                .build();
    }

    /**
     * iOS 플랫폼용 APNs 설정을 생성한다.
     *
     * <p>APNs 헤더 설명:
     * <ul>
     *   <li>apns-push-type: alert - 사용자에게 표시되는 알림임을 명시</li>
     *   <li>apns-priority: 10 - 즉시 전송 (5는 절전 모드로 지연될 수 있음)</li>
     * </ul>
     *
     * <p>{@code badge}가 {@code null}이면 배지를 설정하지 않아 기존 배지를 변경하지 않는다.
     * {@code null}이 아니면 해당 값으로 앱 아이콘 배지를 설정한다.
     *
     * @param badge 앱 아이콘 배지 값 ({@code null}이면 배지를 변경하지 않음)
     * @return ApnsConfig 객체
     */
    private ApnsConfig createApnsConfig(Integer badge) {
        Aps.Builder apsBuilder = Aps.builder()
                .setSound("default");
        if (badge != null) {
            apsBuilder.setBadge(badge);
        }
        return ApnsConfig.builder()
                .putHeader("apns-push-type", "alert")
                .putHeader("apns-priority", "10")
                .setAps(apsBuilder.build())
                .build();
    }

    /**
     * 디바이스별로 서로 다른 배지 값을 적용하여 푸시 알림을 전송한다.
     *
     * <p>각 대상은 자신의 토큰과 배지 값을 가지며, iOS는 APNs 배지로,
     * Android는 알림 개수로 best-effort 적용된다. 배지 값이 {@code null}인 대상은
     * 배지를 변경하지 않는다. FCM의 제한(500개/요청)에 맞춰 자동으로 배치 처리된다.
     *
     * @param targets  전송 대상 목록 (각 대상의 토큰과 배지 값)
     * @param title    알림 제목
     * @param body     알림 본문
     * @param data     추가 데이터 맵
     * @param imageUrl 알림에 표시할 이미지 URL (없으면 null)
     * @return 성공적으로 전송된 알림 수
     */
    @Override
    public int sendEachWithBadge(List<PushTarget> targets, String title, String body, Map<String, String> data, String imageUrl) {
        if (firebaseMessaging == null) {
            log.debug("Firebase is not configured. Skipping push notifications.");
            return 0;
        }

        if (targets.isEmpty()) {
            return 0;
        }

        int successCount = 0;
        for (int i = 0; i < targets.size(); i += FCM_BATCH_SIZE) {
            List<PushTarget> batch = targets.subList(i, Math.min(i + FCM_BATCH_SIZE, targets.size()));
            successCount += sendEachBatch(batch, title, body, data, imageUrl);
        }
        return successCount;
    }

    /**
     * 대상 배치에 대해 대상별 배지가 적용된 메시지를 전송한다.
     *
     * @param targets  전송 대상 목록 (최대 500개)
     * @param title    알림 제목
     * @param body     알림 본문
     * @param data     추가 데이터 맵
     * @param imageUrl 알림에 표시할 이미지 URL (없으면 null)
     * @return 성공적으로 전송된 알림 수
     */
    private int sendEachBatch(List<PushTarget> targets, String title, String body, Map<String, String> data, String imageUrl) {
        try {
            List<Message> messages = new ArrayList<>(targets.size());
            for (PushTarget target : targets) {
                messages.add(Message.builder()
                        .setToken(target.token())
                        .setNotification(createNotification(title, body, imageUrl))
                        .putAllData(data)
                        .setAndroidConfig(createAndroidConfig(target.badge()))
                        .setApnsConfig(createApnsConfig(target.badge()))
                        .build());
            }

            BatchResponse response = firebaseMessaging.sendEach(messages);

            if (response.getFailureCount() > 0) {
                log.warn("FCM sendEach partial failure: {}/{} failed",
                        response.getFailureCount(), targets.size());
                handleEachFailures(targets, response);
            }

            return response.getSuccessCount();

        } catch (FirebaseMessagingException e) {
            log.error("FCM sendEach failed", e);
            return 0;
        }
    }

    /**
     * 대상별 전송 실패를 처리한다.
     * 유효하지 않은 토큰을 찾아 비활성화한다.
     *
     * @param targets  전송된 대상 목록
     * @param response FCM 배치 응답
     */
    private void handleEachFailures(List<PushTarget> targets, BatchResponse response) {
        List<SendResponse> responses = response.getResponses();
        List<String> invalidTokens = new ArrayList<>();

        for (int i = 0; i < responses.size(); i++) {
            SendResponse sendResponse = responses.get(i);
            if (!sendResponse.isSuccessful()) {
                FirebaseMessagingException exception = sendResponse.getException();
                if (exception != null && isInvalidTokenError(exception.getMessagingErrorCode())) {
                    invalidTokens.add(targets.get(i).token());
                }
            }
        }

        if (!invalidTokens.isEmpty()) {
            invalidTokenDeactivator.deactivateTokens(invalidTokens);
        }
    }
}
