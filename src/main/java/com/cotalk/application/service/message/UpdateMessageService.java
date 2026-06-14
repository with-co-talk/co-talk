package com.cotalk.application.service.message;

import com.cotalk.domain.entity.Message;
import com.cotalk.domain.exception.MessageNotFoundException;
import com.cotalk.domain.exception.ResourceAccessDeniedException;
import com.cotalk.domain.port.inbound.message.UpdateMessageUseCase;
import com.cotalk.domain.port.outbound.BlindIndexTokenizer;
import com.cotalk.domain.port.outbound.ChatMessageBroker;
import com.cotalk.domain.port.outbound.MessageRepository;
import com.cotalk.domain.port.outbound.MessageSearchTokenRepository;
import com.cotalk.domain.port.outbound.TimeProvider;
import com.cotalk.domain.util.HtmlSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;

/**
 * 메시지 수정 유스케이스 구현체.
 * 메시지 내용을 수정한다.
 *
 * @author seunggu.lee
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UpdateMessageService implements UpdateMessageUseCase {

    private final MessageRepository messageRepository;
    private final MessageSearchTokenRepository messageSearchTokenRepository;
    private final BlindIndexTokenizer blindIndexTokenizer;
    private final ChatMessageBroker chatMessageBroker;
    private final TimeProvider timeProvider;

    /**
     * 메시지 내용을 수정한다.
     * 본인이 보낸 메시지만 수정할 수 있으며, 삭제된 메시지는 수정할 수 없다.
     *
     * @param messageId 수정할 메시지 ID
     * @param userId 요청 사용자 ID
     * @param newContent 새로운 메시지 내용
     * @return 수정된 메시지
     * @throws MessageNotFoundException 메시지가 존재하지 않는 경우
     * @throws ResourceAccessDeniedException 본인이 보낸 메시지가 아니거나 이미 삭제된 경우
     */
    @Override
    public Message updateMessage(Long messageId, Long userId, String newContent) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFoundException(messageId));

        // 본인이 보낸 메시지인지 확인
        if (!message.isSentBy(userId)) {
            throw ResourceAccessDeniedException.messageNotSender();
        }

        // 삭제된 메시지인지 확인
        if (message.isDeleted()) {
            throw ResourceAccessDeniedException.messageAlreadyDeleted();
        }

        // 5분 초과 여부 확인
        if (message.isEditTimeExpired(timeProvider.now())) {
            throw ResourceAccessDeniedException.messageTimeExpired();
        }

        // XSS 방지 + 메시지 수정
        message.updateContent(HtmlSanitizer.stripAllTags(newContent));
        Message updated = messageRepository.save(message);

        // 블라인드 인덱스 재토큰화 (delete-then-insert) — 수정된 본문 기준으로 검색 토큰 갱신.
        // @Transactional 경계 안에서 수행되어 본문/토큰이 일관되게 커밋된다. TEXT만 수정 가능하므로 항상 토큰화 대상.
        messageSearchTokenRepository.deleteByMessageId(updated.getId());
        messageSearchTokenRepository.saveTokens(updated.getId(), blindIndexTokenizer.tokenize(updated.getContent()));

        log.info("Message updated: messageId={}, userId={}", messageId, userId);

        // 채팅방 참여자들에게 메시지 수정 이벤트 브로드캐스트
        chatMessageBroker.publishRoomEvent(
                updated.getChatRoomId(),
                new MessageUpdatedEvent(
                        1,
                        "message-updated:" + updated.getChatRoomId() + ":" + updated.getId(),
                        "MESSAGE_UPDATED",
                        updated.getChatRoomId(),
                        updated.getId(),
                        userId,
                        updated.getContent(),
                        timeProvider.now().toInstant(ZoneOffset.UTC).toEpochMilli()
                )
        );

        return updated;
    }

    /**
     * 메시지 수정 이벤트 DTO.
     */
    private record MessageUpdatedEvent(
            Integer schemaVersion,
            String eventId,
            String eventType,
            Long chatRoomId,
            Long messageId,
            Long updatedBy,
            String newContent,
            Long updatedAtMillis
    ) {}
}
