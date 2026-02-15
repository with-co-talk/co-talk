package com.cotalk.infrastructure.messaging;

import com.cotalk.domain.port.outbound.ChatMessageBroker.ChatBroadcastMessage;
import com.cotalk.domain.port.outbound.ChatMessageBroker.ReactionBroadcastEvent;
import com.cotalk.domain.util.HtmlSanitizer;
import com.cotalk.infrastructure.config.properties.AppProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Redis Pub/Sub 메시지 구독자.
 * Redis 채널에서 채팅 메시지를 수신하여 WebSocket을 통해 클라이언트에게 브로드캐스트한다.
 *
 * <p>이 컴포넌트는 {@code spring.data.redis.enabled=true}일 때만 활성화된다.</p>
 *
 * @author seunggu.lee
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = true)
public class RedisChatMessageSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final String channelPrefix;
    private static final String ROOM_TOPIC_PREFIX = "/topic/chat/room/";

    public RedisChatMessageSubscriber(
            SimpMessagingTemplate messagingTemplate,
            ObjectMapper objectMapper,
            AppProperties appProperties) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
        this.channelPrefix = appProperties.redis().channelPrefix();
    }

    /**
     * Redis로부터 메시지를 수신하여 처리한다.
     * 수신된 JSON 메시지를 역직렬화하고 WebSocket으로 브로드캐스트한다.
     *
     * @param message Redis로부터 수신한 메시지
     * @param pattern 매칭된 채널 패턴 (바이트 배열)
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel());
            String jsonMessage = new String(message.getBody());

            RoomChannelInfo channelInfo = RoomChannelInfo.parse(channelPrefix, channel);
            if (channelInfo == null) {
                log.warn("Invalid chat room channel format: {}", channel);
                return;
            }

            // suffix 기반 분기:
            // - chat:room:{roomId}            -> 채팅 메시지
            // - chat:room:{roomId}:reaction   -> 리액션 이벤트
            // - chat:room:{roomId}:event      -> 채팅방 이벤트(READ 등)
            if (channelInfo.isReactionChannel()) {
                handleReaction(channelInfo.roomId(), jsonMessage);
                return;
            }
            if (channelInfo.isEventChannel()) {
                handleRoomEvent(channelInfo.roomId(), jsonMessage);
                return;
            }

            ChatBroadcastMessage chatMessage = objectMapper.readValue(jsonMessage, ChatBroadcastMessage.class);
            WebSocketChatMessage wsMessage = toWebSocketMessage(chatMessage);
            String destination = ROOM_TOPIC_PREFIX + chatMessage.roomId();
            messagingTemplate.convertAndSend(destination, wsMessage);
            log.debug("Broadcasted message to WebSocket: destination={}, messageId={}",
                    destination, chatMessage.messageId());

        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize chat message from Redis", e);
        } catch (Exception e) {
            log.error("Failed to process chat message from Redis", e);
        }
    }

    private void handleReaction(Long roomId, String jsonMessage) throws JsonProcessingException {
        ReactionBroadcastEvent reactionEvent = objectMapper.readValue(jsonMessage, ReactionBroadcastEvent.class);
        String destination = ROOM_TOPIC_PREFIX + roomId;
        messagingTemplate.convertAndSend(destination, reactionEvent);
        log.debug("Broadcasted reaction to WebSocket: destination={}, messageId={}",
                destination, reactionEvent.messageId());
    }

    private void handleRoomEvent(Long roomId, String jsonMessage) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(jsonMessage);
        String eventType = root.has("eventType") ? root.get("eventType").asText() : null;

        String destination = ROOM_TOPIC_PREFIX + roomId;
        if ("MESSAGE_DELETED".equals(eventType)) {
            MessageDeletedEventMessage event =
                    objectMapper.treeToValue(root, MessageDeletedEventMessage.class);
            messagingTemplate.convertAndSend(destination, event);
            log.debug("Broadcasted message deleted event to WebSocket: destination={}, messageId={}",
                    destination, event.messageId());
        } else if ("MESSAGE_UPDATED".equals(eventType)) {
            MessageUpdatedEventMessage event =
                    objectMapper.treeToValue(root, MessageUpdatedEventMessage.class);
            messagingTemplate.convertAndSend(destination, event);
            log.debug("Broadcasted message updated event to WebSocket: destination={}, messageId={}",
                    destination, event.messageId());
        } else if ("TYPING".equals(eventType) || "STOP_TYPING".equals(eventType)) {
            TypingEventMessage event = objectMapper.treeToValue(root, TypingEventMessage.class);
            messagingTemplate.convertAndSend(destination, event);
            log.debug("Broadcasted typing event to WebSocket: destination={}, userId={}",
                    destination, event.userId());
        } else if ("LINK_PREVIEW_UPDATED".equals(eventType)) {
            LinkPreviewUpdatedEventMessage event =
                    objectMapper.treeToValue(root, LinkPreviewUpdatedEventMessage.class);
            messagingTemplate.convertAndSend(destination, event);
            log.debug("Broadcasted link preview updated event to WebSocket: destination={}, messageId={}",
                    destination, event.messageId());
        } else {
            ChatRoomEventMessage event = objectMapper.treeToValue(root, ChatRoomEventMessage.class);
            messagingTemplate.convertAndSend(destination, event);
            log.debug("Broadcasted room event to WebSocket: destination={}, eventType={}",
                    destination, event.eventType());
        }
    }

    /**
     * ChatBroadcastMessage를 WebSocket 전송용 메시지로 변환한다.
     *
     * @param msg 변환할 채팅 브로드캐스트 메시지
     * @return WebSocket 전송용 채팅 메시지
     */
    private WebSocketChatMessage toWebSocketMessage(ChatBroadcastMessage msg) {
        LocalDateTime createdAt = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(msg.createdAtMillis()),
                ZoneOffset.UTC
        );

        return new WebSocketChatMessage(
                1,
                "message:" + msg.messageId() + ":" + System.currentTimeMillis(),
                msg.messageId(),
                msg.senderId(),
                msg.senderNickname(),
                msg.senderAvatarUrl(),
                msg.roomId(),
                // 과거 데이터 호환: 저장된 HTML 엔티티를 복원해 클라이언트에 원문으로 보여준다.
                HtmlSanitizer.unescape(msg.content()),
                msg.type(),
                createdAt,
                msg.fileUrl(),
                msg.fileName(),
                msg.fileSize(),
                msg.fileContentType(),
                msg.thumbnailUrl(),
                msg.unreadCount(),
                msg.eventType(),
                msg.relatedUserId(),
                msg.relatedUserNickname()
        );
    }

    /**
     * WebSocket으로 전송할 채팅 메시지 DTO.
     *
     * @param messageId        메시지 ID
     * @param senderId         발신자 ID
     * @param senderNickname   발신자 닉네임
     * @param senderAvatarUrl  발신자 프로필 이미지 URL
     * @param roomId           채팅방 ID
     * @param content          메시지 내용
     * @param type             메시지 타입
     * @param createdAt        생성 일시
     * @param fileUrl          파일 URL (파일 메시지인 경우)
     * @param fileName         파일명 (파일 메시지인 경우)
     * @param fileSize         파일 크기 (파일 메시지인 경우)
     * @param fileContentType  컨텐츠 타입 (파일 메시지인 경우)
     * @param thumbnailUrl     썸네일 URL (이미지 메시지인 경우)
     * @param unreadCount      읽지 않은 멤버 수 (발신자 제외)
     * @param eventType        이벤트 유형 (USER_LEFT, USER_JOINED 등, 시스템 메시지인 경우)
     * @param relatedUserId    관련 사용자 ID (나간 사용자, 참여한 사용자 등)
     * @param relatedUserNickname 관련 사용자 닉네임
     */
    public record WebSocketChatMessage(
            Integer schemaVersion,
            String eventId,
            Long messageId,
            Long senderId,
            String senderNickname,
            String senderAvatarUrl,
            Long roomId,
            String content,
            String type,
            LocalDateTime createdAt,
            String fileUrl,
            String fileName,
            Long fileSize,
            String fileContentType,
            String thumbnailUrl,
            Integer unreadCount,
            String eventType,
            Long relatedUserId,
            String relatedUserNickname
    ) {}

    /**
     * 채팅방 이벤트 메시지 DTO.
     * 채팅방 단위 토픽(/topic/chat/room/{roomId})으로 발행되는 이벤트(READ 등)에 사용한다.
     *
     * @param eventType  이벤트 타입 (예: READ)
     * @param chatRoomId 채팅방 ID
     * @param userId     이벤트를 발생시킨 사용자 ID (예: reader)
     * @param lastReadAt 마지막 읽은 시간 (optional)
     */
    public record ChatRoomEventMessage(
            Integer schemaVersion,
            String eventId,
            String eventType,
            Long chatRoomId,
            Long userId,
            Long lastReadMessageId,
            LocalDateTime lastReadAt
    ) {}

    /**
     * 타이핑 이벤트 DTO.
     * 채팅방 내 사용자 타이핑 상태를 클라이언트에 전달한다.
     *
     * @param schemaVersion 스키마 버전
     * @param eventId       이벤트 ID (중복 체크용)
     * @param eventType     이벤트 타입 (TYPING, STOP_TYPING)
     * @param chatRoomId    채팅방 ID
     * @param userId        타이핑 중인 사용자 ID
     * @param userNickname  타이핑 중인 사용자 닉네임
     * @param isTyping      타이핑 중 여부
     */
    public record TypingEventMessage(
            Integer schemaVersion,
            String eventId,
            String eventType,
            Long chatRoomId,
            Long userId,
            String userNickname,
            Boolean isTyping
    ) {}

    /**
     * 메시지 삭제 이벤트 DTO.
     * DeleteMessageService에서 발행하는 MESSAGE_DELETED 이벤트와 동일한 형식이다.
     * 클라이언트가 실시간으로 삭제된 메시지를 반영할 수 있도록 messageId를 포함해 전달한다.
     *
     * @param eventType     이벤트 타입 (MESSAGE_DELETED)
     * @param chatRoomId    채팅방 ID
     * @param messageId     삭제된 메시지 ID
     * @param deletedBy     삭제한 사용자 ID
     * @param deletedAtMillis 삭제 시간 (밀리초)
     */
    public record MessageDeletedEventMessage(
            Integer schemaVersion,
            String eventId,
            String eventType,
            Long chatRoomId,
            Long messageId,
            Long deletedBy,
            Long deletedAtMillis
    ) {}

    /**
     * 메시지 수정 이벤트 DTO.
     * UpdateMessageService에서 발행하는 MESSAGE_UPDATED 이벤트와 동일한 형식이다.
     * 클라이언트가 실시간으로 수정된 메시지를 반영할 수 있도록 필드를 포함해 전달한다.
     *
     * @param eventType      이벤트 타입 (MESSAGE_UPDATED)
     * @param chatRoomId     채팅방 ID
     * @param messageId      수정된 메시지 ID
     * @param updatedBy      수정한 사용자 ID
     * @param newContent     수정된 메시지 내용
     * @param updatedAtMillis 수정 시간 (밀리초)
     */
    public record MessageUpdatedEventMessage(
            Integer schemaVersion,
            String eventId,
            String eventType,
            Long chatRoomId,
            Long messageId,
            Long updatedBy,
            String newContent,
            Long updatedAtMillis
    ) {}

    /**
     * 링크 미리보기 업데이트 이벤트 DTO.
     * MessageLinkPreviewService에서 링크 미리보기를 수집하여 저장한 후 발행한다.
     * 클라이언트가 실시간으로 링크 미리보기를 표시할 수 있도록 메타데이터를 전달한다.
     *
     * @param schemaVersion           스키마 버전
     * @param eventId                 이벤트 ID (중복 체크용)
     * @param eventType               이벤트 타입 (LINK_PREVIEW_UPDATED)
     * @param chatRoomId              채팅방 ID
     * @param messageId               메시지 ID
     * @param linkPreviewUrl          링크 미리보기 URL
     * @param linkPreviewTitle        링크 미리보기 제목
     * @param linkPreviewDescription  링크 미리보기 설명
     * @param linkPreviewImageUrl     링크 미리보기 이미지 URL
     */
    public record LinkPreviewUpdatedEventMessage(
            Integer schemaVersion,
            String eventId,
            String eventType,
            Long chatRoomId,
            Long messageId,
            String linkPreviewUrl,
            String linkPreviewTitle,
            String linkPreviewDescription,
            String linkPreviewImageUrl
    ) {}

    /**
     * chat:room:{roomId}(:suffix) 채널 파서.
     */
    private record RoomChannelInfo(Long roomId, String suffix) {
        static RoomChannelInfo parse(String prefix, String fullChannel) {
            if (fullChannel == null || prefix == null || !fullChannel.startsWith(prefix)) {
                return null;
            }
            String rest = fullChannel.substring(prefix.length()); // e.g. "10" or "10:reaction"
            String[] parts = rest.split(":");
            if (parts.length == 0) return null;
            Long roomId;
            try {
                roomId = Long.parseLong(parts[0]);
            } catch (NumberFormatException e) {
                return null;
            }
            String suffix = parts.length >= 2 ? parts[1] : null;
            return new RoomChannelInfo(roomId, suffix);
        }

        boolean isReactionChannel() {
            return "reaction".equals(suffix);
        }

        boolean isEventChannel() {
            return "event".equals(suffix);
        }
    }
}
