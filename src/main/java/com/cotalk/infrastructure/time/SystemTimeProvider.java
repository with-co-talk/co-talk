package com.cotalk.infrastructure.time;

import com.cotalk.domain.port.outbound.TimeProvider;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 시스템 시계를 사용하는 TimeProvider 구현체.
 *
 * @author seunggu.lee
 */
@Component
public class SystemTimeProvider implements TimeProvider {

    @Override
    public LocalDateTime now() {
        return LocalDateTime.now();
    }
}
