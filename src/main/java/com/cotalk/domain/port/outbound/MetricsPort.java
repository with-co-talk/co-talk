package com.cotalk.domain.port.outbound;

/**
 * 비즈니스 메트릭 수집을 위한 포트 인터페이스.
 * Application 레이어에서 인프라스트럭처의 메트릭 구현에 직접 의존하지 않도록
 * 포트를 통해 간접적으로 접근한다.
 *
 * @author seunggu.lee
 */
public interface MetricsPort {

    /**
     * 전송된 메시지 카운터를 1 증가시킨다.
     */
    void incrementMessagesSent();

    /**
     * 수신된 메시지 카운터를 1 증가시킨다.
     */
    void incrementMessagesReceived();

    /**
     * 사용자 등록 카운터를 1 증가시킨다.
     */
    void incrementUserRegistration();

    /**
     * 로그인 성공 카운터를 1 증가시킨다.
     */
    void incrementLoginSuccess();

    /**
     * 로그인 실패 카운터를 1 증가시킨다.
     */
    void incrementLoginFailure();

    /**
     * 메시지 처리 시간 측정을 시작한다.
     *
     * @return 타이머 샘플 (구현체에서 정의하는 타이머 객체)
     */
    Object startMessageProcessingTimer();

    /**
     * 메시지 처리 시간 측정을 종료하고 기록한다.
     *
     * @param sample {@link #startMessageProcessingTimer()}에서 반환된 타이머 샘플
     */
    void stopMessageProcessingTimer(Object sample);
}
