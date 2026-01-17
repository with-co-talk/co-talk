package com.cotalk.adapter.inbound.websocket;

import com.cotalk.domain.entity.Message;
import com.cotalk.domain.entity.MessageReaction;
import com.cotalk.domain.port.inbound.message.AddMessageReactionUseCase;
import com.cotalk.domain.port.inbound.message.RemoveMessageReactionUseCase;
import com.cotalk.domain.port.inbound.message.SendMessageUseCase;
import com.cotalk.domain.port.inbound.message.SendMessageUseCase.FileMessageCommand;
import com.cotalk.domain.port.outbound.ChatMessageBroker;
import com.cotalk.domain.port.outbound.ChatMessageBroker.ChatBroadcastMessage;
import com.cotalk.domain.port.outbound.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.time.ZoneId;

/**
 * WebSocket 기반 채팅 컨트롤러.
 *
 * <p>클라이언트로부터 WebSocket 메시지를 수신하여 처리하고,
 * Redis Pub/Sub을 통해 모든 서버 인스턴스로 브로드캐스트합니다.</p>
 *
 * <p>지원하는 기능:</p>
 * <ul>
 *     <li>텍스트 메시지 전송</li>
 *     <li>파일 메시지 전송</li>
 *     <li>메시지 반응(이모지) 추가/제거</li>
 * </ul>
 *
 * @author seunggu.lee
 * @see SendMessageUseCase
 * @see AddMessageReactionUseCase
 * @see RemoveMessageReactionUseCase
 * @see ChatMessageBroker
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final SendMessageUseCase sendMessageUseCase;
    private final ChatMessageBroker chatMessageBroker;
    private final AddMessageReactionUseCase addMessageReactionUseCase;
    private final RemoveMessageReactionUseCase removeMessageReactionUseCase;
    private final MessageRepository messageRepository;

    /**
     * 텍스트 채팅 메시지를 전송합니다.
     *
     * <p>클라이언트로부터 수신한 텍스트 메시지를 데이터베이스에 저장하고,
     * Redis Pub/Sub을 통해 해당 채팅방의 모든 참여자에게 브로드캐스트합니다.</p>
     *
     * @param request 채팅 메시지 요청 정보 (발신자 ID, 채팅방 ID, 메시지 내용)
     */
    @MessageMapping("/chat/message")
    public void sendMessage(@Payload ChatMessageRequest request) {
        log.debug("Received message from user {} to room {}", request.senderId(), request.roomId());

        // 메시지 저장
        Message savedMessage = sendMessageUseCase.sendMessage(
                request.senderId(),
                request.roomId(),
                request.content()
        );

        // Redis Pub/Sub을 통해 모든 서버로 브로드캐스트
        publishToRedis(savedMessage);
    }

    /**
     * 파일 첨부 메시지를 전송합니다.
     *
     * <p>클라이언트로부터 수신한 파일 메시지를 데이터베이스에 저장하고,
     * Redis Pub/Sub을 통해 해당 채팅방의 모든 참여자에게 브로드캐스트합니다.</p>
     *
     * <p>파일 정보에는 URL, 파일명, 크기, 컨텐츠 타입, 썸네일 URL이 포함됩니다.</p>
     *
     * @param request 파일 메시지 요청 정보
     */
    @MessageMapping("/chat/message/file")
    public void sendFileMessage(@Payload FileMessageRequest request) {
        log.debug("Received file message from user {} to room {}", request.senderId(), request.roomId());

        // 파일 메시지 저장
        FileMessageCommand command = new FileMessageCommand(
                request.fileUrl(),
                request.fileName(),
                request.fileSize(),
                request.contentType(),
                request.thumbnailUrl()
        );

        Message savedMessage = sendMessageUseCase.sendFileMessage(
                request.roomId(),
                request.senderId(),
                command
        );

        // Redis Pub/Sub을 통해 모든 서버로 브로드캐스트
        publishToRedis(savedMessage);
    }

    /**
     * 메시지를 Redis Pub/Sub으로 발행
     * Redis Subscriber가 이를 수신하여 WebSocket으로 전달
     */
    private void publishToRedis(Message message) {
        ChatBroadcastMessage broadcastMessage = new ChatBroadcastMessage(
                message.getId(),
                message.getSenderId(),
                message.getChatRoomId(),
                message.getContent(),
                message.getType().name(),
                message.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                message.getFileUrl(),
                message.getFileName(),
                message.getFileSize(),
                message.getFileContentType(),
                message.getThumbnailUrl()
        );

        chatMessageBroker.publish(message.getChatRoomId(), broadcastMessage);
    }

    /**
     * 메시지에 반응(이모지)을 추가합니다.
     *
     * <p>사용자가 특정 메시지에 이모지 반응을 추가하면 데이터베이스에 저장하고,
     * Redis Pub/Sub을 통해 해당 채팅방의 모든 참여자에게 반응 추가 이벤트를 브로드캐스트합니다.</p>
     *
     * @param request 반응 추가 요청 정보 (메시지 ID, 사용자 ID, 이모지)
     */
    @MessageMapping("/chat/reaction/add")
    public void addReaction(@Payload AddReactionRequest request) {
        log.debug("Received reaction add from user {} to message {}", request.userId(), request.messageId());

        MessageReaction reaction = addMessageReactionUseCase.addReaction(
                request.messageId(),
                request.userId(),
                request.emoji()
        );

        // 반응 추가 이벤트를 Redis Pub/Sub으로 브로드캐스트
        publishReactionEvent(reaction, "ADDED");
    }

    /**
     * 메시지에서 반응(이모지)을 제거합니다.
     *
     * <p>사용자가 특정 메시지에서 이모지 반응을 제거하면 데이터베이스에서 삭제하고,
     * Redis Pub/Sub을 통해 해당 채팅방의 모든 참여자에게 반응 제거 이벤트를 브로드캐스트합니다.</p>
     *
     * @param request 반응 제거 요청 정보 (메시지 ID, 사용자 ID, 이모지)
     */
    @MessageMapping("/chat/reaction/remove")
    public void removeReaction(@Payload RemoveReactionRequest request) {
        log.debug("Received reaction remove from user {} to message {}", request.userId(), request.messageId());

        removeMessageReactionUseCase.removeReaction(
                request.messageId(),
                request.userId(),
                request.emoji()
        );

        // 반응 제거 이벤트를 Redis Pub/Sub으로 브로드캐스트
        MessageReaction removedReaction = MessageReaction.builder()
                .messageId(request.messageId())
                .userId(request.userId())
                .emoji(request.emoji())
                .build();
        publishReactionEvent(removedReaction, "REMOVED");
    }

    /**
     * 메시지 반응 이벤트를 Redis Pub/Sub으로 발행
     */
    private void publishReactionEvent(MessageReaction reaction, String eventType) {
        // 메시지의 채팅방 ID 조회
        Long chatRoomId = messageRepository.findById(reaction.getMessageId())
                .map(Message::getChatRoomId)
                .orElse(null);

        if (chatRoomId == null) {
            log.warn("Cannot find chat room for message: {}", reaction.getMessageId());
            return;
        }

        ReactionBroadcastMessage broadcastMessage = new ReactionBroadcastMessage(
                reaction.getId(),
                reaction.getMessageId(),
                reaction.getUserId(),
                reaction.getEmoji(),
                eventType,
                reaction.getCreatedAt() != null 
                    ? reaction.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    : System.currentTimeMillis()
        );

        // 메시지가 속한 채팅방으로 브로드캐스트
        // 채팅방 ID를 사용하여 브로드캐스트
        chatMessageBroker.publishReaction(chatRoomId, broadcastMessage);
    }

    // Request DTOs

    /**
     * 텍스트 채팅 메시지 전송 요청 DTO.
     *
     * @param senderId 발신자 사용자 ID
     * @param roomId   채팅방 ID
     * @param content  메시지 내용
     */
    public record ChatMessageRequest(Long senderId, Long roomId, String content) {}

    /**
     * 파일 첨부 메시지 전송 요청 DTO.
     *
     * @param senderId     발신자 사용자 ID
     * @param roomId       채팅방 ID
     * @param fileUrl      업로드된 파일의 URL
     * @param fileName     파일명
     * @param fileSize     파일 크기 (바이트)
     * @param contentType  파일의 MIME 타입
     * @param thumbnailUrl 썸네일 이미지 URL (이미지/동영상 파일인 경우)
     */
    public record FileMessageRequest(
            Long senderId,
            Long roomId,
            String fileUrl,
            String fileName,
            Long fileSize,
            String contentType,
            String thumbnailUrl
    ) {}

    /**
     * 메시지 반응 추가 요청 DTO.
     *
     * @param messageId 반응을 추가할 메시지 ID
     * @param userId    반응을 추가하는 사용자 ID
     * @param emoji     이모지 문자열
     */
    public record AddReactionRequest(Long messageId, Long userId, String emoji) {}

    /**
     * 메시지 반응 제거 요청 DTO.
     *
     * @param messageId 반응을 제거할 메시지 ID
     * @param userId    반응을 제거하는 사용자 ID
     * @param emoji     제거할 이모지 문자열
     */
    public record RemoveReactionRequest(Long messageId, Long userId, String emoji) {}

    /**
     * 메시지 반응 브로드캐스트 메시지 DTO.
     *
     * <p>Redis Pub/Sub을 통해 모든 서버 인스턴스로 전파되는 반응 이벤트 메시지입니다.</p>
     *
     * @param reactionId 반응 ID
     * @param messageId  대상 메시지 ID
     * @param userId     반응한 사용자 ID
     * @param emoji      이모지 문자열
     * @param eventType  이벤트 타입 ("ADDED" 또는 "REMOVED")
     * @param timestamp  이벤트 발생 시간 (Unix timestamp, 밀리초)
     */
    public record ReactionBroadcastMessage(
            Long reactionId,
            Long messageId,
            Long userId,
            String emoji,
            String eventType,
            long timestamp
    ) {}
}
