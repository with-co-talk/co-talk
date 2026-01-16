package com.cotalk.infrastructure.config;

import com.cotalk.infrastructure.id.SnowflakeIdGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IdGeneratorConfig {

    @Value("${snowflake.datacenter-id:0}")
    private long datacenterId;

    @Value("${snowflake.worker-id:0}")
    private long workerId;

    @Bean
    public SnowflakeIdGenerator snowflakeIdGenerator() {
        return new SnowflakeIdGenerator(datacenterId, workerId);
    }
}
