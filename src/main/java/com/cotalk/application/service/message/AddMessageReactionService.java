package com.cotalk.application.service.message;

import com.cotalk.domain.entity.Emoji;
import com.cotalk.domain.entity.Message;
import com.cotalk.domain.entity.MessageReaction;
import com.cotalk.domain.exception.MessageNotFoundException;
import com.cotalk.domain.port.inbound.message.AddMessageReactionUseCase;
import com.cotalk.domain.port.outbound.MessageReactionRepository;
import com.cotalk.domain.port.outbound.MessageRepository;
import com.cotalk.domain.validator.ChatRoomMemberValidator;
import com.cotalk.domain.validator.MessageValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 메시지 반응 추가 유스케이스 구현체.
 * 메시지에 이모지 반응을 추가한다.
 *
 * <p>동시성 제어:
 * <ul>
 *   <li>DB UNIQUE 제약 조건으로 중복 반응 방지</li>
 *   <li>DataIntegrityViolationException 발생 시 기존 반응 반환</li>
 * </ul>
 *
 * @author seunggu.lee
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AddMessageReactionService implements AddMessageReactionUseCase {

    private final MessageRepository messageRepository;
    private final MessageReactionRepository reactionRepository;
    private final MessageValidator messageValidator;
    private final ChatRoomMemberValidator chatRoomMemberValidator;

    /**
     * 메시지에 이모지 반응을 추가한다.
     * 이미 같은 반응이 있으면 기존 반응을 반환한다.
     *
     * <p>동시성 처리:
     * <ul>
     *   <li>UNIQUE 제약 조건 위반 시 기존 반응 조회하여 반환</li>
     *   <li>동시에 같은 반응 추가 시도해도 안전하게 처리</li>
     * </ul>
     *
     * @param messageId   메시지 ID
     * @param userId      사용자 ID
     * @param emojiString 이모지 문자열 (이모지 문자 또는 이름)
     * @return 생성된 또는 기존 반응 정보
     * @throws MessageNotFoundException 메시지가 존재하지 않는 경우
     */
    @Override
    public MessageReaction addReaction(Long messageId, Long userId, String emojiString) {
        // 메시지 존재 확인
        messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFoundException(messageId));

        // 이모지 유효성 검증 및 변환
        Emoji emoji = messageValidator.validateAndParseEmoji(emojiString);

        // 이미 같은 반응이 있는지 확인
        return reactionRepository.findByMessageIdAndUserIdAndEmoji(messageId, userId, emoji)
                .orElseGet(() -> createReactionSafely(messageId, userId, emoji));
    }

    /**
     * 메시지에 이모지 반응을 추가하고, 브로드캐스트에 필요한 chatRoomId를 함께 반환한다.
     *
     * @param messageId   메시지 ID
     * @param userId      사용자 ID
     * @param emojiString 이모지 문자열 (이모지 문자 또는 이름)
     * @return 반응 결과 (반응 + 채팅방 ID)
     * @throws MessageNotFoundException 메시지가 존재하지 않는 경우
     */
    @Override
    public ReactionResult addReactionWithContext(Long messageId, Long userId, String emojiString) {
        // 메시지 존재 확인 및 chatRoomId 조회
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFoundException(messageId));

        // 채팅방 멤버십 검증
        chatRoomMemberValidator.validateMembership(message.getChatRoomId(), userId);

        // 이모지 유효성 검증 및 변환
        Emoji emoji = messageValidator.validateAndParseEmoji(emojiString);

        // 이미 같은 반응이 있는지 확인
        MessageReaction reaction = reactionRepository.findByMessageIdAndUserIdAndEmoji(messageId, userId, emoji)
                .orElseGet(() -> createReactionSafely(messageId, userId, emoji));

        return new ReactionResult(reaction, message.getChatRoomId());
    }

    /**
     * 반응을 안전하게 생성한다.
     * 동시성으로 인한 중복 키 예외 발생 시 기존 반응을 반환한다.
     *
     * @param messageId 메시지 ID
     * @param userId    사용자 ID
     * @param emoji     이모지
     * @return 생성된 또는 기존 반응
     */
    private MessageReaction createReactionSafely(Long messageId, Long userId, Emoji emoji) {
        try {
            MessageReaction reaction = MessageReaction.create(messageId, userId, emoji);
            MessageReaction saved = reactionRepository.save(reaction);
            log.debug("Message reaction added: messageId={}, userId={}, emoji={}", messageId, userId, emoji);
            return saved;
        } catch (DataIntegrityViolationException e) {
            // 동시성으로 인한 중복 - 기존 반응 반환
            log.debug("Concurrent reaction detected, returning existing: messageId={}, userId={}, emoji={}",
                    messageId, userId, emoji);
            return reactionRepository.findByMessageIdAndUserIdAndEmoji(messageId, userId, emoji)
                    .orElseThrow(() -> new IllegalStateException(
                            "Reaction should exist after DataIntegrityViolationException"));
        }
    }
}
