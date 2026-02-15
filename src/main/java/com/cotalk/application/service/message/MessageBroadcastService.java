package com.cotalk.application.service.message;

import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.entity.Message;
import com.cotalk.domain.port.outbound.ChatMessageBroker;
import com.cotalk.domain.port.outbound.ChatMessageBroker.ChatBroadcastMessage;
import com.cotalk.domain.port.outbound.MessageRepository;
import com.cotalk.domain.port.outbound.UserEventBroker;
import com.cotalk.domain.port.outbound.UserEventBroker.ChatListUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.util.List;

/**
 * 메시지 브로드캐스트 서비스.
 * Redis Pub/Sub를 통해 채팅 메시지와 채팅 목록 업데이트 이벤트를 브로드캐스트한다.
 *
 * @author seunggu.lee
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageBroadcastService {

    private final ChatMessageBroker chatMessageBroker;
    private final UserEventBroker userEventBroker;
    private final MessageRepository messageRepository;

    /**
     * 메시지를 Redis Pub/Sub으로 브로드캐스트한다.
     * 사전 조회된 sender와 members 정보를 사용하여 추가 DB 쿼리를 방지한다.
     *
     * @param message 발행할 메시지
     * @param senderNickname 발신자 닉네임
     * @param senderAvatarUrl 발신자 프로필 이미지 URL
     * @param members 채팅방 멤버 목록
     */
    public void broadcastToRedis(Message message, String senderNickname, String senderAvatarUrl,
                                  List<ChatRoomMember> members) {
        int unreadCount = Math.max(0, members.size() - 1);

        log.info("[MessageBroadcastService] broadcastToRedis roomId={}, messageId={}, senderId={}, type={}",
                message.getChatRoomId(), message.getId(), message.getSenderId(), message.getType());

        ChatBroadcastMessage broadcastMessage = new ChatBroadcastMessage(
                message.getId(),
                message.getSenderId(),
                senderNickname,
                senderAvatarUrl,
                message.getChatRoomId(),
                message.getContent(),
                message.getType().name(),
                message.getCreatedAt().atZone(ZoneOffset.UTC).toInstant().toEpochMilli(),
                message.getFileUrl(),
                message.getFileName(),
                message.getFileSize(),
                message.getFileContentType(),
                message.getThumbnailUrl(),
                unreadCount,
                null, null, null
        );

        chatMessageBroker.publish(message.getChatRoomId(), broadcastMessage);
        broadcastChatListUpdate(message, members, senderNickname);
    }

    /**
     * 채팅 목록 업데이트 이벤트를 채팅방 참여자들에게 브로드캐스트한다.
     *
     * @param message 전송된 메시지
     * @param members 채팅방 멤버 목록
     * @param senderNickname 발신자 닉네임
     */
    private void broadcastChatListUpdate(Message message, List<ChatRoomMember> members, String senderNickname) {
        for (ChatRoomMember member : members) {
            Long lastReadMessageId = member.getLastReadMessageId();
            int memberUnreadCount = (int) messageRepository.countUnreadMessagesByLastReadMessageId(
                    message.getChatRoomId(),
                    member.getUserId(),
                    lastReadMessageId
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

            userEventBroker.publishChatListUpdate(member.getUserId(), event);
        }
    }
}
