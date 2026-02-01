package com.cotalk.infrastructure.ratelimit;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Rate Limiting 설정 클래스.
 * Bucket4j와 Redis를 사용하여 분산 환경에서의 Rate Limiting을 구현한다.
 *
 * <p>이 설정은 {@code app.rate-limit.enabled=true}일 때만 활성화된다.</p>
 *
 * @author seunggu.lee
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "app.rate-limit.enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitConfig {

    private final RedisProperties redisProperties;

    /**
     * RateLimitConfig 생성자.
     *
     * @param redisProperties Spring Data Redis 설정 프로퍼티
     */
    public RateLimitConfig(RedisProperties redisProperties) {
        this.redisProperties = redisProperties;
    }

    /**
     * Bucket4j Redis ProxyManager를 생성한다.
     * 분산 환경에서 Rate Limit 상태를 Redis에 저장하여 공유한다.
     *
     * @return Redis 기반 Bucket4j ProxyManager
     */
    @Bean
    public ProxyManager<byte[]> bucket4jProxyManager() {
        String host = redisProperties.getHost();
        int port = redisProperties.getPort();
        String password = redisProperties.getPassword();

        RedisURI redisURI = RedisURI.builder()
                .withHost(host)
                .withPort(port)
                .withPassword(password != null && !password.isEmpty()
                        ? password.toCharArray()
                        : null)
                .build();

        RedisClient redisClient = RedisClient.create(redisURI);
        
        ProxyManager<byte[]> proxyManager = LettuceBasedProxyManager.builderFor(redisClient)
                .withExpirationStrategy(ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofSeconds(10)))
                .build();

        log.info("Bucket4j ProxyManager initialized with Redis: {}:{}", host, port);
        return proxyManager;
    }
}
