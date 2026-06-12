package com.cotalk.infrastructure.push;

import com.cotalk.domain.port.outbound.PushNotificationSender.PushTarget;
import com.google.firebase.messaging.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FcmPushNotificationSender")
class FcmPushNotificationSenderTest {

    @Mock
    private FirebaseMessaging firebaseMessaging;

    @Mock
    private InvalidTokenDeactivator invalidTokenDeactivator;

    private FcmPushNotificationSender sender;

    @BeforeEach
    void setUp() {
        sender = new FcmPushNotificationSender(firebaseMessaging, invalidTokenDeactivator);
    }

    @Nested
    @DisplayName("단일 메시지 전송 시")
    class SendSingleMessage {

        @Test
        @DisplayName("Firebase가 설정되지 않으면 false를 반환한다")
        void should_ReturnFalse_when_FirebaseNotConfigured() {
            // given
            FcmPushNotificationSender senderWithoutFirebase =
                    new FcmPushNotificationSender(null, invalidTokenDeactivator);

            // when
            boolean result = senderWithoutFirebase.send("token", "title", "body", Map.of(), null);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("정상적인 토큰으로 전송 시 true를 반환한다")
        void should_ReturnTrue_when_SendSucceeds() throws FirebaseMessagingException {
            // given
            String token = "valid-token";
            String title = "제목";
            String body = "본문";
            Map<String, String> data = Map.of("key", "value");

            given(firebaseMessaging.send(any(Message.class))).willReturn("message-id-123");

            // when
            boolean result = sender.send(token, title, body, data, null);

            // then
            assertThat(result).isTrue();

            ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
            verify(firebaseMessaging).send(messageCaptor.capture());
        }

        @Test
        @DisplayName("UNREGISTERED 에러 시 토큰 비활성화를 별도 컴포넌트에 위임한다")
        void should_DeactivateToken_when_UnregisteredError() throws FirebaseMessagingException {
            // given
            String token = "invalid-token";

            FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
            given(exception.getMessagingErrorCode()).willReturn(MessagingErrorCode.UNREGISTERED);
            given(firebaseMessaging.send(any(Message.class))).willThrow(exception);

            // when
            boolean result = sender.send(token, "title", "body", Map.of(), null);

            // then: 비활성화는 별도 트랜잭션을 가진 컴포넌트로 위임된다
            assertThat(result).isFalse();
            verify(invalidTokenDeactivator).deactivateToken(token);
        }

        @Test
        @DisplayName("INVALID_ARGUMENT 에러 시 토큰 비활성화를 별도 컴포넌트에 위임한다")
        void should_DeactivateToken_when_InvalidArgumentError() throws FirebaseMessagingException {
            // given
            String token = "malformed-token";

            FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
            given(exception.getMessagingErrorCode()).willReturn(MessagingErrorCode.INVALID_ARGUMENT);
            given(firebaseMessaging.send(any(Message.class))).willThrow(exception);

            // when
            boolean result = sender.send(token, "title", "body", Map.of(), null);

            // then
            assertThat(result).isFalse();
            verify(invalidTokenDeactivator).deactivateToken(token);
        }

        @Test
        @DisplayName("기타 에러 시 토큰을 비활성화하지 않는다")
        void should_NotDeactivateToken_when_OtherError() throws FirebaseMessagingException {
            // given
            String token = "valid-token";

            FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
            given(exception.getMessagingErrorCode()).willReturn(MessagingErrorCode.INTERNAL);
            given(exception.getMessage()).willReturn("Internal error");
            given(firebaseMessaging.send(any(Message.class))).willThrow(exception);

            // when
            boolean result = sender.send(token, "title", "body", Map.of(), null);

            // then
            assertThat(result).isFalse();
            verifyNoInteractions(invalidTokenDeactivator);
        }
    }

    @Nested
    @DisplayName("다중 메시지 전송 시")
    class SendMultipleMessages {

        @Test
        @DisplayName("Firebase가 설정되지 않으면 0을 반환한다")
        void should_ReturnZero_when_FirebaseNotConfigured() {
            // given
            FcmPushNotificationSender senderWithoutFirebase =
                    new FcmPushNotificationSender(null, invalidTokenDeactivator);
            List<String> tokens = List.of("token1", "token2");

            // when
            int result = senderWithoutFirebase.sendMultiple(tokens, "title", "body", Map.of(), null);

            // then
            assertThat(result).isZero();
        }

