package com.cotalk.adapter.inbound.websocket;

import com.cotalk.domain.entity.Message;
import com.cotalk.domain.entity.MessageReaction;
import com.cotalk.domain.port.inbound.AddMessageReactionUseCase;
import com.cotalk.domain.port.inbound.RemoveMessageReactionUseCase;
import com.cotalk.domain.port.inbound.SendMessageUseCase;
import com.cotalk.domain.port.inbound.SendMessageUseCase.FileMessageCommand;
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
 * WebSocket 채팅 컨트롤러
 * 메시지를 저장하고 Redis Pub/Sub을 통해 모든 서버로 브로드캐스트
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
    public record ChatMessageRequest(Long senderId, Long roomId, String content) {}

    public record FileMessageRequest(
            Long senderId,
            Long roomId,
            String fileUrl,
            String fileName,
            Long fileSize,
            String contentType,
            String thumbnailUrl
    ) {}

    public record AddReactionRequest(Long messageId, Long userId, String emoji) {}

    public record RemoveReactionRequest(Long messageId, Long userId, String emoji) {}

    public record ReactionBroadcastMessage(
            Long reactionId,
            Long messageId,
            Long userId,
            String emoji,
            String eventType,
            long timestamp
    ) {}
}
