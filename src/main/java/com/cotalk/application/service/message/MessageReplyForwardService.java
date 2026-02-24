package com.cotalk.application.service.message;

import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.entity.Message;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.ChatRoomAccessDeniedException;
import com.cotalk.domain.exception.MessageNotFoundException;
import com.cotalk.domain.exception.ResourceAccessDeniedException;
import com.cotalk.domain.port.inbound.message.MessageReplyForwardUseCase;
import com.cotalk.domain.port.outbound.ChatMessageBroker;
import com.cotalk.domain.port.outbound.ChatMessageBroker.ChatBroadcastMessage;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.IdGenerator;
import com.cotalk.domain.port.outbound.MessageRepository;
import com.cotalk.domain.port.outbound.UserEventBroker;
import com.cotalk.domain.port.outbound.UserEventBroker.ChatListUpdateEvent;
import com.cotalk.domain.port.outbound.UserRepository;
import com.cotalk.domain.util.HtmlSanitizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.util.List;

/**
 * 메시지 답장/전달 유스케이스 구현체.
 * 메시지에 답장하거나 다른 채팅방으로 전달한다.
 *
 * @author seunggu.lee
 */
@Service
@RequiredArgsConstructor
@Transactional
public class MessageReplyForwardService implements MessageReplyForwardUseCase {

    private final MessageRepository messageRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final IdGenerator idGenerator;
    private final ChatMessageBroker chatMessageBroker;
    private final UserRepository userRepository;
    private final UserEventBroker userEventBroker;

    /**
     * 메시지에 답장한다.
     * 원본 메시지와 동일한 채팅방에 답장 메시지를 생성한다.
     *
     * @param senderId 발신자 ID
     * @param originalMessageId 원본 메시지 ID
     * @param content 답장 내용
     * @return 생성된 답장 메시지
     * @throws MessageNotFoundException 원본 메시지가 존재하지 않는 경우
     * @throws ChatRoomAccessDeniedException 채팅방 멤버가 아닌 경우
     */
    @Override
    public Message replyToMessage(Long senderId, Long originalMessageId, String content) {
        Message originalMessage = getMessageOrThrow(originalMessageId);
        Long chatRoomId = originalMessage.getChatRoomId();

        if (originalMessage.isDeleted()) {
            throw ResourceAccessDeniedException.messageAlreadyDeleted();
        }

        validateChatRoomAccess(chatRoomId, senderId);

        String sanitizedContent = HtmlSanitizer.stripAllTags(content);

        Message replyMessage = Message.builder()
                .id(idGenerator.nextId())
                .chatRoomId(chatRoomId)
                .senderId(senderId)
                .content(sanitizedContent)
                .type(Message.MessageType.TEXT)
                .replyToMessageId(originalMessageId)
                .build();

        Message saved = messageRepository.save(replyMessage);
        broadcastMessage(saved, senderId, chatRoomId);
        return saved;
    }

    /**
     * 메시지를 다른 채팅방으로 전달한다.
     * 원본 메시지의 내용과 파일 정보를 복사하여 대상 채팅방에 새 메시지를 생성한다.
     *
     * @param senderId 발신자 ID
     * @param originalMessageId 원본 메시지 ID
     * @param targetChatRoomId 대상 채팅방 ID
     * @return 생성된 전달 메시지
     * @throws MessageNotFoundException 원본 메시지가 존재하지 않는 경우
     * @throws ChatRoomAccessDeniedException 원본 또는 대상 채팅방 멤버가 아닌 경우
     */
    @Override
    public Message forwardMessage(Long senderId, Long originalMessageId, Long targetChatRoomId) {
        Message originalMessage = getMessageOrThrow(originalMessageId);
        Long originalChatRoomId = originalMessage.getChatRoomId();

        if (originalMessage.isDeleted()) {
            throw ResourceAccessDeniedException.messageAlreadyDeleted();
        }

        // 원본 채팅방 접근 권한 확인
        validateChatRoomAccess(originalChatRoomId, senderId);
        // 대상 채팅방 접근 권한 확인
        validateChatRoomAccess(targetChatRoomId, senderId);

        Message forwardedMessage = Message.builder()
                .id(idGenerator.nextId())
                .chatRoomId(targetChatRoomId)
                .senderId(senderId)
                .content(originalMessage.getContent())
                .type(originalMessage.getType())
                .fileUrl(originalMessage.getFileUrl())
                .fileName(originalMessage.getFileName())
                .fileSize(originalMessage.getFileSize())
                .fileContentType(originalMessage.getFileContentType())
                .thumbnailUrl(originalMessage.getThumbnailUrl())
                .forwardedFromMessageId(originalMessageId)
                .build();

        Message saved = messageRepository.save(forwardedMessage);
        broadcastMessage(saved, senderId, targetChatRoomId);
        return saved;
    }

    private Message getMessageOrThrow(Long messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFoundException(messageId));
    }

    /**
     * 메시지를 Redis Pub/Sub으로 브로드캐스트하고 채팅 목록을 업데이트한다.
     */
    private void broadcastMessage(Message message, Long senderId, Long chatRoomId) {
        List<ChatRoomMember> members = chatRoomMemberRepository.findByChatRoomId(chatRoomId);
        User sender = userRepository.findById(senderId).orElse(null);
        String senderNickname = sender != null ? sender.getNickname() : "알 수 없음";
        String senderAvatarUrl = sender != null ? sender.getAvatarUrl() : null;
        int unreadCount = Math.max(0, members.size() - 1);

        ChatBroadcastMessage broadcastMsg = new ChatBroadcastMessage(
                message.getId(), message.getSenderId(), senderNickname, senderAvatarUrl,
                message.getChatRoomId(), message.getContent(), message.getType().name(),
                message.getCreatedAt().atZone(ZoneOffset.UTC).toInstant().toEpochMilli(),
                message.getFileUrl(), message.getFileName(), message.getFileSize(),
                message.getFileContentType(), message.getThumbnailUrl(),
                unreadCount, null, null, null);

        chatMessageBroker.publish(chatRoomId, broadcastMsg);

        for (ChatRoomMember member : members) {
            ChatListUpdateEvent event = new ChatListUpdateEvent(
                    1,
                    "chat-list:" + chatRoomId + ":" + message.getId() + ":" + member.getUserId(),
                    "NEW_MESSAGE", chatRoomId, message.getContent(), message.getType().name(),
                    message.getCreatedAt(), message.getSenderId(), senderNickname, 0);
            userEventBroker.publishChatListUpdate(member.getUserId(), event);
        }
    }

    private void validateChatRoomAccess(Long chatRoomId, Long userId) {
        if (!chatRoomMemberRepository.existsByChatRoomIdAndUserId(chatRoomId, userId)) {
            throw new ChatRoomAccessDeniedException(userId, chatRoomId);
        }
    }
}
