package com.cotalk.infrastructure.messaging;

import com.cotalk.domain.port.outbound.UserEventBroker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * 인메모리 기반 사용자 이벤트 브로커 구현체.
 * Redis가 비활성화된 단일 서버 환경에서 사용된다.
 *
 * @author seunggu.lee
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "false")
public class InMemoryUserEventBroker implements UserEventBroker {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 특정 사용자에게 채팅 목록 업데이트 이벤트를 발행한다.
     *
     * @param userId 대상 사용자 ID
     * @param event  채팅 목록 업데이트 이벤트
     */
    @Override
    public void publishChatListUpdate(Long userId, ChatListUpdateEvent event) {
        String destination = "/topic/user/" + userId + "/chat-list";
        log.debug("Publishing chat list update to user {}: {}", userId, event);
        messagingTemplate.convertAndSend(destination, event);
    }

    /**
     * 특정 사용자에게 읽음 상태 변경 이벤트를 발행한다.
     *
     * @param userId 대상 사용자 ID
     * @param event  읽음 상태 이벤트
     */
    @Override
    public void publishReadReceipt(Long userId, ReadReceiptEvent event) {
        String destination = "/topic/user/" + userId + "/read-receipt";
        log.debug("Publishing read receipt to user {}: {}", userId, event);
        messagingTemplate.convertAndSend(destination, event);
    }

    /**
     * 특정 사용자에게 온라인 상태 변경 이벤트를 발행한다.
     *
     * @param userId 대상 사용자 ID
     * @param event  온라인 상태 이벤트
     */
    @Override
    public void publishOnlineStatus(Long userId, OnlineStatusEvent event) {
        String destination = "/topic/user/" + userId + "/online-status";
        log.debug("Publishing online status to user {}: {}", userId, event);
        messagingTemplate.convertAndSend(destination, event);
    }

    /**
     * 특정 사용자에게 프로필 업데이트 이벤트를 발행한다.
     *
     * @param userId 대상 사용자 ID (프로필 변경을 알릴 사용자)
     * @param event  프로필 업데이트 이벤트
     */
    @Override
    public void publishProfileUpdate(Long userId, ProfileUpdateEvent event) {
        String destination = "/topic/user/" + userId + "/profile-update";
        log.debug("Publishing profile update to user {}: {}", userId, event);
        messagingTemplate.convertAndSend(destination, event);
    }
}
