package com.cotalk.domain.port.outbound;

import java.time.LocalDateTime;

/**
 * 현재 시간을 제공하는 포트.
 * 테스트에서 시간을 제어할 수 있도록 추상화한다.
 *
 * @author seunggu.lee
 */
public interface TimeProvider {

    /**
     * 현재 시간을 반환한다.
     *
     * @return 현재 시간
     */
    LocalDateTime now();
}
