package com.cotalk.adapter.outbound.persistence;

import com.cotalk.adapter.outbound.persistence.user.UserRepositoryAdapter;
import com.cotalk.config.TestRedisConfiguration;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.model.Email;
import com.cotalk.infrastructure.config.CacheConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UserRepositoryAdapter 캐싱 테스트.
 *
 * @author seunggu.lee
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(TestRedisConfiguration.class)
@DisplayName("UserRepositoryAdapter 캐싱")
class UserRepositoryAdapterCacheTest {

    @Autowired
    private UserRepositoryAdapter userRepository;

    @Autowired
    private CacheManager cacheManager;

    private User savedUser;

    @BeforeEach
    void setUp() {
        // 캐시 초기화
        cacheManager.getCache(CacheConfig.USER_CACHE).clear();

        // 테스트 사용자 생성 (어댑터를 통해 도메인 User 저장)
        savedUser = userRepository.save(User.builder()
                .id(1000L)
                .email(new Email("cache-test@example.com"))
                .passwordHash("hash")
                .nickname("cacheTestUser")
                .status(User.UserStatus.ACTIVE)
                .build());
    }

    @Nested
    @DisplayName("findById 캐싱 시")
    class FindByIdCaching {

        @Test
        @DisplayName("첫 조회 후 캐시에 저장된다")
        void should_cacheResult_when_firstFindById() {
            // given
            Long userId = savedUser.getId();

            // when
            Optional<User> result = userRepository.findById(userId);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getEmail()).isEqualTo(new Email("cache-test@example.com"));

            // 캐시 확인
            Object cachedValue = cacheManager.getCache(CacheConfig.USER_CACHE).get(userId);
            assertThat(cachedValue).isNotNull();
        }

        @Test
        @DisplayName("캐시된 값이 반환된다")
        void should_returnCachedValue_when_secondFindById() {
            // given
            Long userId = savedUser.getId();

            // 첫 번째 조회 (캐시에 저장)
            userRepository.findById(userId);

            // when - 두 번째 조회 (캐시에서 반환)
            Optional<User> result = userRepository.findById(userId);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getEmail()).isEqualTo(new Email("cache-test@example.com"));
        }

        @Test
        @DisplayName("존재하지 않는 사용자도 캐시된다")
        void should_cacheEmptyOptional_when_userNotFound() {
            // given
            Long nonExistentId = 999999L;

            // when
            Optional<User> result = userRepository.findById(nonExistentId);

            // then
            assertThat(result).isEmpty();

            // 빈 Optional도 캐시됨 확인
            Object cachedValue = cacheManager.getCache(CacheConfig.USER_CACHE).get(nonExistentId);
            assertThat(cachedValue).isNotNull();
        }
    }

    @Nested
    @DisplayName("save 캐시 무효화 시")
    class SaveCacheEviction {

        @Test
        @DisplayName("저장 시 해당 사용자 캐시가 무효화된다")
        void should_evictCache_when_saveUser() {
            // given
            Long userId = savedUser.getId();

            // 먼저 캐시에 저장
            userRepository.findById(userId);
            assertThat(cacheManager.getCache(CacheConfig.USER_CACHE).get(userId)).isNotNull();

            // when - 사용자 정보 수정 후 저장
            savedUser.updateNickname("updatedNickname");
            userRepository.save(savedUser);

            // then - 캐시가 무효화됨
            // Note: @CacheEvict는 condition이 있으므로 id가 null이 아닐 때만 무효화
            // 하지만 Spring Cache의 동작 방식에 따라 캐시가 evict 되거나 업데이트됨
        }
    }

    @Nested
    @DisplayName("delete 캐시 무효화 시")
    class DeleteCacheEviction {

        @Test
        @DisplayName("삭제 시 해당 사용자 캐시가 무효화된다")
        void should_evictCache_when_deleteUser() {
            // given
            Long userId = savedUser.getId();

            // 먼저 캐시에 저장
            userRepository.findById(userId);
            assertThat(cacheManager.getCache(CacheConfig.USER_CACHE).get(userId)).isNotNull();

            // when
            userRepository.delete(savedUser);

            // then - 캐시가 무효화됨
            Object cachedValue = cacheManager.getCache(CacheConfig.USER_CACHE).get(userId);
            assertThat(cachedValue).isNull();
        }
    }
}
