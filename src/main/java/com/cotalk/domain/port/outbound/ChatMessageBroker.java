package com.cotalk.domain.port.outbound;

/**
 * 채팅 메시지 브로커 아웃바운드 포트
 * Redis Pub/Sub, Kafka 등 다양한 메시지 브로커로 교체 가능
 */
public interface ChatMessageBroker {

    /**
     * 채팅방에 메시지 발행
     *
     * @param roomId 채팅방 ID
     * @param message 브로드캐스트할 메시지
     */
    void publish(Long roomId, ChatBroadcastMessage message);

    /**
     * 브로드캐스트할 채팅 메시지
     */
    record ChatBroadcastMessage(
            Long messageId,
            Long senderId,
            Long roomId,
            String content,
            String type,
            Long createdAtMillis,
            String fileUrl,
            String fileName,
            Long fileSize,
            String contentType,
            String thumbnailUrl
    ) {}
}
