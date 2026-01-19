package com.cotalk.domain.exception;

/**
 * 메시지 브로커 관련 예외.
 * 메시지 발행, 직렬화 등 브로커 작업 중 발생하는 오류를 처리한다.
 *
 * @author seunggu.lee
 */
public class MessageBrokerException extends DomainException {

    public MessageBrokerException(String message) {
        super(message);
    }

    public MessageBrokerException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 메시지 직렬화 실패 시 발생하는 예외.
     *
     * @param cause 원인 예외
     * @return MessageBrokerException
     */
    public static MessageBrokerException serializationFailed(Throwable cause) {
        return new MessageBrokerException("메시지 직렬화에 실패했습니다.", cause);
    }

    /**
     * 리액션 이벤트 직렬화 실패 시 발생하는 예외.
     *
     * @param cause 원인 예외
     * @return MessageBrokerException
     */
    public static MessageBrokerException reactionSerializationFailed(Throwable cause) {
        return new MessageBrokerException("리액션 이벤트 직렬화에 실패했습니다.", cause);
    }

    /**
     * 메시지 발행 실패 시 발생하는 예외.
     *
     * @param channel 발행 실패한 채널
     * @param cause 원인 예외
     * @return MessageBrokerException
     */
    public static MessageBrokerException publishFailed(String channel, Throwable cause) {
        return new MessageBrokerException("메시지 발행에 실패했습니다: " + channel, cause);
    }
}