        @Test
        @DisplayName("빈 토큰 목록이면 0을 반환한다")
        void should_ReturnZero_when_EmptyTokenList() {
            // given
            List<String> tokens = List.of();

            // when
            int result = sender.sendMultiple(tokens, "title", "body", Map.of(), null);

            // then
            assertThat(result).isZero();
            verifyNoInteractions(firebaseMessaging);
        }

        @Test
        @DisplayName("모든 토큰 전송 성공 시 성공 카운트를 반환한다")
        void should_ReturnSuccessCount_when_AllSucceed() throws FirebaseMessagingException {
            // given
            List<String> tokens = List.of("token1", "token2", "token3");

            BatchResponse batchResponse = mock(BatchResponse.class);
            given(batchResponse.getSuccessCount()).willReturn(3);
            given(batchResponse.getFailureCount()).willReturn(0);
            given(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class)))
                    .willReturn(batchResponse);

            // when
            int result = sender.sendMultiple(tokens, "title", "body", Map.of(), null);

            // then
            assertThat(result).isEqualTo(3);
        }

        @Test
        @DisplayName("일부 토큰 전송 실패 시 실패한 토큰을 비활성화한다")
        void should_DeactivateFailedTokens_when_PartialFailure() throws FirebaseMessagingException {
            // given
            List<String> tokens = List.of("valid-token", "invalid-token");

            SendResponse successResponse = mock(SendResponse.class);
            given(successResponse.isSuccessful()).willReturn(true);

            SendResponse failResponse = mock(SendResponse.class);
            given(failResponse.isSuccessful()).willReturn(false);
            FirebaseMessagingException failException = mock(FirebaseMessagingException.class);
            given(failException.getMessagingErrorCode()).willReturn(MessagingErrorCode.UNREGISTERED);
            given(failResponse.getException()).willReturn(failException);

            BatchResponse batchResponse = mock(BatchResponse.class);
            given(batchResponse.getSuccessCount()).willReturn(1);
            given(batchResponse.getFailureCount()).willReturn(1);
            given(batchResponse.getResponses()).willReturn(List.of(successResponse, failResponse));
            given(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class)))
                    .willReturn(batchResponse);

            // when
            int result = sender.sendMultiple(tokens, "title", "body", Map.of(), null);

            // then: 무효 토큰만 별도 트랜잭션 컴포넌트로 비활성화 위임
            assertThat(result).isEqualTo(1);
            verify(invalidTokenDeactivator).deactivateTokens(List.of("invalid-token"));
        }

        @Test
        @DisplayName("500개 초과 토큰은 배치로 분할 전송한다")
        void should_SplitIntoBatches_when_MoreThan500Tokens() throws FirebaseMessagingException {
            // given
            List<String> tokens = new ArrayList<>();
            for (int i = 0; i < 600; i++) {
                tokens.add("token" + i);
            }

            BatchResponse batchResponse1 = mock(BatchResponse.class);
            given(batchResponse1.getSuccessCount()).willReturn(500);
            given(batchResponse1.getFailureCount()).willReturn(0);

            BatchResponse batchResponse2 = mock(BatchResponse.class);
            given(batchResponse2.getSuccessCount()).willReturn(100);
            given(batchResponse2.getFailureCount()).willReturn(0);

            given(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class)))
                    .willReturn(batchResponse1)
                    .willReturn(batchResponse2);

            // when
            int result = sender.sendMultiple(tokens, "title", "body", Map.of(), null);

            // then
            assertThat(result).isEqualTo(600);
            verify(firebaseMessaging, times(2)).sendEachForMulticast(any(MulticastMessage.class));
        }

        @Test
        @DisplayName("배치 전송 중 예외 발생 시 0을 반환한다")
        void should_ReturnZero_when_BatchSendFails() throws FirebaseMessagingException {
            // given
            List<String> tokens = List.of("token1", "token2");

            FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
            given(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class)))
                    .willThrow(exception);

            // when
            int result = sender.sendMultiple(tokens, "title", "body", Map.of(), null);

            // then
            assertThat(result).isZero();
        }

        @Test
        @DisplayName("실패 응답에 예외가 없는 경우 무시한다")
        void should_IgnoreFailure_when_NoException() throws FirebaseMessagingException {
            // given
            List<String> tokens = List.of("token1");

            SendResponse failResponse = mock(SendResponse.class);
            given(failResponse.isSuccessful()).willReturn(false);
            given(failResponse.getException()).willReturn(null);

            BatchResponse batchResponse = mock(BatchResponse.class);
            given(batchResponse.getSuccessCount()).willReturn(0);
            given(batchResponse.getFailureCount()).willReturn(1);
            given(batchResponse.getResponses()).willReturn(List.of(failResponse));
            given(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class)))
                    .willReturn(batchResponse);

            // when
            int result = sender.sendMultiple(tokens, "title", "body", Map.of(), null);

            // then
            assertThat(result).isZero();
            verifyNoInteractions(invalidTokenDeactivator);
        }
    }

    @Nested
    @DisplayName("대상별 배지 전송 시")
    class SendEachWithBadge {

        @Test
        @DisplayName("Firebase가 설정되지 않으면 0을 반환한다")
        void should_ReturnZero_when_FirebaseNotConfigured() {
            // given
            FcmPushNotificationSender senderWithoutFirebase =
                    new FcmPushNotificationSender(null, invalidTokenDeactivator);
            List<PushTarget> targets = List.of(new PushTarget("token1", 3));

            // when
            int result = senderWithoutFirebase.sendEachWithBadge(targets, "title", "body", Map.of(), null);

            // then
            assertThat(result).isZero();
        }

        @Test
        @DisplayName("빈 대상 목록이면 0을 반환한다")
        void should_ReturnZero_when_EmptyTargetList() {
            // given
            List<PushTarget> targets = List.of();

            // when
            int result = sender.sendEachWithBadge(targets, "title", "body", Map.of(), null);

            // then
            assertThat(result).isZero();
            verifyNoInteractions(firebaseMessaging);
        }

        @Test
        @DisplayName("대상별 메시지를 생성하여 전송하고 성공 카운트를 반환한다")
        void should_SendPerTargetMessages_when_TargetsProvided() throws FirebaseMessagingException {
            // given
            List<PushTarget> targets = List.of(
                    new PushTarget("token1", 5),
                    new PushTarget("token2", 10));

            BatchResponse batchResponse = mock(BatchResponse.class);
            given(batchResponse.getSuccessCount()).willReturn(2);
            given(batchResponse.getFailureCount()).willReturn(0);
            given(firebaseMessaging.sendEach(anyList())).willReturn(batchResponse);

            // when
            int result = sender.sendEachWithBadge(targets, "title", "body", Map.of("k", "v"), null);

            // then
            assertThat(result).isEqualTo(2);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Message>> messagesCaptor = ArgumentCaptor.forClass(List.class);
            verify(firebaseMessaging).sendEach(messagesCaptor.capture());
            assertThat(messagesCaptor.getValue()).hasSize(2);
        }

        @Test
        @DisplayName("일부 대상 전송 실패 시 실패한 토큰을 비활성화한다")
        void should_DeactivateFailedTokens_when_PartialFailure() throws FirebaseMessagingException {
            // given
            List<PushTarget> targets = List.of(
                    new PushTarget("valid-token", 1),
                    new PushTarget("invalid-token", 2));

            SendResponse successResponse = mock(SendResponse.class);
            given(successResponse.isSuccessful()).willReturn(true);

            SendResponse failResponse = mock(SendResponse.class);
            given(failResponse.isSuccessful()).willReturn(false);
            FirebaseMessagingException failException = mock(FirebaseMessagingException.class);
            given(failException.getMessagingErrorCode()).willReturn(MessagingErrorCode.UNREGISTERED);
            given(failResponse.getException()).willReturn(failException);

            BatchResponse batchResponse = mock(BatchResponse.class);
            given(batchResponse.getSuccessCount()).willReturn(1);
            given(batchResponse.getFailureCount()).willReturn(1);
            given(batchResponse.getResponses()).willReturn(List.of(successResponse, failResponse));
            given(firebaseMessaging.sendEach(anyList())).willReturn(batchResponse);

            // when
            int result = sender.sendEachWithBadge(targets, "title", "body", Map.of(), null);

            // then: 무효 토큰만 별도 트랜잭션 컴포넌트로 비활성화 위임
            assertThat(result).isEqualTo(1);
            verify(invalidTokenDeactivator).deactivateTokens(List.of("invalid-token"));
        }

        @Test
        @DisplayName("전송 중 예외 발생 시 0을 반환한다")
        void should_ReturnZero_when_SendFails() throws FirebaseMessagingException {
            // given
            List<PushTarget> targets = List.of(new PushTarget("token1", 1));

            FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
            given(firebaseMessaging.sendEach(anyList())).willThrow(exception);

            // when
            int result = sender.sendEachWithBadge(targets, "title", "body", Map.of(), null);

            // then
            assertThat(result).isZero();
        }
    }
}
