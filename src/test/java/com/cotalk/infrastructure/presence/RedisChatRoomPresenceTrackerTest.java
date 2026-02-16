package com.cotalk.infrastructure.presence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedisChatRoomPresenceTracker")
class RedisChatRoomPresenceTrackerTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @Mock
    private org.springframework.data.redis.core.SetOperations<String, String> setOperations;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisChatRoomPresenceTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new RedisChatRoomPresenceTracker(redisTemplate);
    }

    @Nested
    @DisplayName("markActive")
    class MarkActive {

        @Test
        @DisplayName("사용자를 활성 상태로 표시한다")
        void should_markUserActive() {
            // given
            Long chatRoomId = 100L;
            Long userId = 1L;
            String sessionId = "session-1";

            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(redisTemplate.opsForSet()).thenReturn(setOperations);
            when(zSetOperations.add(anyString(), anyString(), anyDouble())).thenReturn(true);
            when(valueOperations.increment(anyString())).thenReturn(1L);
            when(setOperations.add(anyString(), anyString())).thenReturn(1L);
            when(redisTemplate.expire(anyString(), anyLong(), any(java.util.concurrent.TimeUnit.class))).thenReturn(true);

            // when
            tracker.markActive(chatRoomId, userId, sessionId);

            // then
            verify(zSetOperations).add(anyString(), eq(String.valueOf(userId)), anyDouble());
            verify(valueOperations).increment(anyString());
            verify(setOperations).add(anyString(), eq(String.valueOf(chatRoomId)));
        }

        @Test
        @DisplayName("chatRoomId가 null이면 아무것도 하지 않는다")
        void should_doNothing_when_chatRoomIdIsNull() {
            // given
            Long userId = 1L;
            String sessionId = "session-1";

            // when
            tracker.markActive(null, userId, sessionId);

            // then
            verify(zSetOperations, never()).add(anyString(), anyString(), anyDouble());
        }

        @Test
        @DisplayName("userId가 null이면 아무것도 하지 않는다")
        void should_doNothing_when_userIdIsNull() {
            // given
            Long chatRoomId = 100L;
            String sessionId = "session-1";

            // when
            tracker.markActive(chatRoomId, null, sessionId);

            // then
            verify(zSetOperations, never()).add(anyString(), anyString(), anyDouble());
        }

        @Test
        @DisplayName("sessionId가 null이어도 활성 상태로 표시한다")
        void should_markActive_when_sessionIdIsNull() {
            // given
            Long chatRoomId = 100L;
            Long userId = 1L;

            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(zSetOperations.add(anyString(), anyString(), anyDouble())).thenReturn(true);
            when(valueOperations.increment(anyString())).thenReturn(1L);
            when(redisTemplate.expire(anyString(), anyLong(), any(java.util.concurrent.TimeUnit.class))).thenReturn(true);

            // when
            tracker.markActive(chatRoomId, userId, null);

            // then
            verify(zSetOperations).add(anyString(), eq(String.valueOf(userId)), anyDouble());
            verify(valueOperations).increment(anyString());
            verify(setOperations, never()).add(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("markInactive")
    class MarkInactive {

        @Test
        @DisplayName("사용자를 비활성 상태로 표시한다")
        void should_markUserInactive() {
            // given
            Long chatRoomId = 100L;
            Long userId = 1L;
            String sessionId = "session-1";

            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            when(redisTemplate.opsForSet()).thenReturn(setOperations);
            when(valueOperations.decrement(anyString())).thenReturn(0L);
            when(zSetOperations.remove(anyString(), anyString())).thenReturn(1L);
            when(redisTemplate.delete(anyString())).thenReturn(true);

            // when
            tracker.markInactive(chatRoomId, userId, sessionId);

            // then
            verify(valueOperations).decrement(anyString());
            verify(zSetOperations).remove(anyString(), eq(String.valueOf(userId)));
            verify(setOperations).remove(anyString(), eq(String.valueOf(chatRoomId)));
        }

        @Test
        @DisplayName("chatRoomId가 null이면 아무것도 하지 않는다")
        void should_doNothing_when_chatRoomIdIsNull() {
            // given
            Long userId = 1L;
            String sessionId = "session-1";

            // when
            tracker.markInactive(null, userId, sessionId);

            // then
            verify(zSetOperations, never()).remove(anyString(), anyString());
        }

        @Test
        @DisplayName("userId가 null이면 아무것도 하지 않는다")
        void should_doNothing_when_userIdIsNull() {
            // given
            Long chatRoomId = 100L;
            String sessionId = "session-1";

            // when
            tracker.markInactive(chatRoomId, null, sessionId);

            // then
            verify(zSetOperations, never()).remove(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("clearSession")
    class ClearSession {

        @Test
        @DisplayName("세션을 정리하고 관련된 모든 방에서 사용자를 제거한다")
        void should_clearSession() {
            // given
            Long userId = 1L;
            String sessionId = "session-1";
            Set<String> rooms = Set.of("100", "200");

            when(redisTemplate.opsForSet()).thenReturn(setOperations);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            when(setOperations.members(anyString())).thenReturn(rooms);
            when(valueOperations.decrement(anyString())).thenReturn(0L);
            when(zSetOperations.remove(anyString(), anyString())).thenReturn(1L);
            when(redisTemplate.delete(anyString())).thenReturn(true);

            // when
            tracker.clearSession(userId, sessionId);

            // then
            verify(setOperations).members(anyString());
            verify(valueOperations, times(2)).decrement(anyString());
            verify(zSetOperations, times(2)).remove(anyString(), eq(String.valueOf(userId)));
        }

        @Test
        @DisplayName("userId가 null이면 아무것도 하지 않는다")
        void should_doNothing_when_userIdIsNull() {
            // given
            String sessionId = "session-1";

            // when
            tracker.clearSession(null, sessionId);

            // then
            verify(setOperations, never()).members(anyString());
        }

        @Test
        @DisplayName("sessionId가 null이면 아무것도 하지 않는다")
        void should_doNothing_when_sessionIdIsNull() {
            // given
            Long userId = 1L;

            // when
            tracker.clearSession(userId, null);

            // then
            verify(setOperations, never()).members(anyString());
        }

        @Test
        @DisplayName("세션에 방이 없으면 정리만 한다")
        void should_clearOnly_when_noRooms() {
            // given
            Long userId = 1L;
            String sessionId = "session-1";

            when(redisTemplate.opsForSet()).thenReturn(setOperations);
            when(setOperations.members(anyString())).thenReturn(null);
            when(redisTemplate.delete(anyString())).thenReturn(true);

            // when
            tracker.clearSession(userId, sessionId);

            // then
            verify(setOperations).members(anyString());
            verify(zSetOperations, never()).remove(anyString(), anyString());
            verify(redisTemplate).delete(anyString());
        }
    }

    @Nested
    @DisplayName("isActive")
    class IsActive {

        @Test
        @DisplayName("활성 상태인 사용자는 true를 반환한다")
        void should_returnTrue_when_userIsActive() {
            // given
            Long chatRoomId = 100L;
            Long userId = 1L;
            long currentTime = System.currentTimeMillis();
            Double score = (double) (currentTime + 1000); // 미래 시간

            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            when(zSetOperations.score(anyString(), eq(String.valueOf(userId)))).thenReturn(score);
            when(zSetOperations.removeRangeByScore(anyString(), anyDouble(), anyDouble())).thenReturn(0L);

            // when
            boolean result = tracker.isActive(chatRoomId, userId);

            // then
            assertThat(result).isTrue();
            verify(zSetOperations).removeRangeByScore(anyString(), anyDouble(), anyDouble());
        }

        @Test
        @DisplayName("비활성 상태인 사용자는 false를 반환한다")
        void should_returnFalse_when_userIsInactive() {
            // given
            Long chatRoomId = 100L;
            Long userId = 1L;

            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            when(zSetOperations.score(anyString(), eq(String.valueOf(userId)))).thenReturn(null);
            when(zSetOperations.removeRangeByScore(anyString(), anyDouble(), anyDouble())).thenReturn(0L);

            // when
            boolean result = tracker.isActive(chatRoomId, userId);

            // then
            assertThat(result).isFalse();
            verify(zSetOperations).removeRangeByScore(anyString(), anyDouble(), anyDouble());
        }

        @Test
        @DisplayName("만료된 사용자는 false를 반환한다")
        void should_returnFalse_when_userExpired() {
            // given
            Long chatRoomId = 100L;
            Long userId = 1L;
            long currentTime = System.currentTimeMillis();
            Double score = (double) (currentTime - 1000); // 과거 시간

            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            when(zSetOperations.score(anyString(), eq(String.valueOf(userId)))).thenReturn(score);
            when(zSetOperations.removeRangeByScore(anyString(), anyDouble(), anyDouble())).thenReturn(0L);

            // when
            boolean result = tracker.isActive(chatRoomId, userId);

            // then
            assertThat(result).isFalse();
            verify(zSetOperations).removeRangeByScore(anyString(), anyDouble(), anyDouble());
        }

        @Test
        @DisplayName("chatRoomId가 null이면 false를 반환한다")
        void should_returnFalse_when_chatRoomIdIsNull() {
            // given
            Long userId = 1L;

            // when
            boolean result = tracker.isActive(null, userId);

            // then
            assertThat(result).isFalse();
            // null 체크로 인해 cleanupExpired가 호출되지 않음
            verify(zSetOperations, never()).removeRangeByScore(anyString(), anyDouble(), anyDouble());
        }

        @Test
        @DisplayName("userId가 null이면 false를 반환한다")
        void should_returnFalse_when_userIdIsNull() {
            // given
            Long chatRoomId = 100L;

            // when
            boolean result = tracker.isActive(chatRoomId, null);

            // then
            assertThat(result).isFalse();
            // null 체크로 인해 cleanupExpired가 호출되지 않음
            verify(zSetOperations, never()).removeRangeByScore(anyString(), anyDouble(), anyDouble());
        }
    }

    @Nested
    @DisplayName("countActiveMembers")
    class CountActiveMembers {

        @Test
        @DisplayName("활성 멤버 수를 반환한다")
        void should_returnActiveMemberCount() {
            // given
            Long chatRoomId = 100L;
            Long count = 3L;

            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            when(zSetOperations.count(anyString(), anyDouble(), eq(Double.POSITIVE_INFINITY)))
                    .thenReturn(count);
            when(zSetOperations.removeRangeByScore(anyString(), anyDouble(), anyDouble())).thenReturn(0L);

            // when
            int result = tracker.countActiveMembers(chatRoomId);

            // then
            assertThat(result).isEqualTo(3);
            verify(zSetOperations).removeRangeByScore(anyString(), anyDouble(), anyDouble());
        }

        @Test
        @DisplayName("활성 멤버가 없으면 0을 반환한다")
        void should_returnZero_when_noActiveMembers() {
            // given
            Long chatRoomId = 100L;

            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            when(zSetOperations.count(anyString(), anyDouble(), eq(Double.POSITIVE_INFINITY)))
                    .thenReturn(0L);
            when(zSetOperations.removeRangeByScore(anyString(), anyDouble(), anyDouble())).thenReturn(0L);

            // when
            int result = tracker.countActiveMembers(chatRoomId);

            // then
            assertThat(result).isEqualTo(0);
            verify(zSetOperations).removeRangeByScore(anyString(), anyDouble(), anyDouble());
        }

        @Test
        @DisplayName("count가 null이면 0을 반환한다")
        void should_returnZero_when_countIsNull() {
            // given
            Long chatRoomId = 100L;

            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            when(zSetOperations.count(anyString(), anyDouble(), eq(Double.POSITIVE_INFINITY)))
                    .thenReturn(null);
            when(zSetOperations.removeRangeByScore(anyString(), anyDouble(), anyDouble())).thenReturn(0L);

            // when
            int result = tracker.countActiveMembers(chatRoomId);

            // then
            assertThat(result).isEqualTo(0);
            verify(zSetOperations).removeRangeByScore(anyString(), anyDouble(), anyDouble());
        }

        @Test
        @DisplayName("chatRoomId가 null이면 0을 반환한다")
        void should_returnZero_when_chatRoomIdIsNull() {
            // when
            int result = tracker.countActiveMembers(null);

            // then
            assertThat(result).isEqualTo(0);
            // null 체크로 인해 cleanupExpired가 호출되지 않음
            verify(zSetOperations, never()).removeRangeByScore(anyString(), anyDouble(), anyDouble());
        }
    }
}
