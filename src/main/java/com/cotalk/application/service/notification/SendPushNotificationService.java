package com.cotalk.application.service.notification;

import com.cotalk.domain.entity.DeviceToken;
import com.cotalk.domain.entity.NotificationSetting;
import com.cotalk.domain.port.inbound.notification.SendPushNotificationUseCase;
import com.cotalk.domain.port.outbound.DeviceTokenRepository;
import com.cotalk.domain.port.outbound.NotificationSettingRepository;
import com.cotalk.domain.port.outbound.PushNotificationSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 푸시 알림 전송 유스케이스 구현체.
 * 사용자 디바이스로 푸시 알림을 비동기로 전송한다.
 * 사용자의 알림 설정(메시지 알림 on/off, 방해 금지 모드, 미리보기 모드)을 확인한 후 전송한다.
 *
 * @author seunggu.lee
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SendPushNotificationService implements SendPushNotificationUseCase {

    private static final int MAX_MESSAGE_PREVIEW_LENGTH = 100;
    private static final String PREVIEW_MODE_NAME_AND_MESSAGE = "NAME_AND_MESSAGE";
    private static final String PREVIEW_MODE_NAME_ONLY = "NAME_ONLY";
    private static final String DEFAULT_MESSAGE_BODY = "새 메시지가 도착했습니다.";
    private static final String APP_NAME = "Co-Talk";

    private final DeviceTokenRepository deviceTokenRepository;
    private final PushNotificationSender pushNotificationSender;
    private final NotificationSettingRepository notificationSettingRepository;

    /**
     * 새 메시지 알림을 전송한다.
     * 수신자의 알림 설정을 확인하여 메시지 알림이 꺼져 있거나 방해 금지 시간대인 경우 전송하지 않는다.
     * 미리보기 모드에 따라 알림 제목과 본문을 조정한다.
     *
     * @param receiverUserId  알림을 받을 사용자 ID
     * @param senderNickname  메시지를 보낸 사용자 닉네임
     * @param messageContent  메시지 내용 (100자 초과시 잘림)
     * @param chatRoomId      채팅방 ID
     * @param senderAvatarUrl 발신자 프로필 이미지 URL (없으면 null)
     */
    @Override
    @Async
    public void sendNewMessageNotification(Long receiverUserId, String senderNickname, String messageContent, Long chatRoomId, String senderAvatarUrl) {
        NotificationSetting setting = notificationSettingRepository.findByUserId(receiverUserId)
                .orElse(null);

        if (!isMessageNotificationAllowed(setting)) {
            log.debug("Message notification disabled for user: {}", receiverUserId);
            return;
        }

        List<DeviceToken> tokens = deviceTokenRepository.findActiveByUserId(receiverUserId);

        if (tokens.isEmpty()) {
            log.debug("No active device tokens for user: {}", receiverUserId);
            return;
        }

        List<String> tokenStrings = tokens.stream()
                .map(DeviceToken::getToken)
                .toList();

        String previewMode = setting != null ? setting.getNotificationPreviewMode() : PREVIEW_MODE_NAME_AND_MESSAGE;
        String title = resolveTitle(senderNickname, previewMode);
        String body = resolveBody(messageContent, previewMode);
        Map<String, String> data = new HashMap<>();
        data.put("type", "NEW_MESSAGE");
        data.put("chatRoomId", chatRoomId.toString());
        data.put("title", title);
        data.put("body", body);
        if (senderAvatarUrl != null) {
            data.put("avatarUrl", senderAvatarUrl);
        }

        int sentCount = pushNotificationSender.sendMultiple(tokenStrings, title, body, data, senderAvatarUrl);
        log.info("New message push sent to user {}: {}/{} devices", receiverUserId, sentCount, tokenStrings.size());
    }

    /**
     * 여러 사용자에게 새 메시지 알림을 벌크 전송한다.
     * 각 사용자의 알림 설정을 확인하여 메시지 알림이 꺼져 있거나 방해 금지 시간대인 사용자는 제외한다.
     * 각 사용자의 미리보기 모드(NAME_AND_MESSAGE, NAME_ONLY, 숨김)를 존중하여 그룹별로 별도 전송한다.
     *
     * @param receiverUserIds 알림을 받을 사용자 ID 목록
     * @param senderNickname  메시지를 보낸 사용자 닉네임
     * @param messageContent  메시지 내용 (100자 초과시 잘림)
     * @param chatRoomId      채팅방 ID
     * @param senderAvatarUrl 발신자 프로필 이미지 URL (없으면 null)
     */
    @Override
    @Async
    public void sendNewMessageNotificationBulk(List<Long> receiverUserIds, String senderNickname, String messageContent, Long chatRoomId, String senderAvatarUrl) {
        if (receiverUserIds.isEmpty()) {
            return;
        }

        Map<Long, NotificationSetting> settingsMap = notificationSettingRepository.findByUserIds(receiverUserIds)
                .stream()
                .collect(Collectors.toMap(NotificationSetting::getUserId, Function.identity()));

        LocalTime now = LocalTime.now();
        List<Long> allowedUserIds = receiverUserIds.stream()
                .filter(userId -> {
                    NotificationSetting setting = settingsMap.get(userId);
                    return isMessageNotificationAllowed(setting, now);
                })
                .toList();

        if (allowedUserIds.isEmpty()) {
            log.debug("All users have message notification disabled: {}", receiverUserIds);
            return;
        }

        // 미리보기 모드별로 사용자 그룹화
        Map<String, List<Long>> usersByPreviewMode = allowedUserIds.stream()
                .collect(Collectors.groupingBy(userId -> {
                    NotificationSetting setting = settingsMap.get(userId);
                    return setting != null ? setting.getNotificationPreviewMode() : PREVIEW_MODE_NAME_AND_MESSAGE;
                }));

        // 각 미리보기 모드 그룹별로 알림 전송
        int totalSentCount = 0;
        int totalTokenCount = 0;

        for (Map.Entry<String, List<Long>> entry : usersByPreviewMode.entrySet()) {
            String previewMode = entry.getKey();
            List<Long> userIds = entry.getValue();

            List<DeviceToken> tokens = deviceTokenRepository.findActiveByUserIds(userIds);
            if (tokens.isEmpty()) {
                log.debug("No active device tokens for users with previewMode {}: {}", previewMode, userIds);
                continue;
            }

            List<String> tokenStrings = tokens.stream()
                    .map(DeviceToken::getToken)
                    .toList();

            String title = resolveTitle(senderNickname, previewMode);
            String body = resolveBody(messageContent, previewMode);

            // data에 title/body도 포함하여 클라이언트가 notification 필드 없이도
            // 포그라운드 알림을 구성할 수 있도록 함 (일부 기기에서 notification이 null일 수 있음)
            Map<String, String> data = new HashMap<>();
            data.put("type", "NEW_MESSAGE");
            data.put("chatRoomId", chatRoomId.toString());
            data.put("title", title);
            data.put("body", body);
            if (senderAvatarUrl != null) {
                data.put("avatarUrl", senderAvatarUrl);
            }

            int sentCount = pushNotificationSender.sendMultiple(tokenStrings, title, body, data, senderAvatarUrl);
            totalSentCount += sentCount;
            totalTokenCount += tokenStrings.size();

            log.debug("Sent bulk notification to {} users (previewMode: {}): {}/{} devices",
                    userIds.size(), previewMode, sentCount, tokenStrings.size());
        }

        log.info("Bulk new message push sent to {} users (filtered from {}): {}/{} devices",
                allowedUserIds.size(), receiverUserIds.size(), totalSentCount, totalTokenCount);
    }

    /**
     * 친구 요청 알림을 전송한다.
     * 수신자의 알림 설정을 확인하여 친구 요청 알림이 꺼져 있거나 방해 금지 시간대인 경우 전송하지 않는다.
     *
     * @param receiverUserId 알림을 받을 사용자 ID
     * @param senderNickname 친구 요청을 보낸 사용자 닉네임
     */
    @Override
    @Async
    public void sendFriendRequestNotification(Long receiverUserId, String senderNickname) {
        NotificationSetting setting = notificationSettingRepository.findByUserId(receiverUserId)
                .orElse(null);

        if (!isFriendRequestNotificationAllowed(setting)) {
            log.debug("Friend request notification disabled for user: {}", receiverUserId);
            return;
        }

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

        int sentCount = pushNotificationSender.sendMultiple(tokenStrings, title, body, data, null);
        log.info("Friend request push sent to user {}: {}/{} devices", receiverUserId, sentCount, tokenStrings.size());
    }

    /**
     * 메시지 알림이 허용되는지 확인한다.
     * 설정이 없으면 기본값(허용)으로 처리한다.
     *
     * @param setting 알림 설정 (null 가능)
     * @return 메시지 알림이 허용되면 {@code true}
     */
    private boolean isMessageNotificationAllowed(NotificationSetting setting) {
        return isMessageNotificationAllowed(setting, LocalTime.now());
    }

    /**
     * 메시지 알림이 허용되는지 확인한다.
     * 설정이 없으면 기본값(허용)으로 처리한다.
     *
     * @param setting 알림 설정 (null 가능)
     * @param now     현재 시각
     * @return 메시지 알림이 허용되면 {@code true}
     */
    private boolean isMessageNotificationAllowed(NotificationSetting setting, LocalTime now) {
        if (setting == null) {
            return true;
        }
        if (!setting.isMessageNotification()) {
            return false;
        }
        return !setting.isInDoNotDisturbTime(now);
    }

    /**
     * 친구 요청 알림이 허용되는지 확인한다.
     * 설정이 없으면 기본값(허용)으로 처리한다.
     *
     * @param setting 알림 설정 (null 가능)
     * @return 친구 요청 알림이 허용되면 {@code true}
     */
    private boolean isFriendRequestNotificationAllowed(NotificationSetting setting) {
        if (setting == null) {
            return true;
        }
        if (!setting.isFriendRequestNotification()) {
            return false;
        }
        return !setting.isInDoNotDisturbTime(LocalTime.now());
    }

    /**
     * 미리보기 모드에 따라 알림 제목을 결정한다.
     *
     * @param senderNickname 발신자 닉네임
     * @param previewMode    미리보기 모드
     * @return 알림 제목
     */
    private String resolveTitle(String senderNickname, String previewMode) {
        return switch (previewMode) {
            case PREVIEW_MODE_NAME_AND_MESSAGE, PREVIEW_MODE_NAME_ONLY -> senderNickname;
            default -> APP_NAME;
        };
    }

    /**
     * 미리보기 모드에 따라 알림 본문을 결정한다.
     *
     * @param messageContent 원본 메시지 내용
     * @param previewMode    미리보기 모드
     * @return 알림 본문
     */
    private String resolveBody(String messageContent, String previewMode) {
        if (PREVIEW_MODE_NAME_AND_MESSAGE.equals(previewMode)) {
            return truncateMessage(messageContent);
        }
        return DEFAULT_MESSAGE_BODY;
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
