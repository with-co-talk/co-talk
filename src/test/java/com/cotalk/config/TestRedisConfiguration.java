package com.cotalk.config;

import com.cotalk.infrastructure.config.properties.AppProperties;
import com.cotalk.infrastructure.lock.DistributedLockExecutor;
import com.cotalk.infrastructure.security.SecurityContextHelper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * 테스트 환경에서 Redis 관련 빈을 모킹하는 설정.
 *
 * <p>통합 테스트 시 실제 Redis 연결 없이 테스트를 실행할 수 있도록 한다.
 *
 * @author seunggu.lee
 */
@TestConfiguration
public class TestRedisConfiguration {

    /**
     * 모킹된 RedissonClient 빈을 제공한다.
     *
     * @return 모킹된 RedissonClient
     */
    @Bean
    @Primary
    public RedissonClient redissonClient() {
        RedissonClient mockClient = mock(RedissonClient.class);
        RLock mockLock = mock(RLock.class);

        try {
            given(mockLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).willReturn(true);
            given(mockLock.isHeldByCurrentThread()).willReturn(true);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        given(mockClient.getLock(anyString())).willReturn(mockLock);

        return mockClient;
    }

    /**
     * 테스트용 DistributedLockExecutor를 제공한다.
     * 락 획득 없이 바로 작업을 실행한다.
     *
     * @return 테스트용 DistributedLockExecutor
     */
    @Bean
    @Primary
    public DistributedLockExecutor distributedLockExecutor() {
        return new TestDistributedLockExecutor();
    }

    /**
     * 모킹된 RedisConnectionFactory 빈을 제공한다.
     *
     * @return 모킹된 RedisConnectionFactory
     */
    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        RedisConnectionFactory factory = mock(RedisConnectionFactory.class);
        RedisConnection connection = mock(RedisConnection.class);
        given(factory.getConnection()).willReturn(connection);
        return factory;
    }

    /**
     * 모킹된 RedisTemplate 빈을 제공한다.
     *
     * @return 모킹된 RedisTemplate
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }

    /**
     * 모킹된 RedisTemplate<String, String> 빈을 제공한다.
     *
     * @return 모킹된 RedisTemplate<String, String>
     */
    @Bean
    @Primary
    public RedisTemplate<String, String> stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }

    /**
     * 테스트용 SecurityContextHelper를 제공한다.
     * 요청 파라미터에서 userId를 읽어 반환한다.
     *
     * @return 테스트용 SecurityContextHelper
     */
    @Bean
    @Primary
    public SecurityContextHelper securityContextHelper() {
        return new TestSecurityContextHelper();
    }

    /**
     * 테스트용 SecurityContextHelper.
     * 요청 파라미터에서 userId를 읽어 반환한다.
     */
    private static class TestSecurityContextHelper extends SecurityContextHelper {

        @Override
        public Long getCurrentUserId() {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String userIdParam = request.getParameter("userId");
                if (userIdParam != null) {
                    return Long.parseLong(userIdParam);
                }
            }
            // 기본값 반환 (테스트용)
            return 1L;
        }

        @Override
        public boolean isAuthenticated() {
            return true;
        }
    }

    /**
     * 테스트용 분산락 실행기.
     * 락 획득 과정 없이 바로 작업을 실행한다.
     */
    private static class TestDistributedLockExecutor extends DistributedLockExecutor {

        public TestDistributedLockExecutor() {
            super(null, new AppProperties(null, null, null, null, null, null, null, null,
                    new AppProperties.Lock(false)));
        }

        @Override
        public <T> T executeWithLock(String lockKey, long waitTime, long leaseTime,
                                      TimeUnit timeUnit, Supplier<T> supplier) {
            return supplier.get();
        }

        @Override
        public void executeWithLock(String lockKey, long waitTime, long leaseTime,
                                    TimeUnit timeUnit, Runnable runnable) {
            runnable.run();
        }

        @Override
        public <T> T executeWithLock(String lockKey, Supplier<T> supplier) {
            return supplier.get();
        }

        @Override
        public void executeWithLock(String lockKey, Runnable runnable) {
            runnable.run();
        }
    }
}
