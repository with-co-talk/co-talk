package com.cotalk.application.service.message;

import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.entity.Message;
import com.cotalk.domain.entity.Message.MessageType;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.port.inbound.message.SendMessageUseCase;
import com.cotalk.domain.port.inbound.notification.SendPushNotificationUseCase;
import com.cotalk.domain.port.outbound.ChatMessageBroker;
import com.cotalk.domain.port.outbound.ChatMessageBroker.ChatBroadcastMessage;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.ChatRoomPresenceTracker;
import com.cotalk.domain.port.outbound.IdGenerator;
import com.cotalk.domain.port.outbound.MessageRepository;
import com.cotalk.domain.port.outbound.UserEventBroker;
import com.cotalk.domain.port.outbound.UserEventBroker.ChatListUpdateEvent;
import com.cotalk.domain.port.outbound.UserRepository;
import com.cotalk.domain.util.HtmlSanitizer;
import com.cotalk.domain.validator.ChatRoomMemberValidator;
import com.cotalk.infrastructure.metrics.CustomMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * 메시지 전송 유스케이스 구현체.
 * 텍스트 메시지와 파일 메시지를 전송한다.
 *
 * @author seunggu.lee
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SendMessageService implements SendMessageUseCase {

    private final MessageRepository messageRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final UserRepository userRepository;
    private final IdGenerator idGenerator;
    private final SendPushNotificationUseCase sendPushNotificationUseCase;
    private final ChatRoomMemberValidator chatRoomMemberValidator;
    private final ChatRoomPresenceTracker chatRoomPresenceTracker;
    private final CustomMetrics customMetrics;
    private final MessageLinkPreviewService messageLinkPreviewService;
    private final ChatMessageBroker chatMessageBroker;
    private final UserEventBroker userEventBroker;

    /**
     * 텍스트 메시지를 전송한다.
     * 채팅방의 다른 멤버들에게 푸시 알림을 전송한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param senderId 발신자 ID
     * @param content 메시지 내용
     * @return 전송된 메시지
     * @throws ChatRoomAccessDeniedException 채팅방 멤버가 아닌 경우
     */
    @Override
    public Message sendMessage(Long chatRoomId, Long senderId, String content) {
        return sendMessageWithContext(chatRoomId, senderId, content).message();
    }

    /**
     * 파일 메시지를 전송한다.
     * 이미지 또는 파일을 첨부한 메시지를 전송하고, 채팅방의 다른 멤버들에게 푸시 알림을 전송한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param senderId 발신자 ID
     * @param command 파일 메시지 정보
     * @return 전송된 파일 메시지
     * @throws ChatRoomAccessDeniedException 채팅방 멤버가 아닌 경우
     */
    @Override
    public Message sendFileMessage(Long chatRoomId, Long senderId, FileMessageCommand command) {
        return sendFileMessageWithContext(chatRoomId, senderId, command).message();
    }

    @Override
    public SendResult sendMessageWithContext(Long chatRoomId, Long senderId, String content) {
        // XSS 방지(텍스트 채팅): HTML 태그만 제거하고, 유니코드/특수문자는 그대로 유지한다.
        // (HTML 엔티티로 저장하면 클라이언트에 &hellip; 같은 문자열이 그대로 노출될 수 있다)
        String sanitizedContent = HtmlSanitizer.stripAllTags(content);

        if (sanitizedContent != null && sanitizedContent.length() > 5000) {
            throw new IllegalArgumentException("메시지는 5000자를 초과할 수 없습니다.");
        }

        Message message = Message.builder()
                .id(idGenerator.nextId())
                .chatRoomId(chatRoomId)
                .senderId(senderId)
                .content(sanitizedContent)
                .type(MessageType.TEXT)
                .build();

        SendResult result = doSendMessage(chatRoomId, senderId, message, content);

        // 링크 미리보기 수집 (비동기): 텍스트에 URL이 있으면 OG 메타 수집 후 메시지에 저장
        messageLinkPreviewService.extractFirstUrl(sanitizedContent)
                .ifPresent(url -> messageLinkPreviewService.fetchAndSaveLinkPreview(result.message().getId(), url));

        return result;
    }

    @Override
    public SendResult sendFileMessageWithContext(Long chatRoomId, Long senderId, FileMessageCommand command) {
        Message message = Message.builder()
                .id(idGenerator.nextId())
                .chatRoomId(chatRoomId)
                .senderId(senderId)
                .content(command.fileName())
                .type(command.getMessageType())
                .fileUrl(command.fileUrl())
                .fileName(command.fileName())
                .fileSize(command.fileSize())
                .fileContentType(command.contentType())
                .thumbnailUrl(command.thumbnailUrl())
                .build();

        String notificationContent = command.getMessageType() == MessageType.IMAGE
                ? "📷 사진을 보냈습니다."
                : "📎 파일을 보냈습니다: " + command.fileName();

        return doSendMessage(chatRoomId, senderId, message, notificationContent);
    }

    /**
     * 메시지 저장을 위한 공통 로직을 실행하고, 사전 조회한 컨텍스트를 함께 반환한다.
     * sender와 members를 한 번만 조회하여 중복 DB 쿼리를 제거한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param senderId 발신자 ID
     * @param message 저장할 메시지
     * @param notificationContent 푸시 알림 내용
     * @return 저장된 메시지와 브로드캐스트 컨텍스트
     */
    private SendResult doSendMessage(Long chatRoomId, Long senderId, Message message, String notificationContent) {
        // Pre-fetch ONCE: 중복 쿼리 방지
        List<ChatRoomMember> members = chatRoomMemberRepository.findByChatRoomId(chatRoomId);
        User sender = userRepository.findById(senderId).orElse(null);
        String senderNickname = sender != null ? sender.getNickname() : "알 수 없음";
        String senderAvatarUrl = sender != null ? sender.getAvatarUrl() : null;

        // Validate membership using pre-fetched members (별도 쿼리 없음)
        boolean isMember = members.stream().anyMatch(m -> m.getUserId().equals(senderId));
        if (!isMember) {
            throw new com.cotalk.domain.exception.ChatRoomAccessDeniedException(chatRoomId, senderId);
        }

        // 내용 검증
        message.validateContent();

        // 메시지 저장
        Message savedMessage = messageRepository.save(message);
        customMetrics.incrementMessagesSent();

        // 발신자는 자신이 보낸 메시지를 읽은 것으로 간주하여 lastReadMessageId 업데이트
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        int updated = chatRoomMemberRepository.updateLastReadMessageIdIfNewer(
                chatRoomId, senderId, now, savedMessage.getId());
        if (updated > 0) {
            log.debug("Auto-updated sender's lastReadMessageId: userId={}, chatRoomId={}, messageId={}",
                    senderId, chatRoomId, savedMessage.getId());
        }

        // 푸시 알림 전송 (사전 조회된 데이터 사용, 추가 DB 쿼리 없음)
        sendPushNotificationsToOtherMembers(chatRoomId, senderId, notificationContent, senderNickname, members);

        return new SendResult(savedMessage, senderNickname, senderAvatarUrl, members);
    }

    /**
     * 파일 메시지를 전송하고 WebSocket 브로드캐스트까지 내부에서 처리한다.
     * REST 컨트롤러가 outbound 포트에 직접 의존하지 않도록 브로드캐스트 로직을 서비스 내부에 캡슐화한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param senderId 발신자 ID
     * @param command 파일 메시지 명령
     * @return 전송된 메시지
     */
    @Override
    public Message sendFileMessageAndBroadcast(Long chatRoomId, Long senderId, FileMessageCommand command) {
        SendResult result = sendFileMessageWithContext(chatRoomId, senderId, command);
        broadcastToRedis(result.message(), result.senderNickname(), result.senderAvatarUrl(), result.members());
        return result.message();
    }

    @Override
    public Message sendTextMessageAndBroadcast(Long chatRoomId, Long senderId, String content) {
        SendResult result = sendMessageWithContext(chatRoomId, senderId, content);
        broadcastToRedis(result.message(), result.senderNickname(), result.senderAvatarUrl(), result.members());
        return result.message();
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
    private void broadcastToRedis(Message message, String senderNickname, String senderAvatarUrl,
                                   List<ChatRoomMember> members) {
        int unreadCount = Math.max(0, members.size() - 1);

        log.info("[SendMessageService] broadcastToRedis roomId={}, messageId={}, senderId={}, type={}",
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

    /**
     * 채팅방의 다른 멤버들에게 푸시 알림을 전송한다.
     * 사전 조회된 sender와 members 정보를 사용하여 추가 DB 쿼리를 방지한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param senderId 발신자 ID
     * @param content 알림 내용
     * @param senderNickname 발신자 닉네임 (사전 조회됨)
     * @param members 채팅방 멤버 목록 (사전 조회됨)
     */
    private void sendPushNotificationsToOtherMembers(Long chatRoomId, Long senderId, String content,
                                                      String senderNickname, List<ChatRoomMember> members) {
        // 채팅방의 다른 멤버들 중 현재 채팅방을 보고 있지 않은 사용자만 필터링
        // (채팅방에 있는 사용자는 WebSocket으로 실시간 메시지를 받으므로 푸시 불필요)
        List<Long> receiverUserIds = members.stream()
                .map(ChatRoomMember::getUserId)
                .filter(userId -> !userId.equals(senderId))
                .filter(userId -> !chatRoomPresenceTracker.isActive(chatRoomId, userId))
                .toList();

        // 벌크 푸시 알림 전송 (한 번의 호출로 처리)
        if (!receiverUserIds.isEmpty()) {
            sendPushNotificationUseCase.sendNewMessageNotificationBulk(
                    receiverUserIds,
                    senderNickname,
                    content,
                    chatRoomId
            );
            log.debug("Push notification sent to {} users (excluded {} active users in room)",
                    receiverUserIds.size(),
                    members.size() - receiverUserIds.size() - 1);
        }
    }
}
