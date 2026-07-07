package com.cotalk.application.service.message;

import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.entity.Message;
import com.cotalk.domain.port.outbound.ChatMessageBroker;
import com.cotalk.domain.port.outbound.ChatMessageBroker.ChatBroadcastMessage;
import com.cotalk.domain.port.outbound.FileStorage;
import com.cotalk.domain.port.outbound.MessageRepository;
import com.cotalk.domain.port.outbound.UserEventBroker;
import com.cotalk.domain.port.outbound.UserEventBroker.ChatListUpdateEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
public class MessageBroadcastService {

    private final ChatMessageBroker chatMessageBroker;
    private final UserEventBroker userEventBroker;
    private final MessageRepository messageRepository;
    private final FileStorage fileStorage;
    private final int presignedUrlExpiryMinutes;

    /**
     * MessageBroadcastService를 생성한다.
     *
     * @param chatMessageBroker          채팅 메시지 브로커 포트
     * @param userEventBroker            사용자 이벤트 브로커 포트
     * @param messageRepository          메시지 저장소 포트
     * @param fileStorage                파일 저장소 포트(첨부파일 Pre-signed URL 재발급용)
     * @param presignedUrlExpiryMinutes  첨부파일 Pre-signed URL 만료 시간(분)
     */
    public MessageBroadcastService(
            ChatMessageBroker chatMessageBroker,
            UserEventBroker userEventBroker,
            MessageRepository messageRepository,
            FileStorage fileStorage,
            @Value("${minio.presigned-url-expiry-minutes:10}") int presignedUrlExpiryMinutes) {
        this.chatMessageBroker = chatMessageBroker;
        this.userEventBroker = userEventBroker;
        this.messageRepository = messageRepository;
        this.fileStorage = fileStorage;
        this.presignedUrlExpiryMinutes = presignedUrlExpiryMinutes;
    }

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

        log.debug("[MessageBroadcastService] broadcastToRedis roomId={}, messageId={}, senderId={}, type={}",
                message.getChatRoomId(), message.getId(), message.getSenderId(), message.getType());

        // 첨부파일 URL은 단기 Pre-signed URL로 재발급해 브로드캐스트한다(H-1). 수신자는 채팅방 멤버로,
        // WebSocket 구독 자체가 멤버십으로 게이트되어 있으므로 검증된 멤버에게만 전달된다.
        String fileUrl = fileStorage.presignAttachmentUrl(message.getFileUrl(), presignedUrlExpiryMinutes);
        String thumbnailUrl = fileStorage.presignAttachmentUrl(message.getThumbnailUrl(), presignedUrlExpiryMinutes);

        ChatBroadcastMessage broadcastMessage = new ChatBroadcastMessage(
                message.getId(),
                message.getSenderId(),
                senderNickname,
                senderAvatarUrl,
                message.getChatRoomId(),
                message.getContent(),
                message.getType().name(),
                message.getCreatedAt().atZone(ZoneOffset.UTC).toInstant().toEpochMilli(),
                fileUrl,
                message.getFileName(),
                message.getFileSize(),
                message.getFileContentType(),
                thumbnailUrl,
                unreadCount,
                null, null, null,
                null  // clientMessageId (해당 경로 없음)
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
