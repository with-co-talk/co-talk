package com.cotalk.infrastructure.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Docker 환경용 Redisson 설정 클래스.
 *
 * <p>Docker 프로파일에서 사용되며, 비밀번호 없는 Redis 연결을 설정한다.
 * RedissonAutoConfigurationV2의 자동 설정을 대체하여 AUTH 에러를 방지한다.
 *
 * <p>이 빈이 먼저 생성되므로 RedissonAutoConfigurationV2의 @ConditionalOnMissingBean
 * 조건에 의해 자동 설정이 비활성화된다.
 *
 * @author seunggu.lee
 */
@Configuration
@Profile("docker")
public class DockerRedissonConfig {

    @Value("${spring.data.redis.host:redis}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    /**
     * Docker 환경용 RedissonClient 빈을 생성한다.
     * 비밀번호 없이 Redis에 연결한다.
     *
     * @return 설정된 RedissonClient 인스턴스
     */
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + redisHost + ":" + redisPort)
                .setConnectionMinimumIdleSize(1)
                .setConnectionPoolSize(10)
                .setConnectTimeout(10000)
                .setTimeout(3000)
                .setRetryAttempts(3)
                .setRetryInterval(1500);
        // 비밀번호를 설정하지 않음 - AUTH 명령이 전송되지 않음

        return Redisson.create(config);
    }
}
