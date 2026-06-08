package com.cotalk.infrastructure.presence;

import com.cotalk.domain.port.outbound.ChatRoomPresenceTracker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
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

    private static final long TTL_MILLIS = TimeUnit.SECONDS.toMillis(30);
    private static final long SESSION_TTL_MILLIS = TimeUnit.SECONDS.toMillis(45);

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void markActive(Long chatRoomId, Long userId, String sessionId) {
        if (chatRoomId == null || userId == null) return;
        long expiresAt = System.currentTimeMillis() + TTL_MILLIS;
        String roomKey = roomKey(chatRoomId);
        String userKey = String.valueOf(userId);
        // ZSet 점수(만료시각)는 ping 마다 항상 갱신한다.
        redisTemplate.opsForZSet().add(roomKey, userKey, expiresAt);

        // 멀티 인스턴스 세션 카운트: 같은 유저가 여러 세션으로 같은 방을 구독할 수 있다.
        // 단, 세션 카운트는 (방, 세션)당 1회만 증가해야 한다.
        // presence ping(20초 주기)도 markActive를 호출하므로, 매번 증가시키면
        // 카운트가 누적되어 markInactive로 0까지 감소하지 못하고 방을 나가도
        // TTL(30초) 만료 전까지 "보는 중"으로 남아 푸시가 억제되는 버그가 생긴다.
        // session -> rooms Set에 처음 추가될 때(SADD 반환=1)만 카운트를 증가시켜 멱등화한다.
        String countKey = userCountKey(chatRoomId, userId);
        if (sessionId != null) {
            String sessionRoomsKey = sessionRoomsKey(sessionId);
            Long added = redisTemplate.opsForSet().add(sessionRoomsKey, String.valueOf(chatRoomId));
            redisTemplate.expire(sessionRoomsKey, SESSION_TTL_MILLIS, TimeUnit.MILLISECONDS);
            if (added != null && added > 0) {
                // 이 세션이 이 방을 처음 활성화한 경우에만 카운트 증가 (ping 반복 증가 방지)
                redisTemplate.opsForValue().increment(countKey);
            }
        } else {
            // sessionId가 없는 예외적 경우: 멱등 보장이 불가하므로 기존 동작 유지
            redisTemplate.opsForValue().increment(countKey);
        }
        redisTemplate.expire(countKey, TTL_MILLIS + 10000, TimeUnit.MILLISECONDS);
        log.debug("Marked active: roomId={}, userId={}, sessionId={}", chatRoomId, userId, sessionId);
    }

    @Override
    public void markInactive(Long chatRoomId, Long userId, String sessionId) {
        if (chatRoomId == null || userId == null) return;
        String roomKey = roomKey(chatRoomId);
        String userKey = String.valueOf(userId);

        // 멱등성: 이 세션이 실제로 이 방에 활성 등록돼 있던 경우(SREM 반환=1)에만 카운트를 감소시킨다.
        // markActive가 (방, 세션)당 1회만 증가시키므로, 감소도 1회만 일어나야 대칭이 맞는다.
        // 중복 호출(예: presenceInactive + UNSUBSCRIBE)이 와도 카운트를 두 번 깎지 않는다.
        if (sessionId != null) {
            Long removed = redisTemplate.opsForSet().remove(sessionRoomsKey(sessionId), String.valueOf(chatRoomId));
            if (removed == null || removed == 0) {
                // 이미 비활성 처리된 세션 → 아무 것도 하지 않음
                log.debug("Marked inactive (already inactive): roomId={}, userId={}, sessionId={}",
                        chatRoomId, userId, sessionId);
                return;
            }
        }

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

    /**
     * Redis pipeline을 사용하여 여러 사용자의 활성 상태를 한 번에 조회한다.
     * 개별 isActive() 호출 대비 Redis 왕복 횟수를 2N → 2회로 감소시킨다.
     *
     * @param chatRoomId 채팅방 ID
     * @param userIds    확인할 사용자 ID 목록
     * @return 활성 상태인 사용자 ID의 Set
     */
    @Override
    public Set<Long> getActiveUserIds(Long chatRoomId, List<Long> userIds) {
        if (chatRoomId == null || userIds == null || userIds.isEmpty()) {
            return Set.of();
        }
        cleanupExpired(chatRoomId);
        String roomKey = roomKey(chatRoomId);
        long now = System.currentTimeMillis();

        List<Object> results = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            byte[] key = roomKey.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            for (Long userId : userIds) {
                connection.zSetCommands().zScore(key, String.valueOf(userId).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            return null;
        });

        Set<Long> activeIds = new HashSet<>();
        for (int i = 0; i < userIds.size(); i++) {
            Double score = (Double) results.get(i);
            if (score != null && score > now) {
                activeIds.add(userIds.get(i));
            }
        }
        return activeIds;
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

