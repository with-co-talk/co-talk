package com.cotalk.application.service.message;

import com.cotalk.domain.entity.Message;
import com.cotalk.domain.exception.MessageAccessDeniedException;
import com.cotalk.domain.exception.MessageNotFoundException;
import com.cotalk.domain.port.inbound.message.DeleteMessageUseCase;
import com.cotalk.domain.port.outbound.ChatMessageBroker;
import com.cotalk.domain.port.outbound.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 메시지 삭제 유스케이스 구현체.
 * 메시지를 소프트 삭제하고 실시간으로 채팅방 참여자들에게 알린다.
 *
 * @author seunggu.lee
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DeleteMessageService implements DeleteMessageUseCase {

    private final MessageRepository messageRepository;
    private final ChatMessageBroker chatMessageBroker;

    /**
     * 메시지를 삭제한다.
     * 본인이 보낸 메시지만 삭제할 수 있으며, 소프트 삭제 방식으로 처리된다.
     *
     * @param messageId 삭제할 메시지 ID
     * @param userId 요청 사용자 ID
     * @throws MessageNotFoundException 메시지가 존재하지 않는 경우
     * @throws MessageAccessDeniedException 본인이 보낸 메시지가 아니거나 이미 삭제된 경우
     */
    @Override
    public void deleteMessage(Long messageId, Long userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFoundException(messageId));

        // 본인이 보낸 메시지인지 확인
        if (!message.isSentBy(userId)) {
            throw MessageAccessDeniedException.notSender();
        }

        // 이미 삭제된 메시지인지 확인
        if (message.isDeleted()) {
            throw MessageAccessDeniedException.alreadyDeleted();
        }

        // 5분 초과 여부 확인
        if (message.isEditTimeExpired()) {
            throw MessageAccessDeniedException.timeExpired();
        }

        // 메시지 삭제 (소프트 삭제)
        message.delete();
        messageRepository.save(message);

        log.info("Message deleted: messageId={}, userId={}", messageId, userId);

        // 채팅방 참여자들에게 메시지 삭제 이벤트 브로드캐스트
        publishMessageDeletedEvent(message.getChatRoomId(), messageId, userId);
    }

    /**
     * 메시지 삭제 이벤트를 채팅방에 브로드캐스트한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param messageId  삭제된 메시지 ID
     * @param deletedBy  삭제한 사용자 ID
     */
    private void publishMessageDeletedEvent(Long chatRoomId, Long messageId, Long deletedBy) {
        String eventId = "message-deleted:" + chatRoomId + ":" + messageId;

        chatMessageBroker.publishRoomEvent(
                chatRoomId,
                new MessageDeletedEvent(
                        1,
                        eventId,
                        "MESSAGE_DELETED",
                        chatRoomId,
                        messageId,
                        deletedBy,
                        System.currentTimeMillis()
                )
        );

        log.debug("Message deleted event published: roomId={}, messageId={}", chatRoomId, messageId);
    }

    /**
     * 메시지 삭제 이벤트 DTO.
     * Redis Pub/Sub -> WebSocket 방 토픽(/topic/chat/room/{roomId})으로 전달되는 이벤트다.
     *
     * @param schemaVersion 스키마 버전
     * @param eventId       이벤트 고유 ID (중복 체크용)
     * @param eventType     이벤트 유형 (MESSAGE_DELETED)
     * @param chatRoomId    채팅방 ID
     * @param messageId     삭제된 메시지 ID
     * @param deletedBy     삭제한 사용자 ID
     * @param deletedAtMillis 삭제 시간 (밀리초)
     */
    private record MessageDeletedEvent(
            Integer schemaVersion,
            String eventId,
            String eventType,
            Long chatRoomId,
            Long messageId,
            Long deletedBy,
            Long deletedAtMillis
    ) {}
}
