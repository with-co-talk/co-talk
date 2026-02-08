package com.cotalk.application.service.chat;

import com.cotalk.domain.port.inbound.chat.UpdatePresenceStatusUseCase;
import com.cotalk.domain.port.outbound.ChatRoomPresenceTracker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 채팅방 presence 상태 변경 유스케이스 구현체.
 * {@link ChatRoomPresenceTracker}에 위임하여 사용자의 활성/비활성 상태를 관리한다.
 *
 * @author seunggu.lee
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UpdatePresenceStatusService implements UpdatePresenceStatusUseCase {

    private final ChatRoomPresenceTracker chatRoomPresenceTracker;

    /**
     * {@inheritDoc}
     */
    @Override
    public void markActive(Long chatRoomId, Long userId, String sessionId) {
        chatRoomPresenceTracker.markActive(chatRoomId, userId, sessionId);
        log.debug("[WS] markActive roomId={}, userId={}, sessionId={}", chatRoomId, userId, sessionId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void markInactive(Long chatRoomId, Long userId, String sessionId) {
        chatRoomPresenceTracker.markInactive(chatRoomId, userId, sessionId);
        log.debug("[WS] markInactive roomId={}, userId={}, sessionId={}", chatRoomId, userId, sessionId);
    }
}
