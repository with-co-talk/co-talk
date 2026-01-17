package com.cotalk.infrastructure.ratelimit;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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

    /** Redis 호스트 주소 */
    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    /** Redis 포트 번호 */
    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    /** Redis 비밀번호 */
    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    /**
     * Bucket4j Redis ProxyManager를 생성한다.
     * 분산 환경에서 Rate Limit 상태를 Redis에 저장하여 공유한다.
     *
     * @return Redis 기반 Bucket4j ProxyManager
     */
    @Bean
    public ProxyManager<byte[]> bucket4jProxyManager() {
        RedisURI redisURI = RedisURI.builder()
                .withHost(redisHost)
                .withPort(redisPort)
                .withPassword(redisPassword != null && !redisPassword.isEmpty() 
                        ? redisPassword.toCharArray() 
                        : null)
                .build();

        RedisClient redisClient = RedisClient.create(redisURI);
        
        ProxyManager<byte[]> proxyManager = LettuceBasedProxyManager.builderFor(redisClient)
                .withExpirationStrategy(ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofSeconds(10)))
                .build();

        log.info("Bucket4j ProxyManager initialized with Redis: {}:{}", redisHost, redisPort);
        return proxyManager;
    }
}
