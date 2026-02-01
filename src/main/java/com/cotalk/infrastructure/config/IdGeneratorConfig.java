package com.cotalk.infrastructure.config;

import com.cotalk.infrastructure.config.properties.SnowflakeProperties;
import com.cotalk.infrastructure.id.RedisWorkerIdAllocator;
import com.cotalk.infrastructure.id.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * ID 생성기 설정 클래스.
 * Snowflake 알고리즘 기반의 분산 ID 생성기를 구성한다.
 *
 * <p>분산 환경에서는 {@code snowflake.use-redis-allocation=true}로 설정하여
 * Redis를 통한 자동 Worker ID 할당을 사용하는 것을 권장한다.
 *
 * @author seunggu.lee
 */
@Configuration
@RequiredArgsConstructor
public class IdGeneratorConfig {

    private final SnowflakeProperties snowflakeProperties;

    /**
     * Redis 기반 Worker ID 할당기를 생성한다.
     * 분산 환경에서 자동으로 고유한 Worker ID를 할당받는다.
     *
     * @param redisTemplate Redis 템플릿
     * @return Redis Worker ID 할당기 인스턴스
     */
    @Bean
    @ConditionalOnProperty(name = "snowflake.use-redis-allocation", havingValue = "true")
    public RedisWorkerIdAllocator redisWorkerIdAllocator(StringRedisTemplate redisTemplate) {
        return new RedisWorkerIdAllocator(redisTemplate, snowflakeProperties.datacenterId());
    }

    /**
     * Snowflake ID 생성기를 생성한다 (Redis 자동 할당 사용).
     * Worker ID는 Redis를 통해 자동으로 할당된다.
     *
     * @param allocator Redis Worker ID 할당기
     * @return Snowflake ID 생성기 인스턴스
     */
    @Bean
    @ConditionalOnProperty(name = "snowflake.use-redis-allocation", havingValue = "true")
    public SnowflakeIdGenerator snowflakeIdGeneratorWithRedis(RedisWorkerIdAllocator allocator) {
        return new SnowflakeIdGenerator(allocator.getDatacenterId(), allocator.getWorkerId());
    }

    /**
     * Snowflake ID 생성기를 생성한다 (수동 설정 사용).
     * 데이터센터 ID와 워커 ID를 설정 파일에서 직접 지정한다.
     *
     * @return Snowflake ID 생성기 인스턴스
     */
    @Bean
    @ConditionalOnProperty(name = "snowflake.use-redis-allocation", havingValue = "false", matchIfMissing = true)
    public SnowflakeIdGenerator snowflakeIdGeneratorManual() {
        return new SnowflakeIdGenerator(snowflakeProperties.datacenterId(), snowflakeProperties.workerId());
    }
}
