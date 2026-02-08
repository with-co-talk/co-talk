package com.cotalk.application.service.chat;

import com.cotalk.domain.entity.User;
import com.cotalk.domain.port.inbound.chat.PublishTypingStatusUseCase;
import com.cotalk.domain.port.outbound.ChatMessageBroker;
import com.cotalk.domain.port.outbound.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 타이핑 상태 발행 유스케이스 구현체.
 * 사용자의 타이핑 시작/중지 상태를 채팅방에 브로드캐스트한다.
 *
 * <p>닉네임 캐시를 사용하여 반복적인 DB 조회를 방지한다.</p>
 *
 * @author seunggu.lee
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PublishTypingStatusService implements PublishTypingStatusUseCase {

    private final UserRepository userRepository;
    private final ChatMessageBroker chatMessageBroker;
    private final Map<Long, String> userNicknameCache = new ConcurrentHashMap<>();

    /**
     * {@inheritDoc}
     *
     * <p>사용자의 닉네임을 조회(캐시 우선)하여 타이핑 이벤트를 Redis Pub/Sub으로 발행한다.</p>
     */
    @Override
    public void publishTypingStatus(Long chatRoomId, Long userId, boolean isTyping) {
        String userNickname = userNicknameCache.computeIfAbsent(userId,
                id -> userRepository.findById(id).map(User::getNickname).orElse(null));

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
