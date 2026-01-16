package com.cotalk.adapter.inbound.websocket;

import com.cotalk.domain.entity.Message;
import com.cotalk.domain.port.inbound.SendMessageUseCase;
import com.cotalk.domain.port.inbound.SendMessageUseCase.FileMessageCommand;
import com.cotalk.domain.port.outbound.ChatMessageBroker;
import com.cotalk.domain.port.outbound.ChatMessageBroker.ChatBroadcastMessage;
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
}
