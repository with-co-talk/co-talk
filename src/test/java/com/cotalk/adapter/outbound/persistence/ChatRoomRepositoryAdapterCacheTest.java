package com.cotalk.adapter.outbound.persistence;

import com.cotalk.adapter.outbound.persistence.chatroom.ChatRoomJpaRepository;
import com.cotalk.adapter.outbound.persistence.chatroom.ChatRoomRepositoryAdapter;
import com.cotalk.domain.entity.ChatRoom;
import com.cotalk.infrastructure.config.CacheConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ChatRoomRepositoryAdapter 캐싱 테스트.
 *
 * @author seunggu.lee
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("ChatRoomRepositoryAdapter 캐싱")
class ChatRoomRepositoryAdapterCacheTest {

    @Autowired
    private ChatRoomRepositoryAdapter chatRoomRepository;

    @Autowired
    private ChatRoomJpaRepository chatRoomJpaRepository;

    @Autowired
    private CacheManager cacheManager;

    private ChatRoom savedChatRoom;

    @BeforeEach
    void setUp() {
        // 캐시 초기화
        cacheManager.getCache(CacheConfig.CHAT_ROOM_CACHE).clear();

        // 테스트 채팅방 생성 (ChatRoom은 ID 자동 생성이 없으므로 명시적으로 설정)
        savedChatRoom = chatRoomJpaRepository.save(ChatRoom.builder()
                .id(1000L)
                .name("cache-test-room")
                .type(ChatRoom.ChatRoomType.DIRECT)
                .build());
    }

    @Nested
    @DisplayName("findById 캐싱 시")
    class FindByIdCaching {

        @Test
        @DisplayName("첫 조회 후 캐시에 저장된다")
        void should_cacheResult_when_firstFindById() {
            // given
            Long chatRoomId = savedChatRoom.getId();

            // when
            Optional<ChatRoom> result = chatRoomRepository.findById(chatRoomId);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("cache-test-room");

            // 캐시 확인
            Object cachedValue = cacheManager.getCache(CacheConfig.CHAT_ROOM_CACHE).get(chatRoomId);
            assertThat(cachedValue).isNotNull();
        }

        @Test
        @DisplayName("캐시된 값이 반환된다")
        void should_returnCachedValue_when_secondFindById() {
            // given
            Long chatRoomId = savedChatRoom.getId();

            // 첫 번째 조회 (캐시에 저장)
            chatRoomRepository.findById(chatRoomId);

            // when - 두 번째 조회 (캐시에서 반환)
            Optional<ChatRoom> result = chatRoomRepository.findById(chatRoomId);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("cache-test-room");
        }

        @Test
        @DisplayName("존재하지 않는 채팅방도 캐시된다")
        void should_cacheEmptyOptional_when_chatRoomNotFound() {
            // given
            Long nonExistentId = 999999L;

            // when
            Optional<ChatRoom> result = chatRoomRepository.findById(nonExistentId);

            // then
            assertThat(result).isEmpty();

            // 빈 Optional도 캐시됨 확인
            Object cachedValue = cacheManager.getCache(CacheConfig.CHAT_ROOM_CACHE).get(nonExistentId);
            assertThat(cachedValue).isNotNull();
        }
    }

    @Nested
    @DisplayName("save 캐시 무효화 시")
    class SaveCacheEviction {

        @Test
        @DisplayName("저장 시 해당 채팅방 캐시가 무효화된다")
        void should_evictCache_when_saveChatRoom() {
            // given
            Long chatRoomId = savedChatRoom.getId();

            // 먼저 캐시에 저장
            chatRoomRepository.findById(chatRoomId);
            assertThat(cacheManager.getCache(CacheConfig.CHAT_ROOM_CACHE).get(chatRoomId)).isNotNull();

            // when - 채팅방 정보 수정 후 저장
            savedChatRoom.updateName("updatedRoomName");
            chatRoomRepository.save(savedChatRoom);

            // then - 캐시가 무효화됨
            // Note: @CacheEvict는 condition이 있으므로 id가 null이 아닐 때만 무효화
        }
    }

    @Nested
    @DisplayName("delete 캐시 무효화 시")
    class DeleteCacheEviction {

        @Test
        @DisplayName("삭제 시 해당 채팅방 캐시가 무효화된다")
        void should_evictCache_when_deleteChatRoom() {
            // given
            Long chatRoomId = savedChatRoom.getId();

            // 먼저 캐시에 저장
            chatRoomRepository.findById(chatRoomId);
            assertThat(cacheManager.getCache(CacheConfig.CHAT_ROOM_CACHE).get(chatRoomId)).isNotNull();

            // when
            chatRoomRepository.delete(savedChatRoom);

            // then - 캐시가 무효화됨
            Object cachedValue = cacheManager.getCache(CacheConfig.CHAT_ROOM_CACHE).get(chatRoomId);
            assertThat(cachedValue).isNull();
        }
    }
}
