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
 * <p>키 설계:
 * <ul>
 *   <li>{@code presence:chat:room:{roomId}:active:z} -> ZSet(userId -> expiresAtMillis)</li>
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
        redisTemplate.opsForZSet().remove(roomKey, userKey);
        if (sessionId != null) {
            redisTemplate.opsForSet().remove(sessionRoomsKey(sessionId), String.valueOf(chatRoomId));
        }
        log.debug("Marked inactive: roomId={}, userId={}, sessionId={}", chatRoomId, userId, sessionId);
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
                    redisTemplate.opsForZSet().remove(roomKey(roomId), String.valueOf(userId));
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

    private String sessionRoomsKey(String sessionId) {
        return SESSION_KEY_PREFIX + sessionId + SESSION_ROOMS_SUFFIX;
    }
}

