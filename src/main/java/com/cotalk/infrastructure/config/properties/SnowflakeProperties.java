package com.cotalk.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Snowflake ID 생성기 설정 프로퍼티.
 *
 * @param datacenterId 데이터센터 ID (0-31)
 * @param workerId     워커 ID (0-31)
 * @author seunggu.lee
 */
@ConfigurationProperties(prefix = "snowflake")
public record SnowflakeProperties(
        long datacenterId,
        long workerId
) {
    public SnowflakeProperties {
        if (datacenterId < 0 || datacenterId > 31) {
            datacenterId = 0;
        }
        if (workerId < 0 || workerId > 31) {
            workerId = 0;
        }
    }
}
