package com.cotalk.application.service.message;

import com.cotalk.domain.constants.MessageConstants;
import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.entity.Message;
import com.cotalk.domain.entity.Message.MessageType;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.port.inbound.message.SendMessageUseCase;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.ChatRoomPresenceTracker;
import com.cotalk.domain.port.outbound.IdGenerator;
import com.cotalk.domain.port.outbound.MessageRepository;
import com.cotalk.domain.port.outbound.MetricsPort;
import com.cotalk.domain.port.outbound.NotificationCommandPort;
import com.cotalk.domain.port.outbound.TimeProvider;
import com.cotalk.domain.port.outbound.UserRepository;
import com.cotalk.domain.util.HtmlSanitizer;
import com.cotalk.domain.validator.FileMessageValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 메시지 전송 유스케이스 구현체.
 * 텍스트 메시지와 파일 메시지를 전송한다.
 *
 * <p>성능 최적화: DB 작업만 {@code TransactionTemplate}으로 트랜잭션 래핑하고,
 * 푸시 알림(Redis 호출)은 트랜잭션 밖에서 실행하여 DB 커넥션 점유 시간을 최소화한다.</p>
 *
 * @author seunggu.lee
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SendMessageService implements SendMessageUseCase {

    private final MessageRepository messageRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final UserRepository userRepository;
    private final IdGenerator idGenerator;
    private final NotificationCommandPort notificationCommandPort;
    private final ChatRoomPresenceTracker chatRoomPresenceTracker;
    private final MetricsPort customMetrics;
    private final MessageLinkPreviewService messageLinkPreviewService;
    private final MessageBroadcastService messageBroadcastService;
    private final TransactionTemplate transactionTemplate;
    private final TimeProvider timeProvider;
    private final FileMessageValidator fileMessageValidator;

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

        if (sanitizedContent != null && sanitizedContent.length() > MessageConstants.MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("메시지는 " + MessageConstants.MAX_MESSAGE_LENGTH + "자를 초과할 수 없습니다.");
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
        // 서버사이드 검증: 클라이언트가 보낸 contentType/fileUrl을 신뢰하지 않고 재검증한다.
        // - contentType: 업로드 허용 MIME 목록 재검증
        // - fileUrl/thumbnailUrl: 본 서버 업로드 경로(uploads/{senderId}/...) 소유 여부 검증
        fileMessageValidator.validate(senderId, command.contentType(), command.fileUrl(), command.thumbnailUrl());

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
     * <p>성능 최적화: DB 작업(조회+저장)만 트랜잭션으로 래핑하고,
     * 푸시 알림(Redis 호출)은 트랜잭션 밖에서 실행하여 DB 커넥션 점유를 최소화한다.</p>
     *
     * @param chatRoomId 채팅방 ID
     * @param senderId 발신자 ID
     * @param message 저장할 메시지
     * @param notificationContent 푸시 알림 내용
     * @return 저장된 메시지와 브로드캐스트 컨텍스트
     */
    private SendResult doSendMessage(Long chatRoomId, Long senderId, Message message, String notificationContent) {
        var timerSample = customMetrics.startMessageProcessingTimer();

        // DB 작업만 트랜잭션으로 래핑 (커넥션 점유 최소화)
        SendResult result = transactionTemplate.execute(status -> {
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
            LocalDateTime now = timeProvider.now();
            int updated = chatRoomMemberRepository.updateLastReadMessageIdIfNewer(
                    chatRoomId, senderId, now, savedMessage.getId());
            if (updated > 0) {
                log.debug("Auto-updated sender's lastReadMessageId: userId={}, chatRoomId={}, messageId={}",
                        senderId, chatRoomId, savedMessage.getId());
            }

            return new SendResult(savedMessage, senderNickname, senderAvatarUrl, members);
        });

        // TransactionTemplate.execute()의 반환값이 null인 경우 예외 처리
        if (result == null) {
            throw new IllegalStateException("트랜잭션 실행 결과가 null입니다. chatRoomId=" + chatRoomId);
        }

        // 트랜잭션 밖: 푸시 알림 전송 (Redis 호출 — DB 커넥션 미점유)
        sendPushNotificationsToOtherMembers(chatRoomId, senderId, notificationContent,
                result.senderNickname(), result.senderAvatarUrl(), result.members());

        customMetrics.stopMessageProcessingTimer(timerSample);
        return result;
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
        messageBroadcastService.broadcastToRedis(result.message(), result.senderNickname(), result.senderAvatarUrl(), result.members());
        return result.message();
    }

    @Override
    public Message sendTextMessageAndBroadcast(Long chatRoomId, Long senderId, String content) {
        SendResult result = sendMessageWithContext(chatRoomId, senderId, content);
        messageBroadcastService.broadcastToRedis(result.message(), result.senderNickname(), result.senderAvatarUrl(), result.members());
        return result.message();
    }

    /**
     * 채팅방의 다른 멤버들에게 푸시 알림을 전송한다.
     * 사전 조회된 sender와 members 정보를 사용하여 추가 DB 쿼리를 방지한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param senderId 발신자 ID
     * @param content 알림 내용
     * @param senderNickname 발신자 닉네임 (사전 조회됨)
     * @param senderAvatarUrl 발신자 프로필 이미지 URL (없으면 null)
     * @param members 채팅방 멤버 목록 (사전 조회됨)
     */
    private void sendPushNotificationsToOtherMembers(Long chatRoomId, Long senderId, String content,
                                                      String senderNickname, String senderAvatarUrl, List<ChatRoomMember> members) {
        // 채팅방의 다른 멤버들 중 현재 채팅방을 보고 있지 않은 사용자만 필터링
        // (채팅방에 있는 사용자는 WebSocket으로 실시간 메시지를 받으므로 푸시 불필요)
        List<Long> otherMemberIds = members.stream()
                .map(ChatRoomMember::getUserId)
                .filter(userId -> !userId.equals(senderId))
                .toList();

        // 배치 presence 조회 (Redis pipeline: 2N → 2회)
        Set<Long> activeUserIds = chatRoomPresenceTracker.getActiveUserIds(chatRoomId, otherMemberIds);

        List<Long> receiverUserIds = otherMemberIds.stream()
                .filter(userId -> !activeUserIds.contains(userId))
                .toList();

        // 벌크 푸시 알림 전송 (한 번의 호출로 처리)
        if (!receiverUserIds.isEmpty()) {
            notificationCommandPort.sendNewMessageNotificationBulk(
                    receiverUserIds,
                    senderNickname,
                    content,
                    chatRoomId,
                    senderAvatarUrl
            );
            log.debug("Push notification sent to {} users (excluded {} active users in room)",
                    receiverUserIds.size(),
                    members.size() - receiverUserIds.size() - 1);
        }
    }
}
