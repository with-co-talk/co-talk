package com.cotalk.infrastructure.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * 캐시 설정 클래스.
 * 애플리케이션의 캐시 매니저를 구성한다.
 *
 * <p>지원하는 캐시:
 * <ul>
 *   <li>{@link #USER_CACHE} - 사용자 정보 캐시</li>
 *   <li>{@link #CHAT_ROOM_CACHE} - 채팅방 정보 캐시</li>
 *   <li>{@link #STATISTICS_CACHE} - 통계 정보 캐시</li>
 * </ul>
 *
 * @author seunggu.lee
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 사용자 정보 캐시 이름.
     */
    public static final String USER_CACHE = "users";

    /**
     * 채팅방 정보 캐시 이름.
     */
    public static final String CHAT_ROOM_CACHE = "chatRooms";

    /**
     * 통계 정보 캐시 이름.
     */
    public static final String STATISTICS_CACHE = "statistics";

    /**
     * 운영 환경용 캐시 매니저를 생성한다.
     * ConcurrentMapCacheManager를 사용하여 인메모리 캐시를 제공한다.
     *
     * @return 캐시 매니저
     */
    @Bean
    @Profile("!test")
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager(
                USER_CACHE,
                CHAT_ROOM_CACHE,
                STATISTICS_CACHE
        );
        return cacheManager;
    }

    /**
     * 테스트 환경용 캐시 매니저를 생성한다.
     * 테스트 격리를 위해 별도의 캐시 매니저 인스턴스를 제공한다.
     *
     * @return 테스트용 캐시 매니저
     */
    @Bean
    @Profile("test")
    public CacheManager testCacheManager() {
        return new ConcurrentMapCacheManager(
                USER_CACHE,
                CHAT_ROOM_CACHE,
                STATISTICS_CACHE
        );
    }
}
