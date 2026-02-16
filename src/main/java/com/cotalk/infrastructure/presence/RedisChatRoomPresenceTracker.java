package com.cotalk.infrastructure.presence;

import com.cotalk.domain.port.outbound.ChatRoomPresenceTracker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 기반 채팅방 활성 상태(현재 보고 있는 방) 트래커.
 *
 * <p>멀티 인스턴스 환경에서 같은 유저가 여러 세션으로 같은 방을 구독할 수 있으므로,
 * 세션 카운트 기반으로 활성/비활성을 관리한다.
 *
 * <p>키 설계:
 * <ul>
 *   <li>{@code presence:chat:room:{roomId}:active:z} -> ZSet(userId -> expiresAtMillis)</li>
 *   <li>{@code presence:chat:room:{roomId}:user:count:{userId}} -> String(세션 카운트)</li>
 *   <li>{@code presence:ws:session:{sessionId}:rooms} -> Set(roomId) (disconnect cleanup용)</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = true)
public class RedisChatRoomPresenceTracker implements ChatRoomPresenceTracker {

    private static final String ROOM_KEY_PREFIX = "presence:chat:room:";
    private static final String ROOM_ZSET_SUFFIX = ":active:z";
    private static final String USER_COUNT_INFIX = ":user:count:";
    private static final String SESSION_KEY_PREFIX = "presence:ws:session:";
    private static final String SESSION_ROOMS_SUFFIX = ":rooms";

    private static final long TTL_MILLIS = TimeUnit.SECONDS.toMillis(60);
    private static final long SESSION_TTL_MILLIS = TimeUnit.SECONDS.toMillis(90);

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void markActive(Long chatRoomId, Long userId, String sessionId) {
        if (chatRoomId == null || userId == null) return;
        long expiresAt = System.currentTimeMillis() + TTL_MILLIS;
        String roomKey = roomKey(chatRoomId);
        String userKey = String.valueOf(userId);
        redisTemplate.opsForZSet().add(roomKey, userKey, expiresAt);
        // 멀티 인스턴스 세션 카운트: 같은 유저가 여러 세션으로 같은 방을 구독할 수 있음
        String countKey = userCountKey(chatRoomId, userId);
        redisTemplate.opsForValue().increment(countKey);
        redisTemplate.expire(countKey, TTL_MILLIS + 10000, TimeUnit.MILLISECONDS);
        // disconnect cleanup용으로 session -> rooms 매핑 저장
        if (sessionId != null) {
            String sessionRoomsKey = sessionRoomsKey(sessionId);
            redisTemplate.opsForSet().add(sessionRoomsKey, String.valueOf(chatRoomId));
            redisTemplate.expire(sessionRoomsKey, SESSION_TTL_MILLIS, TimeUnit.MILLISECONDS);
        }
        log.debug("Marked active: roomId={}, userId={}, sessionId={}", chatRoomId, userId, sessionId);
    }

    @Override
    public void markInactive(Long chatRoomId, Long userId, String sessionId) {
        if (chatRoomId == null || userId == null) return;
        String roomKey = roomKey(chatRoomId);
        String userKey = String.valueOf(userId);
        // 세션 카운트 DECR → 0 이하일 때만 ZSet에서 제거 (멀티 인스턴스 안전)
        String countKey = userCountKey(chatRoomId, userId);
        Long count = redisTemplate.opsForValue().decrement(countKey);
        if (count == null || count <= 0) {
            redisTemplate.opsForZSet().remove(roomKey, userKey);
            redisTemplate.delete(countKey);
            log.debug("Marked inactive (last session): roomId={}, userId={}", chatRoomId, userId);
        } else {
            log.debug("Marked inactive (remaining sessions: {}): roomId={}, userId={}, sessionId={}",
                    count, chatRoomId, userId, sessionId);
        }
        if (sessionId != null) {
            redisTemplate.opsForSet().remove(sessionRoomsKey(sessionId), String.valueOf(chatRoomId));
        }
    }

    @Override
    public void clearSession(Long userId, String sessionId) {
        if (userId == null || sessionId == null) return;
        String sessionRoomsKey = sessionRoomsKey(sessionId);
        Set<String> rooms = redisTemplate.opsForSet().members(sessionRoomsKey);
        if (rooms != null) {
            for (String roomIdStr : rooms) {
                try {
                    Long roomId = Long.parseLong(roomIdStr);
                    // 세션 카운트 기반 정리: markInactive와 동일한 로직
                    String countKey = userCountKey(roomId, userId);
                    Long count = redisTemplate.opsForValue().decrement(countKey);
                    if (count == null || count <= 0) {
                        redisTemplate.opsForZSet().remove(roomKey(roomId), String.valueOf(userId));
                        redisTemplate.delete(countKey);
                    }
                } catch (NumberFormatException ignored) {
                    // ignore
                }
            }
        }
        redisTemplate.delete(sessionRoomsKey);
        log.debug("Cleared session: userId={}, sessionId={}", userId, sessionId);
    }

    @Override
    public boolean isActive(Long chatRoomId, Long userId) {
        if (chatRoomId == null || userId == null) return false;
        cleanupExpired(chatRoomId);
        Double score = redisTemplate.opsForZSet().score(roomKey(chatRoomId), String.valueOf(userId));
        return score != null && score > System.currentTimeMillis();
    }

    @Override
    public int countActiveMembers(Long chatRoomId) {
        if (chatRoomId == null) return 0;
        cleanupExpired(chatRoomId);
        Long count = redisTemplate.opsForZSet().count(roomKey(chatRoomId), System.currentTimeMillis(), Double.POSITIVE_INFINITY);
        return count == null ? 0 : Math.toIntExact(count);
    }

    private void cleanupExpired(Long chatRoomId) {
        // 만료된 엔트리 제거(조회 시점 정리)
        redisTemplate.opsForZSet().removeRangeByScore(roomKey(chatRoomId), Double.NEGATIVE_INFINITY, System.currentTimeMillis());
    }

    private String roomKey(Long chatRoomId) {
        return ROOM_KEY_PREFIX + chatRoomId + ROOM_ZSET_SUFFIX;
    }

    /**
     * 채팅방-사용자별 세션 카운트 키를 생성한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param userId     사용자 ID
     * @return Redis 키 (예: {@code presence:chat:room:1:user:count:42})
     */
    private String userCountKey(Long chatRoomId, Long userId) {
        return ROOM_KEY_PREFIX + chatRoomId + USER_COUNT_INFIX + userId;
    }

    private String sessionRoomsKey(String sessionId) {
        return SESSION_KEY_PREFIX + sessionId + SESSION_ROOMS_SUFFIX;
    }
}

