package com.cotalk.application.service.notification;

import com.cotalk.domain.entity.DeviceToken;
import com.cotalk.domain.port.outbound.DeviceTokenRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SendPushNotificationServiceTest {

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    @Mock
    private PushNotificationSender pushNotificationSender;

    private SendPushNotificationService sendPushNotificationService;

    @BeforeEach
    void setUp() {
        sendPushNotificationService = new SendPushNotificationService(deviceTokenRepository, pushNotificationSender);
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

            given(deviceTokenRepository.findActiveByUserId(receiverUserId)).willReturn(List.of(token));
            given(pushNotificationSender.sendMultiple(anyList(), anyString(), anyString(), anyMap())).willReturn(1);

            // when
            sendPushNotificationService.sendNewMessageNotification(receiverUserId, senderNickname, messageContent, chatRoomId);

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
                    dataCaptor.capture()
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
            given(deviceTokenRepository.findActiveByUserId(receiverUserId)).willReturn(List.of());

            // when
            sendPushNotificationService.sendNewMessageNotification(receiverUserId, "친구", "안녕!", 100L);

            // then
            verify(pushNotificationSender, never()).sendMultiple(anyList(), anyString(), anyString(), anyMap());
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

            given(deviceTokenRepository.findActiveByUserId(receiverUserId)).willReturn(List.of(token));
            given(pushNotificationSender.sendMultiple(anyList(), anyString(), anyString(), anyMap())).willReturn(1);

            // when
            sendPushNotificationService.sendNewMessageNotification(receiverUserId, "친구", longMessage, 100L);

            // then
            ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
            verify(pushNotificationSender).sendMultiple(anyList(), anyString(), bodyCaptor.capture(), anyMap());

            assertThat(bodyCaptor.getValue().length()).isLessThanOrEqualTo(103); // 100 + "..."
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

            given(deviceTokenRepository.findActiveByUserId(receiverUserId)).willReturn(List.of(token));
            given(pushNotificationSender.sendMultiple(anyList(), anyString(), anyString(), anyMap())).willReturn(1);

            // when
            sendPushNotificationService.sendFriendRequestNotification(receiverUserId, senderNickname);

            // then
            ArgumentCaptor<String> titleCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, String>> dataCaptor = ArgumentCaptor.forClass(Map.class);

            verify(pushNotificationSender).sendMultiple(anyList(), titleCaptor.capture(), bodyCaptor.capture(), dataCaptor.capture());

            assertThat(titleCaptor.getValue()).isEqualTo("친구 요청");
            assertThat(bodyCaptor.getValue()).isEqualTo("새친구님이 친구 요청을 보냈습니다.");
            assertThat(dataCaptor.getValue()).containsEntry("type", "FRIEND_REQUEST");
        }
    }
}
