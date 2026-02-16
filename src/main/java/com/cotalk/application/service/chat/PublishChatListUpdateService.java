package com.cotalk.application.service.chat;

import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.entity.Message;
import com.cotalk.domain.port.inbound.chat.PublishChatListUpdateUseCase;
import com.cotalk.domain.port.outbound.MessageRepository;
import com.cotalk.domain.port.outbound.UserEventBroker;
import com.cotalk.domain.port.outbound.UserEventBroker.ChatListUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 채팅 목록 업데이트 발행 유스케이스 구현체.
 * 새 메시지 전송 시 채팅방 참여자들의 채팅 목록을 실시간으로 업데이트한다.
 *
 * <p>성능 최적화:
 * <ul>
 *   <li>배치 쿼리를 사용하여 모든 멤버의 unreadCount를 한 번에 조회 (N+1 쿼리 방지)</li>
 *   <li>{@code @Async}로 비동기 실행하여 메시지 전송 RTT에서 N번의 Redis publish 비용을 제거</li>
 * </ul>
 *
 * @author seunggu.lee
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PublishChatListUpdateService implements PublishChatListUpdateUseCase {

    private final MessageRepository messageRepository;
    private final UserEventBroker userEventBroker;

    /**
     * {@inheritDoc}
     *
     * <p>배치 쿼리로 모든 멤버의 unreadCount를 한 번에 조회하고,
     * 각 멤버에게 개별적으로 채팅 목록 업데이트 이벤트를 발행한다.</p>
     */
    @Async
    @Override
    public void publishChatListUpdate(Message message, List<ChatRoomMember> members, String senderNickname) {
        log.debug("Publishing chat list update for message: chatRoomId={}, messageId={}",
                message.getChatRoomId(), message.getId());

        log.debug("Found {} members in chat room {}", members.size(), message.getChatRoomId());

        // 배치 쿼리로 모든 멤버의 unreadCount를 한 번에 조회 (N+1 쿼리 방지)
        Map<Long, Long> unreadCountMap = messageRepository.batchCountUnreadMessagesForAllMembers(
                message.getChatRoomId());

        for (ChatRoomMember member : members) {
            int memberUnreadCount = unreadCountMap.getOrDefault(member.getUserId(), 0L).intValue();

            log.debug(
                    "[WS] chatListUpdate roomId={}, targetUserId={}, lastReadMessageId={}, memberUnreadCount={}",
                    message.getChatRoomId(),
                    member.getUserId(),
                    member.getLastReadMessageId(),
                    memberUnreadCount
            );

            ChatListUpdateEvent event = new ChatListUpdateEvent(
                    1,
                    "chat-list:" + message.getChatRoomId() + ":" + message.getId() + ":" + member.getUserId(),
                    "NEW_MESSAGE",
                    message.getChatRoomId(),
                    message.getContent(),
                    message.getType().name(),
                    message.getCreatedAt(),
                    message.getSenderId(),
                    senderNickname,
                    memberUnreadCount
            );

            log.debug("Publishing chat list update to user {}: roomId={}, unreadCount={}",
                    member.getUserId(), message.getChatRoomId(), memberUnreadCount);
            userEventBroker.publishChatListUpdate(member.getUserId(), event);
        }
    }
}
