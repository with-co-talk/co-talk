package com.cotalk.infrastructure.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Docker 환경용 Redisson 설정 클래스.
 *
 * <p>Docker 프로파일에서 사용되며, Redis 비밀번호가 설정된 경우 인증하여 연결한다.
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

    private final RedisProperties redisProperties;

    /**
     * DockerRedissonConfig 생성자.
     *
     * @param redisProperties Spring Data Redis 설정 프로퍼티
     */
    public DockerRedissonConfig(RedisProperties redisProperties) {
        this.redisProperties = redisProperties;
    }

    /**
     * Docker 환경용 RedissonClient 빈을 생성한다.
     * Redis 비밀번호가 설정된 경우 인증하여 연결한다.
     *
     * @return 설정된 RedissonClient 인스턴스
     */
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        String host = redisProperties.getHost();
        int port = redisProperties.getPort();
        String password = redisProperties.getPassword();

        Config config = new Config();
        SingleServerConfig serverConfig = config.useSingleServer()
                .setAddress("redis://" + host + ":" + port);

        if (password != null && !password.isEmpty()) {
            serverConfig.setPassword(password);
        }

        serverConfig.setConnectionMinimumIdleSize(1)
                .setConnectionPoolSize(10)
                .setConnectTimeout(10000)
                .setTimeout(3000)
                .setRetryAttempts(3)
                .setRetryInterval(1500);

        return Redisson.create(config);
    }
}
