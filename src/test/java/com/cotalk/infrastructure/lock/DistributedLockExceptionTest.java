package com.cotalk.infrastructure.lock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DistributedLockException 단위 테스트.
 *
 * @author seunggu.lee
 */
@DisplayName("DistributedLockException 단위 테스트")
class DistributedLockExceptionTest {

    @Test
    @DisplayName("메시지만 포함한 예외 생성")
    void should_createException_when_messageProvided() {
        // given
        String message = "락 획득 실패";

        // when
        DistributedLockException exception = new DistributedLockException(message);

        // then
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    @DisplayName("메시지와 원인을 포함한 예외 생성")
    void should_createException_when_messageAndCauseProvided() {
        // given
        String message = "락 획득 중 인터럽트 발생";
        Throwable cause = new InterruptedException("Interrupted");

        // when
        DistributedLockException exception = new DistributedLockException(message, cause);

        // then
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    @DisplayName("RuntimeException 상속 확인")
    void should_beRuntimeException() {
        // given
        DistributedLockException exception = new DistributedLockException("test");

        // then
        assertInstanceOf(RuntimeException.class, exception);
    }
}
