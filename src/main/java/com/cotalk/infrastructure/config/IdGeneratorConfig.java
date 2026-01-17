package com.cotalk.infrastructure.config;

import com.cotalk.infrastructure.id.SnowflakeIdGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ID 생성기 설정 클래스.
 * Snowflake 알고리즘 기반의 분산 ID 생성기를 구성한다.
 *
 * <p>설정 프로퍼티:
 * <ul>
 *   <li>{@code snowflake.datacenter-id} - 데이터센터 ID (기본값: 0)</li>
 *   <li>{@code snowflake.worker-id} - 워커 ID (기본값: 0)</li>
 * </ul>
 *
 * @author seunggu.lee
 */
@Configuration
public class IdGeneratorConfig {

    @Value("${snowflake.datacenter-id:0}")
    private long datacenterId;

    @Value("${snowflake.worker-id:0}")
    private long workerId;

    /**
     * Snowflake ID 생성기를 생성한다.
     * 데이터센터 ID와 워커 ID를 조합하여 고유한 ID를 생성할 수 있도록 한다.
     *
     * @return Snowflake ID 생성기 인스턴스
     */
    @Bean
    public SnowflakeIdGenerator snowflakeIdGenerator() {
        return new SnowflakeIdGenerator(datacenterId, workerId);
    }
}
