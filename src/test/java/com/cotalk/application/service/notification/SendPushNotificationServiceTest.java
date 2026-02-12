package com.cotalk.application.service.notification;

import com.cotalk.domain.entity.DeviceToken;
import com.cotalk.domain.entity.NotificationSetting;
import com.cotalk.domain.port.outbound.DeviceTokenRepository;
import com.cotalk.domain.port.outbound.NotificationSettingRepository;
import com.cotalk.domain.port.outbound.PushNotificationSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SendPushNotificationServiceTest {

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    @Mock
    private PushNotificationSender pushNotificationSender;

    @Mock
    private NotificationSettingRepository notificationSettingRepository;

    private SendPushNotificationService sendPushNotificationService;

    @BeforeEach
    void setUp() {
        sendPushNotificationService = new SendPushNotificationService(
                deviceTokenRepository, pushNotificationSender, notificationSettingRepository);
    }

    private NotificationSetting createDefaultSetting(Long userId) {
        return NotificationSetting.builder()
                .id(userId)
                .userId(userId)
                .messageNotification(true)
                .friendRequestNotification(true)
                .groupInviteNotification(true)
                .notificationPreviewMode("NAME_AND_MESSAGE")
                .soundEnabled(true)
                .vibrationEnabled(true)
                .doNotDisturbEnabled(false)
                .build();
    }

    @Nested
    @DisplayName("새 메시지 알림 전송")
    class SendNewMessageNotification {

        @Test
        @DisplayName("활성 토큰이 있으면 푸시 알림 전송")
        void should_sendPush_when_activeTokensExist() {
            // given
            Long receiverUserId = 1L;
            String senderNickname = "친구";
            String messageContent = "안녕하세요!";
            Long chatRoomId = 100L;

            DeviceToken token = DeviceToken.builder()
                    .id(1L)
                    .userId(receiverUserId)
                    .token("fcm-token-123")
                    .deviceType(DeviceToken.DeviceType.ANDROID)
                    .build();

            given(notificationSettingRepository.findByUserId(receiverUserId))
                    .willReturn(Optional.of(createDefaultSetting(receiverUserId)));
            given(deviceTokenRepository.findActiveByUserId(receiverUserId)).willReturn(List.of(token));
            given(pushNotificationSender.sendMultiple(anyList(), anyString(), anyString(), anyMap(), nullable(String.class))).willReturn(1);

            // when
            sendPushNotificationService.sendNewMessageNotification(receiverUserId, senderNickname, messageContent, chatRoomId, null);

            // then
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<String>> tokensCaptor = ArgumentCaptor.forClass(List.class);
            ArgumentCaptor<String> titleCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, String>> dataCaptor = ArgumentCaptor.forClass(Map.class);

            verify(pushNotificationSender).sendMultiple(
                    tokensCaptor.capture(),
                    titleCaptor.capture(),
                    bodyCaptor.capture(),
                    dataCaptor.capture(),
                    nullable(String.class)
            );

            assertThat(tokensCaptor.getValue()).containsExactly("fcm-token-123");
            assertThat(titleCaptor.getValue()).isEqualTo("친구");
            assertThat(bodyCaptor.getValue()).isEqualTo("안녕하세요!");
            assertThat(dataCaptor.getValue()).containsEntry("chatRoomId", "100");
            assertThat(dataCaptor.getValue()).containsEntry("type", "NEW_MESSAGE");
        }

        @Test
        @DisplayName("활성 토큰이 없으면 푸시 알림 전송하지 않음")
        void should_notSendPush_when_noActiveTokens() {
            // given
            Long receiverUserId = 1L;
            given(notificationSettingRepository.findByUserId(receiverUserId))
                    .willReturn(Optional.of(createDefaultSetting(receiverUserId)));
            given(deviceTokenRepository.findActiveByUserId(receiverUserId)).willReturn(List.of());

            // when
            sendPushNotificationService.sendNewMessageNotification(receiverUserId, "친구", "안녕!", 100L, null);

            // then
            verify(pushNotificationSender, never()).sendMultiple(anyList(), anyString(), anyString(), anyMap(), any());
        }

        @Test
        @DisplayName("긴 메시지는 미리보기로 잘림")
        void should_truncateMessage_when_tooLong() {
            // given
            Long receiverUserId = 1L;
            String longMessage = "이것은 매우 긴 메시지입니다. ".repeat(10);

            DeviceToken token = DeviceToken.builder()
                    .id(1L)
                    .userId(receiverUserId)
                    .token("fcm-token")
                    .deviceType(DeviceToken.DeviceType.ANDROID)
                    .build();

            given(notificationSettingRepository.findByUserId(receiverUserId))
                    .willReturn(Optional.of(createDefaultSetting(receiverUserId)));
            given(deviceTokenRepository.findActiveByUserId(receiverUserId)).willReturn(List.of(token));
            given(pushNotificationSender.sendMultiple(anyList(), anyString(), anyString(), anyMap(), nullable(String.class))).willReturn(1);

            // when
            sendPushNotificationService.sendNewMessageNotification(receiverUserId, "친구", longMessage, 100L, null);

            // then
            ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
            verify(pushNotificationSender).sendMultiple(anyList(), anyString(), bodyCaptor.capture(), anyMap(), nullable(String.class));

            assertThat(bodyCaptor.getValue().length()).isLessThanOrEqualTo(103); // 100 + "..."
        }

        @Test
        @DisplayName("null 메시지는 빈 문자열로 변환됨")
        void should_handleNullMessage_when_sending() {
            // given
            Long receiverUserId = 1L;

            DeviceToken token = DeviceToken.builder()
                    .id(1L)
                    .userId(receiverUserId)
                    .token("fcm-token")
                    .deviceType(DeviceToken.DeviceType.ANDROID)
                    .build();

            given(notificationSettingRepository.findByUserId(receiverUserId))
                    .willReturn(Optional.of(createDefaultSetting(receiverUserId)));
            given(deviceTokenRepository.findActiveByUserId(receiverUserId)).willReturn(List.of(token));
            given(pushNotificationSender.sendMultiple(anyList(), anyString(), anyString(), anyMap(), nullable(String.class))).willReturn(1);

            // when
            sendPushNotificationService.sendNewMessageNotification(receiverUserId, "친구", null, 100L, null);

            // then
            ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
            verify(pushNotificationSender).sendMultiple(anyList(), anyString(), bodyCaptor.capture(), anyMap(), nullable(String.class));

            assertThat(bodyCaptor.getValue()).isEmpty();
        }

        @Test
        @DisplayName("아바타 URL이 있으면 data에 avatarUrl이 포함된다")
        void should_includeAvatarUrl_when_avatarUrlProvided() {
            // given
            Long receiverUserId = 1L;
            String avatarUrl = "https://example.com/avatar.jpg";

            DeviceToken token = DeviceToken.builder()
                    .id(1L)
                    .userId(receiverUserId)
                    .token("fcm-token")
                    .deviceType(DeviceToken.DeviceType.ANDROID)
                    .build();

            given(notificationSettingRepository.findByUserId(receiverUserId))
                    .willReturn(Optional.of(createDefaultSetting(receiverUserId)));
            given(deviceTokenRepository.findActiveByUserId(receiverUserId)).willReturn(List.of(token));
            given(pushNotificationSender.sendMultiple(anyList(), anyString(), anyString(), anyMap(), eq(avatarUrl))).willReturn(1);

            // when
            sendPushNotificationService.sendNewMessageNotification(receiverUserId, "친구", "안녕!", 100L, avatarUrl);

            // then
            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, String>> dataCaptor = ArgumentCaptor.forClass(Map.class);
            verify(pushNotificationSender).sendMultiple(anyList(), anyString(), anyString(), dataCaptor.capture(), eq(avatarUrl));

            assertThat(dataCaptor.getValue()).containsEntry("avatarUrl", avatarUrl);
        }

        @Test
        @DisplayName("아바타 URL이 null이면 data에 avatarUrl이 포함되지 않는다")
        void should_notIncludeAvatarUrl_when_avatarUrlIsNull() {
            // given
            Long receiverUserId = 1L;

            DeviceToken token = DeviceToken.builder()
                    .id(1L)
                    .userId(receiverUserId)
                    .token("fcm-token")
                    .deviceType(DeviceToken.DeviceType.ANDROID)
                    .build();

            given(notificationSettingRepository.findByUserId(receiverUserId))
                    .willReturn(Optional.of(createDefaultSetting(receiverUserId)));
            given(deviceTokenRepository.findActiveByUserId(receiverUserId)).willReturn(List.of(token));
            given(pushNotificationSender.sendMultiple(anyList(), anyString(), anyString(), anyMap(), isNull())).willReturn(1);

            // when
            sendPushNotificationService.sendNewMessageNotification(receiverUserId, "친구", "안녕!", 100L, null);

            // then
            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, String>> dataCaptor = ArgumentCaptor.forClass(Map.class);
            verify(pushNotificationSender).sendMultiple(anyList(), anyString(), anyString(), dataCaptor.capture(), isNull());

            assertThat(dataCaptor.getValue()).doesNotContainKey("avatarUrl");
        }

        @Test
        @DisplayName("메시지 알림이 꺼져 있으면 푸시 알림을 전송하지 않는다")
        void should_notSendPush_when_messageNotificationDisabled() {
            // given
            Long receiverUserId = 1L;
            NotificationSetting setting = NotificationSetting.builder()
                    .id(1L).userId(receiverUserId)
                    .messageNotification(false)
                    .friendRequestNotification(true)
                    .doNotDisturbEnabled(false)
                    .build();

            given(notificationSettingRepository.findByUserId(receiverUserId))
                    .willReturn(Optional.of(setting));

            // when
            sendPushNotificationService.sendNewMessageNotification(receiverUserId, "친구", "안녕!", 100L, null);

            // then
            verify(deviceTokenRepository, never()).findActiveByUserId(anyLong());
            verify(pushNotificationSender, never()).sendMultiple(anyList(), anyString(), anyString(), anyMap(), any());
        }

        @Test
        @DisplayName("방해 금지 시간대이면 푸시 알림을 전송하지 않는다")
        void should_notSendPush_when_inDoNotDisturbTime() {
            // given
            Long receiverUserId = 1L;
            NotificationSetting setting = NotificationSetting.builder()
                    .id(1L).userId(receiverUserId)
                    .messageNotification(true)
                    .doNotDisturbEnabled(true)
                    .doNotDisturbStart("00:00")
                    .doNotDisturbEnd("23:59")
                    .build();

            given(notificationSettingRepository.findByUserId(receiverUserId))
                    .willReturn(Optional.of(setting));

            // when
            sendPushNotificationService.sendNewMessageNotification(receiverUserId, "친구", "안녕!", 100L, null);

            // then
            verify(deviceTokenRepository, never()).findActiveByUserId(anyLong());
            verify(pushNotificationSender, never()).sendMultiple(anyList(), anyString(), anyString(), anyMap(), any());
        }

        @Test
        @DisplayName("알림 설정이 없는 유저는 기본값(알림 허용)으로 처리한다")
        void should_sendPush_when_noNotificationSettingExists() {
            // given
            Long receiverUserId = 1L;
            DeviceToken token = DeviceToken.builder()
                    .id(1L).userId(receiverUserId).token("fcm-token")
                    .deviceType(DeviceToken.DeviceType.ANDROID).build();

            given(notificationSettingRepository.findByUserId(receiverUserId))
                    .willReturn(Optional.empty());
            given(deviceTokenRepository.findActiveByUserId(receiverUserId)).willReturn(List.of(token));
            given(pushNotificationSender.sendMultiple(anyList(), anyString(), anyString(), anyMap(), nullable(String.class))).willReturn(1);

            // when
            sendPushNotificationService.sendNewMessageNotification(receiverUserId, "친구", "안녕!", 100L, null);

            // then
            verify(pushNotificationSender).sendMultiple(anyList(), anyString(), anyString(), anyMap(), nullable(String.class));
        }

        @Test
        @DisplayName("미리보기 모드가 NAME_ONLY이면 메시지 내용 대신 안내 문구를 전송한다")
        void should_sendGenericBody_when_previewModeIsNameOnly() {
            // given
            Long receiverUserId = 1L;
            NotificationSetting setting = NotificationSetting.builder()
                    .id(1L).userId(receiverUserId)
                    .messageNotification(true)
                    .notificationPreviewMode("NAME_ONLY")
                    .doNotDisturbEnabled(false)
                    .build();
            DeviceToken token = DeviceToken.builder()
                    .id(1L).userId(receiverUserId).token("fcm-token")
                    .deviceType(DeviceToken.DeviceType.ANDROID).build();

            given(notificationSettingRepository.findByUserId(receiverUserId))
                    .willReturn(Optional.of(setting));
            given(deviceTokenRepository.findActiveByUserId(receiverUserId)).willReturn(List.of(token));
            given(pushNotificationSender.sendMultiple(anyList(), anyString(), anyString(), anyMap(), nullable(String.class))).willReturn(1);

            // when
            sendPushNotificationService.sendNewMessageNotification(receiverUserId, "친구", "비밀 메시지", 100L, null);

            // then
            ArgumentCaptor<String> titleCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
            verify(pushNotificationSender).sendMultiple(anyList(), titleCaptor.capture(), bodyCaptor.capture(), anyMap(), nullable(String.class));

            assertThat(titleCaptor.getValue()).isEqualTo("친구");
            assertThat(bodyCaptor.getValue()).isEqualTo("새 메시지가 도착했습니다.");
        }

        @Test
        @DisplayName("미리보기 모드가 NOTHING이면 발신자 이름과 메시지 내용 모두 숨긴다")
        void should_sendAnonymousNotification_when_previewModeIsNothing() {
            // given
            Long receiverUserId = 1L;
            NotificationSetting setting = NotificationSetting.builder()
                    .id(1L).userId(receiverUserId)
                    .messageNotification(true)
                    .notificationPreviewMode("NOTHING")
                    .doNotDisturbEnabled(false)
                    .build();
            DeviceToken token = DeviceToken.builder()
                    .id(1L).userId(receiverUserId).token("fcm-token")
                    .deviceType(DeviceToken.DeviceType.ANDROID).build();

            given(notificationSettingRepository.findByUserId(receiverUserId))
                    .willReturn(Optional.of(setting));
            given(deviceTokenRepository.findActiveByUserId(receiverUserId)).willReturn(List.of(token));
            given(pushNotificationSender.sendMultiple(anyList(), anyString(), anyString(), anyMap(), nullable(String.class))).willReturn(1);

            // when
            sendPushNotificationService.sendNewMessageNotification(receiverUserId, "친구", "비밀 메시지", 100L, null);

            // then
            ArgumentCaptor<String> titleCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
            verify(pushNotificationSender).sendMultiple(anyList(), titleCaptor.capture(), bodyCaptor.capture(), anyMap(), nullable(String.class));

            assertThat(titleCaptor.getValue()).isEqualTo("Co-Talk");
            assertThat(bodyCaptor.getValue()).isEqualTo("새 메시지가 도착했습니다.");
        }
    }

    @Nested
    @DisplayName("친구 요청 알림 전송")
    class SendFriendRequestNotification {

        @Test
        @DisplayName("친구 요청 알림 전송 성공")
        void should_sendFriendRequestNotification() {
            // given
            Long receiverUserId = 1L;
            String senderNickname = "새친구";

            DeviceToken token = DeviceToken.builder()
                    .id(1L)
                    .userId(receiverUserId)
                    .token("fcm-token")
                    .deviceType(DeviceToken.DeviceType.IOS)
                    .build();

            given(notificationSettingRepository.findByUserId(receiverUserId))
                    .willReturn(Optional.of(createDefaultSetting(receiverUserId)));
            given(deviceTokenRepository.findActiveByUserId(receiverUserId)).willReturn(List.of(token));
            given(pushNotificationSender.sendMultiple(anyList(), anyString(), anyString(), anyMap(), nullable(String.class))).willReturn(1);

            // when
            sendPushNotificationService.sendFriendRequestNotification(receiverUserId, senderNickname);

            // then
            ArgumentCaptor<String> titleCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, String>> dataCaptor = ArgumentCaptor.forClass(Map.class);

            verify(pushNotificationSender).sendMultiple(anyList(), titleCaptor.capture(), bodyCaptor.capture(), dataCaptor.capture(), nullable(String.class));

            assertThat(titleCaptor.getValue()).isEqualTo("친구 요청");
            assertThat(bodyCaptor.getValue()).isEqualTo("새친구님이 친구 요청을 보냈습니다.");
            assertThat(dataCaptor.getValue()).containsEntry("type", "FRIEND_REQUEST");
        }

        @Test
        @DisplayName("활성 토큰이 없으면 친구 요청 알림 전송하지 않음")
        void should_notSendFriendRequestNotification_when_noActiveTokens() {
            // given
            Long receiverUserId = 1L;
            given(notificationSettingRepository.findByUserId(receiverUserId))
                    .willReturn(Optional.of(createDefaultSetting(receiverUserId)));
            given(deviceTokenRepository.findActiveByUserId(receiverUserId)).willReturn(List.of());

            // when
            sendPushNotificationService.sendFriendRequestNotification(receiverUserId, "친구");

            // then
            verify(pushNotificationSender, never()).sendMultiple(anyList(), anyString(), anyString(), anyMap(), any());
        }

        @Test
        @DisplayName("친구 요청 알림이 꺼져 있으면 푸시 알림을 전송하지 않는다")
        void should_notSendPush_when_friendRequestNotificationDisabled() {
            // given
            Long receiverUserId = 1L;
            NotificationSetting setting = NotificationSetting.builder()
                    .id(1L).userId(receiverUserId)
                    .messageNotification(true)
                    .friendRequestNotification(false)
                    .doNotDisturbEnabled(false)
                    .build();

            given(notificationSettingRepository.findByUserId(receiverUserId))
                    .willReturn(Optional.of(setting));

            // when
            sendPushNotificationService.sendFriendRequestNotification(receiverUserId, "새친구");

            // then
            verify(deviceTokenRepository, never()).findActiveByUserId(anyLong());
            verify(pushNotificationSender, never()).sendMultiple(anyList(), anyString(), anyString(), anyMap(), any());
        }

        @Test
        @DisplayName("방해 금지 시간대이면 친구 요청 알림을 전송하지 않는다")
        void should_notSendFriendRequestPush_when_inDoNotDisturbTime() {
            // given
            Long receiverUserId = 1L;
            NotificationSetting setting = NotificationSetting.builder()
                    .id(1L).userId(receiverUserId)
                    .friendRequestNotification(true)
                    .doNotDisturbEnabled(true)
                    .doNotDisturbStart("00:00")
                    .doNotDisturbEnd("23:59")
                    .build();

            given(notificationSettingRepository.findByUserId(receiverUserId))
                    .willReturn(Optional.of(setting));

            // when
            sendPushNotificationService.sendFriendRequestNotification(receiverUserId, "새친구");

            // then
            verify(deviceTokenRepository, never()).findActiveByUserId(anyLong());
            verify(pushNotificationSender, never()).sendMultiple(anyList(), anyString(), anyString(), anyMap(), any());
        }
    }

    @Nested
    @DisplayName("벌크 메시지 알림 전송")
    class SendNewMessageNotificationBulk {

        @Test
        @DisplayName("여러 사용자에게 알림 전송 성공")
        void should_sendBulkPush_when_multipleUsersHaveTokens() {
            // given
            List<Long> receiverUserIds = List.of(1L, 2L, 3L);
            String senderNickname = "발신자";
            String messageContent = "안녕하세요!";
            Long chatRoomId = 100L;

            given(notificationSettingRepository.findByUserIds(receiverUserIds))
                    .willReturn(List.of(
                            createDefaultSetting(1L),
                            createDefaultSetting(2L),
                            createDefaultSetting(3L)));

            DeviceToken token1 = DeviceToken.builder()
                    .id(1L).userId(1L).token("token1").deviceType(DeviceToken.DeviceType.ANDROID).build();
            DeviceToken token2 = DeviceToken.builder()
                    .id(2L).userId(2L).token("token2").deviceType(DeviceToken.DeviceType.IOS).build();
            DeviceToken token3 = DeviceToken.builder()
                    .id(3L).userId(3L).token("token3").deviceType(DeviceToken.DeviceType.ANDROID).build();

            given(deviceTokenRepository.findActiveByUserIds(receiverUserIds))
                    .willReturn(List.of(token1, token2, token3));
            given(pushNotificationSender.sendMultiple(anyList(), anyString(), anyString(), anyMap(), any())).willReturn(3);

            // when
            sendPushNotificationService.sendNewMessageNotificationBulk(receiverUserIds, senderNickname, messageContent, chatRoomId, null);

            // then
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<String>> tokensCaptor = ArgumentCaptor.forClass(List.class);
            verify(pushNotificationSender).sendMultiple(tokensCaptor.capture(), anyString(), anyString(), anyMap(), any());

            assertThat(tokensCaptor.getValue()).containsExactly("token1", "token2", "token3");
        }

        @Test
        @DisplayName("빈 수신자 목록이면 알림 전송하지 않음")
        void should_notSendBulkPush_when_emptyReceiverList() {
            // given
            List<Long> receiverUserIds = List.of();

            // when
            sendPushNotificationService.sendNewMessageNotificationBulk(receiverUserIds, "발신자", "메시지", 100L, null);

            // then
            verify(deviceTokenRepository, never()).findActiveByUserIds(anyList());
            verify(pushNotificationSender, never()).sendMultiple(anyList(), anyString(), anyString(), anyMap(), any());
        }

        @Test
        @DisplayName("활성 토큰이 없으면 알림 전송하지 않음")
        void should_notSendBulkPush_when_noActiveTokens() {
            // given
            List<Long> receiverUserIds = List.of(1L, 2L);

            given(notificationSettingRepository.findByUserIds(receiverUserIds))
                    .willReturn(List.of(createDefaultSetting(1L), createDefaultSetting(2L)));
            given(deviceTokenRepository.findActiveByUserIds(receiverUserIds)).willReturn(List.of());

            // when
            sendPushNotificationService.sendNewMessageNotificationBulk(receiverUserIds, "발신자", "메시지", 100L, null);

            // then
            verify(pushNotificationSender, never()).sendMultiple(anyList(), anyString(), anyString(), anyMap(), any());
        }

        @Test
        @DisplayName("긴 메시지는 미리보기로 잘림")
        void should_truncateMessage_when_bulkSendingLongMessage() {
            // given
            List<Long> receiverUserIds = List.of(1L);
            String longMessage = "이것은 매우 긴 메시지입니다. ".repeat(10);

            given(notificationSettingRepository.findByUserIds(receiverUserIds))
                    .willReturn(List.of(createDefaultSetting(1L)));

            DeviceToken token = DeviceToken.builder()
                    .id(1L).userId(1L).token("token").deviceType(DeviceToken.DeviceType.ANDROID).build();

            given(deviceTokenRepository.findActiveByUserIds(receiverUserIds)).willReturn(List.of(token));
            given(pushNotificationSender.sendMultiple(anyList(), anyString(), anyString(), anyMap(), nullable(String.class))).willReturn(1);

            // when
            sendPushNotificationService.sendNewMessageNotificationBulk(receiverUserIds, "발신자", longMessage, 100L, null);

            // then
            ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
            verify(pushNotificationSender).sendMultiple(anyList(), anyString(), bodyCaptor.capture(), anyMap(), nullable(String.class));

            assertThat(bodyCaptor.getValue()).endsWith("...");
            assertThat(bodyCaptor.getValue().length()).isLessThanOrEqualTo(103);
        }

        @Test
        @DisplayName("null 메시지는 빈 문자열로 변환됨")
        void should_handleNullMessage_when_bulkSending() {
            // given
            List<Long> receiverUserIds = List.of(1L);

            given(notificationSettingRepository.findByUserIds(receiverUserIds))
                    .willReturn(List.of(createDefaultSetting(1L)));

            DeviceToken token = DeviceToken.builder()
                    .id(1L).userId(1L).token("token").deviceType(DeviceToken.DeviceType.ANDROID).build();

            given(deviceTokenRepository.findActiveByUserIds(receiverUserIds)).willReturn(List.of(token));
            given(pushNotificationSender.sendMultiple(anyList(), anyString(), anyString(), anyMap(), nullable(String.class))).willReturn(1);

            // when
            sendPushNotificationService.sendNewMessageNotificationBulk(receiverUserIds, "발신자", null, 100L, null);

            // then
            ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
            verify(pushNotificationSender).sendMultiple(anyList(), anyString(), bodyCaptor.capture(), anyMap(), nullable(String.class));

            assertThat(bodyCaptor.getValue()).isEmpty();
        }

        @Test
        @DisplayName("메시지 알림이 꺼진 유저는 벌크 전송에서 제외한다")
        void should_excludeUsersWithMessageNotificationDisabled_when_bulkSending() {
            // given
            List<Long> receiverUserIds = List.of(1L, 2L, 3L);

            NotificationSetting disabledSetting = NotificationSetting.builder()
                    .id(2L).userId(2L)
                    .messageNotification(false)
                    .doNotDisturbEnabled(false)
                    .build();

            given(notificationSettingRepository.findByUserIds(receiverUserIds))
                    .willReturn(List.of(
                            createDefaultSetting(1L),
                            disabledSetting,
                            createDefaultSetting(3L)));

            DeviceToken token1 = DeviceToken.builder()
                    .id(1L).userId(1L).token("token1").deviceType(DeviceToken.DeviceType.ANDROID).build();
            DeviceToken token3 = DeviceToken.builder()
                    .id(3L).userId(3L).token("token3").deviceType(DeviceToken.DeviceType.ANDROID).build();

            given(deviceTokenRepository.findActiveByUserIds(List.of(1L, 3L)))
                    .willReturn(List.of(token1, token3));
            given(pushNotificationSender.sendMultiple(anyList(), anyString(), anyString(), anyMap(), any())).willReturn(2);

            // when
            sendPushNotificationService.sendNewMessageNotificationBulk(receiverUserIds, "발신자", "안녕!", 100L, null);

            // then
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<String>> tokensCaptor = ArgumentCaptor.forClass(List.class);
            verify(pushNotificationSender).sendMultiple(tokensCaptor.capture(), anyString(), anyString(), anyMap(), any());

            assertThat(tokensCaptor.getValue()).containsExactly("token1", "token3");
        }

        @Test
        @DisplayName("방해 금지 시간대인 유저는 벌크 전송에서 제외한다")
        void should_excludeUsersInDoNotDisturbTime_when_bulkSending() {
            // given
            List<Long> receiverUserIds = List.of(1L, 2L);

            NotificationSetting dndSetting = NotificationSetting.builder()
                    .id(2L).userId(2L)
                    .messageNotification(true)
                    .doNotDisturbEnabled(true)
                    .doNotDisturbStart("00:00")
                    .doNotDisturbEnd("23:59")
                    .build();

            given(notificationSettingRepository.findByUserIds(receiverUserIds))
                    .willReturn(List.of(createDefaultSetting(1L), dndSetting));

            DeviceToken token1 = DeviceToken.builder()
                    .id(1L).userId(1L).token("token1").deviceType(DeviceToken.DeviceType.ANDROID).build();

            given(deviceTokenRepository.findActiveByUserIds(List.of(1L)))
                    .willReturn(List.of(token1));
            given(pushNotificationSender.sendMultiple(anyList(), anyString(), anyString(), anyMap(), any())).willReturn(1);

            // when
            sendPushNotificationService.sendNewMessageNotificationBulk(receiverUserIds, "발신자", "안녕!", 100L, null);

            // then
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<String>> tokensCaptor = ArgumentCaptor.forClass(List.class);
            verify(pushNotificationSender).sendMultiple(tokensCaptor.capture(), anyString(), anyString(), anyMap(), any());

            assertThat(tokensCaptor.getValue()).containsExactly("token1");
        }

        @Test
        @DisplayName("모든 수신자의 알림이 꺼져 있으면 전송하지 않는다")
        void should_notSendBulkPush_when_allUsersDisabledNotification() {
            // given
            List<Long> receiverUserIds = List.of(1L, 2L);

            NotificationSetting disabled1 = NotificationSetting.builder()
                    .id(1L).userId(1L).messageNotification(false).doNotDisturbEnabled(false).build();
            NotificationSetting disabled2 = NotificationSetting.builder()
                    .id(2L).userId(2L).messageNotification(false).doNotDisturbEnabled(false).build();

            given(notificationSettingRepository.findByUserIds(receiverUserIds))
                    .willReturn(List.of(disabled1, disabled2));

            // when
            sendPushNotificationService.sendNewMessageNotificationBulk(receiverUserIds, "발신자", "안녕!", 100L, null);

            // then
            verify(deviceTokenRepository, never()).findActiveByUserIds(anyList());
            verify(pushNotificationSender, never()).sendMultiple(anyList(), anyString(), anyString(), anyMap(), any());
        }

        @Test
        @DisplayName("알림 설정이 없는 유저는 기본값(알림 허용)으로 벌크 전송에 포함한다")
        void should_includeUsersWithNoSettings_when_bulkSending() {
            // given
            List<Long> receiverUserIds = List.of(1L, 2L);

            // userId 2L은 설정이 없음 → 기본값으로 허용
            given(notificationSettingRepository.findByUserIds(receiverUserIds))
                    .willReturn(List.of(createDefaultSetting(1L)));

            DeviceToken token1 = DeviceToken.builder()
                    .id(1L).userId(1L).token("token1").deviceType(DeviceToken.DeviceType.ANDROID).build();
            DeviceToken token2 = DeviceToken.builder()
                    .id(2L).userId(2L).token("token2").deviceType(DeviceToken.DeviceType.ANDROID).build();

            given(deviceTokenRepository.findActiveByUserIds(receiverUserIds))
                    .willReturn(List.of(token1, token2));
            given(pushNotificationSender.sendMultiple(anyList(), anyString(), anyString(), anyMap(), any())).willReturn(2);

            // when
            sendPushNotificationService.sendNewMessageNotificationBulk(receiverUserIds, "발신자", "안녕!", 100L, null);

            // then
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<String>> tokensCaptor = ArgumentCaptor.forClass(List.class);
            verify(pushNotificationSender).sendMultiple(tokensCaptor.capture(), anyString(), anyString(), anyMap(), any());

            assertThat(tokensCaptor.getValue()).containsExactly("token1", "token2");
        }
    }
}
