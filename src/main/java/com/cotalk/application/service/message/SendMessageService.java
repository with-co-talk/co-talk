package com.cotalk.application.service.message;

import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.entity.Message;
import com.cotalk.domain.entity.Message.MessageType;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.port.inbound.message.SendMessageUseCase;
import com.cotalk.domain.port.inbound.notification.SendPushNotificationUseCase;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.ChatRoomPresenceTracker;
import com.cotalk.domain.port.outbound.IdGenerator;
import com.cotalk.domain.port.outbound.MessageRepository;
import com.cotalk.domain.port.outbound.UserRepository;
import com.cotalk.domain.util.HtmlSanitizer;
import com.cotalk.domain.validator.ChatRoomMemberValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
        // 채팅방 멤버인지 확인
        chatRoomMemberValidator.validateMembership(chatRoomId, senderId);

        // XSS 방지(텍스트 채팅): HTML 태그만 제거하고, 유니코드/특수문자는 그대로 유지한다.
        // (HTML 엔티티로 저장하면 클라이언트에 &hellip; 같은 문자열이 그대로 노출될 수 있다)
        String sanitizedContent = HtmlSanitizer.stripAllTags(content);

        // 메시지 생성
        Message message = Message.builder()
                .id(idGenerator.nextId())
                .chatRoomId(chatRoomId)
                .senderId(senderId)
                .content(sanitizedContent)
                .type(MessageType.TEXT)
                .build();

        // 내용 검증
        message.validateContent();

        Message savedMessage = messageRepository.save(message);

        // 발신자는 자신이 보낸 메시지를 읽은 것으로 간주하여 lastReadMessageId 업데이트
        // 메시지 전송 시점에 자동으로 읽음 처리하여 unreadCount 계산이 정확하게 이루어지도록 함
        LocalDateTime now = LocalDateTime.now();
        int updated = chatRoomMemberRepository.updateLastReadMessageIdIfNewer(
                chatRoomId, senderId, now, savedMessage.getId());
        if (updated > 0) {
            log.debug("Auto-updated sender's lastReadMessageId: userId={}, chatRoomId={}, messageId={}", 
                    senderId, chatRoomId, savedMessage.getId());
        }

        // 푸시 알림 전송 (비동기)
        sendPushNotificationsToOtherMembers(chatRoomId, senderId, content);

        return savedMessage;
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
        // 채팅방 멤버인지 확인
        chatRoomMemberValidator.validateMembership(chatRoomId, senderId);

        // 파일 메시지 생성
        Message message = Message.builder()
                .id(idGenerator.nextId())
                .chatRoomId(chatRoomId)
                .senderId(senderId)
                .content(command.fileName()) // 파일명을 content로 저장
                .type(command.getMessageType())
                .fileUrl(command.fileUrl())
                .fileName(command.fileName())
                .fileSize(command.fileSize())
                .fileContentType(command.contentType())
                .thumbnailUrl(command.thumbnailUrl())
                .build();

        // 내용 검증
        message.validateContent();

        Message savedMessage = messageRepository.save(message);

        // 발신자는 자신이 보낸 메시지를 읽은 것으로 간주하여 lastReadMessageId 업데이트
        // 메시지 전송 시점에 자동으로 읽음 처리하여 unreadCount 계산이 정확하게 이루어지도록 함
        LocalDateTime now = LocalDateTime.now();
        int updated = chatRoomMemberRepository.updateLastReadMessageIdIfNewer(
                chatRoomId, senderId, now, savedMessage.getId());
        if (updated > 0) {
            log.debug("Auto-updated sender's lastReadMessageId: userId={}, chatRoomId={}, messageId={}", 
                    senderId, chatRoomId, savedMessage.getId());
        }

        // 푸시 알림 전송 (비동기)
        String notificationContent = command.getMessageType() == MessageType.IMAGE 
                ? "📷 사진을 보냈습니다." 
                : "📎 파일을 보냈습니다: " + command.fileName();
        sendPushNotificationsToOtherMembers(chatRoomId, senderId, notificationContent);

        return savedMessage;
    }

    private void sendPushNotificationsToOtherMembers(Long chatRoomId, Long senderId, String content) {
        // 발신자 정보 조회
        String senderNickname = userRepository.findById(senderId)
                .map(User::getNickname)
                .orElse("알 수 없음");

        // 채팅방의 다른 멤버들 중 현재 채팅방을 보고 있지 않은 사용자만 필터링
        // (채팅방에 있는 사용자는 WebSocket으로 실시간 메시지를 받으므로 푸시 불필요)
        List<ChatRoomMember> members = chatRoomMemberRepository.findByChatRoomId(chatRoomId);
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
