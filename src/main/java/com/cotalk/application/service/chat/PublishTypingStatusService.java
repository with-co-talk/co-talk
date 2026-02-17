package com.cotalk.application.service.chat;

import com.cotalk.domain.entity.User;
import com.cotalk.domain.port.inbound.chat.PublishTypingStatusUseCase;
import com.cotalk.domain.port.outbound.ChatMessageBroker;
import com.cotalk.domain.port.outbound.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 타이핑 상태 발행 유스케이스 구현체.
 * 사용자의 타이핑 시작/중지 상태를 채팅방에 브로드캐스트한다.
 *
 * <p>닉네임 캐시를 사용하여 반복적인 DB 조회를 방지한다.
 * 캐시 항목은 1시간 후 만료되어 메모리 누수를 방지한다.</p>
 *
 * @author seunggu.lee
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PublishTypingStatusService implements PublishTypingStatusUseCase {

    private final UserRepository userRepository;
    private final ChatMessageBroker chatMessageBroker;
    private final Map<Long, CachedNickname> userNicknameCache = new ConcurrentHashMap<>();

    /** 캐시 TTL: 1시간 */
    private static final long CACHE_TTL_MILLIS = 3_600_000L;

    /** 캐시 정리 주기: 100회 호출마다 만료 항목 정리 */
    private static final int EVICTION_INTERVAL = 100;
    private int callCount = 0;

    /**
     * {@inheritDoc}
     *
     * <p>사용자의 닉네임을 조회(캐시 우선)하여 타이핑 이벤트를 Redis Pub/Sub으로 발행한다.</p>
     */
    @Override
    public void publishTypingStatus(Long chatRoomId, Long userId, boolean isTyping) {
        String userNickname = getCachedNickname(userId);

        String eventType = isTyping ? "TYPING" : "STOP_TYPING";
        chatMessageBroker.publishRoomEvent(chatRoomId, new TypingBroadcastEvent(
                1,
                "typing:" + chatRoomId + ":" + userId + ":" + System.currentTimeMillis(),
                eventType,
                chatRoomId,
                userId,
                userNickname,
                isTyping
        ));
        log.debug("[WS] publishTypingStatus roomId={}, userId={}, isTyping={}", chatRoomId, userId, isTyping);

        // 주기적으로 만료된 캐시 항목 정리
        if (++callCount % EVICTION_INTERVAL == 0) {
            evictExpiredEntries();
        }
    }

    /**
     * TTL이 적용된 닉네임 캐시에서 조회한다.
     * 캐시 미스 또는 만료 시 DB에서 조회하여 갱신한다.
     */
    private String getCachedNickname(Long userId) {
        CachedNickname cached = userNicknameCache.get(userId);
        if (cached != null && !cached.isExpired()) {
            return cached.nickname;
        }

        String nickname = userRepository.findById(userId)
                .map(User::getNickname)
                .orElse(null);
        userNicknameCache.put(userId, new CachedNickname(nickname, Instant.now()));
        return nickname;
    }

    /**
     * 만료된 캐시 항목을 제거한다.
     */
    private void evictExpiredEntries() {
        Iterator<Map.Entry<Long, CachedNickname>> it = userNicknameCache.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().isExpired()) {
                it.remove();
            }
        }
    }

    /**
     * TTL이 적용된 닉네임 캐시 항목.
     */
    private record CachedNickname(String nickname, Instant cachedAt) {
        boolean isExpired() {
            return Instant.now().toEpochMilli() - cachedAt.toEpochMilli() > CACHE_TTL_MILLIS;
        }
    }

    /**
     * 타이핑 브로드캐스트 이벤트 DTO.
     * Redis Pub/Sub -> WebSocket 방 토픽(/topic/chat/room/{roomId})으로 전달된다.
     */
    private record TypingBroadcastEvent(
            Integer schemaVersion,
            String eventId,
            String eventType,
            Long chatRoomId,
            Long userId,
            String userNickname,
            Boolean isTyping
    ) {}
}
