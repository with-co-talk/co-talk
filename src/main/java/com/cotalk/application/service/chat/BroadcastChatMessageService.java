package com.cotalk.application.service.chat;

import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.entity.Message;
import com.cotalk.domain.port.inbound.chat.BroadcastChatMessageUseCase;
import com.cotalk.domain.port.outbound.ChatMessageBroker;
import com.cotalk.domain.port.outbound.ChatMessageBroker.ChatBroadcastMessage;
import com.cotalk.domain.port.outbound.FileStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
public class BroadcastChatMessageService implements BroadcastChatMessageUseCase {

    private final ChatMessageBroker chatMessageBroker;
    private final FileStorage fileStorage;
    private final int presignedUrlExpiryMinutes;

    /**
     * BroadcastChatMessageService를 생성한다.
     *
     * @param chatMessageBroker          채팅 메시지 브로커 포트
     * @param fileStorage                파일 저장소 포트(첨부파일 Pre-signed URL 재발급용)
     * @param presignedUrlExpiryMinutes  첨부파일 Pre-signed URL 만료 시간(분)
     */
    public BroadcastChatMessageService(
            ChatMessageBroker chatMessageBroker,
            FileStorage fileStorage,
            @Value("${minio.presigned-url-expiry-minutes:10}") int presignedUrlExpiryMinutes) {
        this.chatMessageBroker = chatMessageBroker;
        this.fileStorage = fileStorage;
        this.presignedUrlExpiryMinutes = presignedUrlExpiryMinutes;
    }

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

        // 첨부파일 URL은 단기 Pre-signed URL로 재발급해 브로드캐스트한다(H-1).
        // 수신자는 WebSocket 구독이 멤버십으로 게이트된 채팅방 멤버다.
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
                null,  // eventType (일반 메시지)
                null,  // relatedUserId
                null   // relatedUserNickname
        );

        chatMessageBroker.publish(message.getChatRoomId(), broadcastMessage);
    }
}
