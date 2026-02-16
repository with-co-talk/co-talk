package com.cotalk.infrastructure.config;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
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
 * <p>RedissonClient 생성에 실패하면 null을 반환하여 앱 시작을 차단하지 않는다.
 * 이 경우 {@link com.cotalk.infrastructure.lock.DistributedLockExecutor}가
 * NoOp 모드로 동작한다.
 *
 * @author seunggu.lee
 */
@Slf4j
@Configuration
@Profile("docker")
public class DockerRedissonConfig {

    private final RedisProperties redisProperties;
    private RedissonClient client;

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
     * 연결 실패 시 null을 반환하여 분산락이 NoOp 모드로 동작한다.
     *
     * @return 설정된 RedissonClient 인스턴스, 또는 연결 실패 시 null
     */
    @Bean(destroyMethod = "")
    public RedissonClient redissonClient() {
        String host = redisProperties.getHost();
        int port = redisProperties.getPort();
        String password = redisProperties.getPassword();

        try {
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

            this.client = Redisson.create(config);
            log.info("RedissonClient created successfully ({}:{})", host, port);
            return this.client;
        } catch (Exception e) {
            log.warn("Failed to create RedissonClient ({}:{}): {}. Distributed locks disabled.",
                    host, port, e.getMessage());
            return null;
        }
    }

    /**
     * 애플리케이션 종료 시 RedissonClient를 안전하게 종료한다.
     */
    @PreDestroy
    public void destroy() {
        if (client != null && !client.isShutdown()) {
            client.shutdown();
            log.info("RedissonClient shut down.");
        }
    }
}
