package com.cotalk.infrastructure.presence;

import com.cotalk.domain.port.outbound.ChatRoomPresenceTracker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 인메모리 채팅방 활성 상태 트래커.
 * Redis가 비활성화된 단일 서버/테스트 환경에서 사용한다.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "false")
public class InMemoryChatRoomPresenceTracker implements ChatRoomPresenceTracker {

    private static final long TTL_MILLIS = PresenceTtl.ROOM_TTL_MILLIS;
    private static final int MAX_SESSION_ROOMS = 1000;

    /**
     * roomId -> (userId -> expiresAtMillis)
     */
    private final Map<Long, Map<Long, Long>> expiresAtByRoomUser = new ConcurrentHashMap<>();

    /**
     * sessionId -> rooms (disconnect cleanup용)
     */
    private final Map<String, Set<Long>> roomsBySession = new ConcurrentHashMap<>();

    @Override
    public void markActive(Long chatRoomId, Long userId, String sessionId) {
        if (chatRoomId == null || userId == null) return;
        long expiresAt = System.currentTimeMillis() + TTL_MILLIS;
        expiresAtByRoomUser
                .computeIfAbsent(chatRoomId, ignored -> new ConcurrentHashMap<>())
                .put(userId, expiresAt);
        if (sessionId != null) {
            roomsBySession
                    .computeIfAbsent(sessionId, ignored -> ConcurrentHashMap.newKeySet())
                    .add(chatRoomId);
            // 메모리 폭주 방지(비정상 케이스)
            if (roomsBySession.get(sessionId).size() > MAX_SESSION_ROOMS) {
                log.warn("Too many rooms tracked for sessionId={}, size={}", sessionId, roomsBySession.get(sessionId).size());
            }
        }
        log.debug("Marked active (in-memory): roomId={}, userId={}, sessionId={}", chatRoomId, userId, sessionId);
    }

    @Override
    public void markInactive(Long chatRoomId, Long userId, String sessionId) {
        if (chatRoomId == null || userId == null) return;
        Map<Long, Long> map = expiresAtByRoomUser.get(chatRoomId);
        if (map != null) map.remove(userId);
        if (sessionId != null) {
            Set<Long> rooms = roomsBySession.get(sessionId);
            if (rooms != null) rooms.remove(chatRoomId);
        }
        log.debug("Marked inactive (in-memory): roomId={}, userId={}, sessionId={}", chatRoomId, userId, sessionId);
    }

    @Override
    public void clearSession(Long userId, String sessionId) {
        if (sessionId == null || userId == null) return;
        Set<Long> rooms = roomsBySession.remove(sessionId);
        if (rooms == null) return;
        for (Long roomId : rooms) {
            Map<Long, Long> map = expiresAtByRoomUser.get(roomId);
            if (map != null) map.remove(userId);
        }
        log.debug("Cleared session (in-memory): userId={}, sessionId={}, rooms={}", userId, sessionId, rooms.size());
    }

    @Override
    public boolean isActive(Long chatRoomId, Long userId) {
        if (chatRoomId == null || userId == null) return false;
        cleanupExpired(chatRoomId);
        Map<Long, Long> map = expiresAtByRoomUser.get(chatRoomId);
        if (map == null) return false;
        Long expiresAt = map.get(userId);
        return expiresAt != null && expiresAt > System.currentTimeMillis();
    }

    @Override
    public Set<Long> getActiveUserIds(Long chatRoomId, List<Long> userIds) {
        if (chatRoomId == null || userIds == null || userIds.isEmpty()) {
            return Set.of();
        }
        cleanupExpired(chatRoomId);
        Map<Long, Long> map = expiresAtByRoomUser.get(chatRoomId);
        if (map == null) return Set.of();
        long now = System.currentTimeMillis();
        Set<Long> activeIds = new HashSet<>();
        for (Long userId : userIds) {
            Long expiresAt = map.get(userId);
            if (expiresAt != null && expiresAt > now) {
                activeIds.add(userId);
            }
        }
        return activeIds;
    }

    @Override
    public int countActiveMembers(Long chatRoomId) {
        if (chatRoomId == null) return 0;
        cleanupExpired(chatRoomId);
        Map<Long, Long> map = expiresAtByRoomUser.get(chatRoomId);
        return map == null ? 0 : map.size();
    }

    private void cleanupExpired(Long chatRoomId) {
        Map<Long, Long> map = expiresAtByRoomUser.get(chatRoomId);
        if (map == null || map.isEmpty()) return;
        long now = System.currentTimeMillis();
        map.entrySet().removeIf(e -> e.getValue() == null || e.getValue() <= now);
    }
}

