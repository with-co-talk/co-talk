package com.cotalk.application.service.chat;

import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.entity.Message;
import com.cotalk.domain.port.inbound.chat.BroadcastChatMessageUseCase;
import com.cotalk.domain.port.outbound.ChatMessageBroker;
import com.cotalk.domain.port.outbound.ChatMessageBroker.ChatBroadcastMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.util.List;

/**
 * 채팅 메시지 브로드캐스트 유스케이스 구현체.
 * 저장된 메시지를 Redis Pub/Sub을 통해 채팅방 참여자들에게 브로드캐스트한다.
 *
 * <p>카톡/라인 방식: 발신자를 제외한 모든 멤버가 읽지 않은 상태로 시작하므로
 * unreadCount = 멤버 수 - 1 로 설정한다.</p>
 *
 * @author seunggu.lee
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BroadcastChatMessageService implements BroadcastChatMessageUseCase {

    private final ChatMessageBroker chatMessageBroker;

    /**
     * {@inheritDoc}
     */
    @Override
    public void broadcastMessage(Message message, String senderNickname, String senderAvatarUrl,
                                 List<ChatRoomMember> members) {
        // 카톡/라인 방식: 발신자를 제외한 모든 멤버가 읽지 않은 상태로 시작
        int unreadCount = Math.max(0, members.size() - 1);

        log.debug(
                "[WS] broadcastMessage roomId={}, messageId={}, senderId={}, senderNickname={}, totalMembers={}, unreadCount={}",
                message.getChatRoomId(), message.getId(), message.getSenderId(),
                senderNickname, members.size(), unreadCount);

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
                null,  // eventType (일반 메시지)
                null,  // relatedUserId
                null   // relatedUserNickname
        );

        chatMessageBroker.publish(message.getChatRoomId(), broadcastMessage);
    }
}
