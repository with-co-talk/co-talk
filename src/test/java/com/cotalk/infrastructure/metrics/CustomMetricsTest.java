package com.cotalk.infrastructure.metrics;

import io.micrometer.core.instrument.MeterRegistry;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CustomMetrics 단위 테스트.
 *
 * @author seunggu.lee
 */
@DisplayName("CustomMetrics")
class CustomMetricsTest {

    private MeterRegistry meterRegistry;
    private CustomMetrics customMetrics;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        customMetrics = new CustomMetrics(meterRegistry);
    }

    @Nested
    @DisplayName("메시지 메트릭")
    class MessageMetrics {

        @Test
        @DisplayName("전송 메시지 카운터를 증가시킨다")
        void should_incrementMessagesSent() {
            // when
            customMetrics.incrementMessagesSent();
            customMetrics.incrementMessagesSent();

            // then
            assertThat(customMetrics.getMessagesSentCounter().count()).isEqualTo(2.0);
        }

        @Test
        @DisplayName("수신 메시지 카운터를 증가시킨다")
        void should_incrementMessagesReceived() {
            // when
            customMetrics.incrementMessagesReceived();
            customMetrics.incrementMessagesReceived();
            customMetrics.incrementMessagesReceived();

            // then
            assertThat(customMetrics.getMessagesReceivedCounter().count()).isEqualTo(3.0);
        }

        @Test
        @DisplayName("메시지 처리 시간을 측정한다")
        void should_measureMessageProcessingTime() throws InterruptedException {
            // given
            Object sample = customMetrics.startMessageProcessingTimer();

            // when
            Thread.sleep(10); // 10ms 대기
            customMetrics.stopMessageProcessingTimer(sample);

            // then
            assertThat(customMetrics.getMessageProcessingTimer().count()).isEqualTo(1);
            assertThat(customMetrics.getMessageProcessingTimer().totalTime(java.util.concurrent.TimeUnit.MILLISECONDS))
                    .isGreaterThanOrEqualTo(10);
        }
    }

    @Nested
    @DisplayName("사용자 메트릭")
    class UserMetrics {

        @Test
        @DisplayName("사용자 등록 카운터를 증가시킨다")
        void should_incrementUserRegistration() {
            // when
            customMetrics.incrementUserRegistration();

            // then
            assertThat(customMetrics.getUserRegistrationCounter().count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("로그인 성공 카운터를 증가시킨다")
        void should_incrementLoginSuccess() {
            // when
            customMetrics.incrementLoginSuccess();
            customMetrics.incrementLoginSuccess();

            // then
            assertThat(customMetrics.getLoginSuccessCounter().count()).isEqualTo(2.0);
        }

        @Test
        @DisplayName("로그인 실패 카운터를 증가시킨다")
        void should_incrementLoginFailure() {
            // when
            customMetrics.incrementLoginFailure();

            // then
            assertThat(customMetrics.getLoginFailureCounter().count()).isEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("WebSocket 메트릭")
    class WebSocketMetrics {

        @Test
        @DisplayName("WebSocket 연결 수를 증가시킨다")
        void should_incrementWebSocketConnections() {
            // when
            customMetrics.incrementWebSocketConnections();
            customMetrics.incrementWebSocketConnections();

            // then
            assertThat(customMetrics.getActiveWebSocketConnections().get()).isEqualTo(2);
        }

        @Test
        @DisplayName("WebSocket 연결 수를 감소시킨다")
        void should_decrementWebSocketConnections() {
            // given
            customMetrics.incrementWebSocketConnections();
            customMetrics.incrementWebSocketConnections();

            // when
            customMetrics.decrementWebSocketConnections();

            // then
            assertThat(customMetrics.getActiveWebSocketConnections().get()).isEqualTo(1);
        }

        @Test
        @DisplayName("연결이 없을 때 감소시키면 음수가 된다")
        void should_becomeNegative_when_decrementWithNoConnections() {
            // when
            customMetrics.decrementWebSocketConnections();

            // then
            assertThat(customMetrics.getActiveWebSocketConnections().get()).isEqualTo(-1);
        }
    }

    @Nested
    @DisplayName("채팅방 메트릭")
    class ChatRoomMetrics {

        @Test
        @DisplayName("활성 채팅방 수를 설정한다")
        void should_setActiveChatRooms() {
            // when
            customMetrics.setActiveChatRooms(10);

            // then
            assertThat(customMetrics.getActiveChatRooms().get()).isEqualTo(10);
        }

        @Test
        @DisplayName("활성 채팅방 수를 변경한다")
        void should_updateActiveChatRooms() {
            // given
            customMetrics.setActiveChatRooms(5);

            // when
            customMetrics.setActiveChatRooms(15);

            // then
            assertThat(customMetrics.getActiveChatRooms().get()).isEqualTo(15);
        }
    }

    @Nested
    @DisplayName("메트릭 등록 확인")
    class MetricRegistration {

        @Test
        @DisplayName("모든 카운터가 레지스트리에 등록된다")
        void should_registerAllCounters() {
            // then
            assertThat(meterRegistry.find("cotalk.messages.sent").counter()).isNotNull();
            assertThat(meterRegistry.find("cotalk.messages.received").counter()).isNotNull();
            assertThat(meterRegistry.find("cotalk.users.registered").counter()).isNotNull();
            assertThat(meterRegistry.find("cotalk.auth.login.success").counter()).isNotNull();
            assertThat(meterRegistry.find("cotalk.auth.login.failure").counter()).isNotNull();
        }

        @Test
        @DisplayName("타이머가 레지스트리에 등록된다")
        void should_registerTimer() {
            // then
            assertThat(meterRegistry.find("cotalk.messages.processing.time").timer()).isNotNull();
        }

        @Test
        @DisplayName("게이지가 레지스트리에 등록된다")
        void should_registerGauges() {
            // then
            assertThat(meterRegistry.find("cotalk.websocket.connections").gauge()).isNotNull();
            assertThat(meterRegistry.find("cotalk.chatrooms.active").gauge()).isNotNull();
        }

        @Test
        @DisplayName("게이지가 실시간 값을 반영한다")
        void should_reflectRealTimeValues_inGauges() {
            // when
            customMetrics.incrementWebSocketConnections();
            customMetrics.incrementWebSocketConnections();
            customMetrics.setActiveChatRooms(5);

            // then
            assertThat(meterRegistry.find("cotalk.websocket.connections").gauge().value()).isEqualTo(2.0);
            assertThat(meterRegistry.find("cotalk.chatrooms.active").gauge().value()).isEqualTo(5.0);
        }
    }
}
