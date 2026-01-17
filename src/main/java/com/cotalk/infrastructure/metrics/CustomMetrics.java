package com.cotalk.infrastructure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Co-Talk 애플리케이션 커스텀 메트릭 클래스.
 * Micrometer를 사용하여 비즈니스 메트릭을 수집한다.
 *
 * <p>수집되는 메트릭:</p>
 * <ul>
 *   <li>cotalk.messages.sent - 전송된 메시지 수</li>
 *   <li>cotalk.messages.received - 수신된 메시지 수</li>
 *   <li>cotalk.messages.processing.time - 메시지 처리 시간</li>
 *   <li>cotalk.users.registered - 등록된 사용자 수</li>
 *   <li>cotalk.auth.login.success - 로그인 성공 수</li>
 *   <li>cotalk.auth.login.failure - 로그인 실패 수</li>
 *   <li>cotalk.websocket.connections - 활성 WebSocket 연결 수</li>
 *   <li>cotalk.chatrooms.active - 활성 채팅방 수</li>
 * </ul>
 *
 * @author seunggu.lee
 */
@Component
@Getter
public class CustomMetrics {

    /** 전송된 메시지 카운터 */
    private final Counter messagesSentCounter;
    /** 수신된 메시지 카운터 */
    private final Counter messagesReceivedCounter;
    /** 사용자 등록 카운터 */
    private final Counter userRegistrationCounter;
    /** 로그인 성공 카운터 */
    private final Counter loginSuccessCounter;
    /** 로그인 실패 카운터 */
    private final Counter loginFailureCounter;
    /** 메시지 처리 시간 타이머 */
    private final Timer messageProcessingTimer;

    /** 활성 WebSocket 연결 수 */
    private final AtomicLong activeWebSocketConnections = new AtomicLong(0);
    /** 활성 채팅방 수 */
    private final AtomicLong activeChatRooms = new AtomicLong(0);

    /**
     * CustomMetrics를 생성하고 메트릭을 등록한다.
     *
     * @param meterRegistry Micrometer 메트릭 레지스트리
     */
    public CustomMetrics(MeterRegistry meterRegistry) {
        // 메시지 관련 메트릭
        this.messagesSentCounter = Counter.builder("cotalk.messages.sent")
                .description("Total number of messages sent")
                .register(meterRegistry);

        this.messagesReceivedCounter = Counter.builder("cotalk.messages.received")
                .description("Total number of messages received")
                .register(meterRegistry);

        this.messageProcessingTimer = Timer.builder("cotalk.messages.processing.time")
                .description("Time taken to process messages")
                .register(meterRegistry);

        // 사용자 관련 메트릭
        this.userRegistrationCounter = Counter.builder("cotalk.users.registered")
                .description("Total number of user registrations")
                .register(meterRegistry);

        this.loginSuccessCounter = Counter.builder("cotalk.auth.login.success")
                .description("Total number of successful logins")
                .register(meterRegistry);

        this.loginFailureCounter = Counter.builder("cotalk.auth.login.failure")
                .description("Total number of failed logins")
                .register(meterRegistry);

        // WebSocket 연결 수 (Gauge)
        Gauge.builder("cotalk.websocket.connections", activeWebSocketConnections, AtomicLong::get)
                .description("Current number of active WebSocket connections")
                .register(meterRegistry);

        // 활성 채팅방 수 (Gauge)
        Gauge.builder("cotalk.chatrooms.active", activeChatRooms, AtomicLong::get)
                .description("Current number of active chat rooms")
                .register(meterRegistry);
    }

    /**
     * 전송된 메시지 카운터를 1 증가시킨다.
     */
    public void incrementMessagesSent() {
        messagesSentCounter.increment();
    }

    /**
     * 수신된 메시지 카운터를 1 증가시킨다.
     */
    public void incrementMessagesReceived() {
        messagesReceivedCounter.increment();
    }

    /**
     * 사용자 등록 카운터를 1 증가시킨다.
     */
    public void incrementUserRegistration() {
        userRegistrationCounter.increment();
    }

    /**
     * 로그인 성공 카운터를 1 증가시킨다.
     */
    public void incrementLoginSuccess() {
        loginSuccessCounter.increment();
    }

    /**
     * 로그인 실패 카운터를 1 증가시킨다.
     */
    public void incrementLoginFailure() {
        loginFailureCounter.increment();
    }

    /**
     * 활성 WebSocket 연결 수를 1 증가시킨다.
     */
    public void incrementWebSocketConnections() {
        activeWebSocketConnections.incrementAndGet();
    }

    /**
     * 활성 WebSocket 연결 수를 1 감소시킨다.
     */
    public void decrementWebSocketConnections() {
        activeWebSocketConnections.decrementAndGet();
    }

    /**
     * 활성 채팅방 수를 설정한다.
     *
     * @param count 활성 채팅방 수
     */
    public void setActiveChatRooms(long count) {
        activeChatRooms.set(count);
    }

    /**
     * 메시지 처리 시간 측정을 시작한다.
     *
     * @return 타이머 샘플
     */
    public Timer.Sample startMessageProcessingTimer() {
        return Timer.start();
    }

    /**
     * 메시지 처리 시간 측정을 종료하고 기록한다.
     *
     * @param sample 타이머 샘플
     */
    public void stopMessageProcessingTimer(Timer.Sample sample) {
        sample.stop(messageProcessingTimer);
    }
}
