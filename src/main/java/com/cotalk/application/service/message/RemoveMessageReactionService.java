package com.cotalk.application.service.message;

import com.cotalk.domain.entity.Emoji;
import com.cotalk.domain.entity.Message;
import com.cotalk.domain.entity.MessageReaction;
import com.cotalk.domain.exception.MessageReactionNotFoundException;
import com.cotalk.domain.port.inbound.message.RemoveMessageReactionUseCase;
import com.cotalk.domain.port.outbound.MessageReactionRepository;
import com.cotalk.domain.port.outbound.MessageRepository;
import com.cotalk.domain.validator.MessageValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 메시지 반응 삭제 유스케이스 구현체.
 * 메시지에서 이모지 반응을 삭제한다.
 *
 * @author seunggu.lee
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RemoveMessageReactionService implements RemoveMessageReactionUseCase {

    private final MessageReactionRepository reactionRepository;
    private final MessageRepository messageRepository;
    private final MessageValidator messageValidator;

    /**
     * 메시지에서 이모지 반응을 삭제한다.
     *
     * @param messageId 메시지 ID
     * @param userId 사용자 ID
     * @param emojiString 삭제할 이모지 문자열 (이모지 문자 또는 이름)
     * @throws MessageReactionNotFoundException 해당 반응이 존재하지 않는 경우
     */
    @Override
    public void removeReaction(Long messageId, Long userId, String emojiString) {
        // 이모지 유효성 검증 및 변환
        Emoji emoji = messageValidator.validateAndParseEmoji(emojiString);

        MessageReaction reaction = reactionRepository.findByMessageIdAndUserIdAndEmoji(messageId, userId, emoji)
                .orElseThrow(() -> new MessageReactionNotFoundException(messageId, userId, emojiString));

        reactionRepository.delete(reaction);
        log.info("Message reaction removed: messageId={}, userId={}, emoji={}", messageId, userId, emoji);
    }

    /**
     * 메시지에서 이모지 반응을 삭제하고, 브로드캐스트에 필요한 chatRoomId를 반환한다.
     *
     * @param messageId   메시지 ID
     * @param userId      사용자 ID
     * @param emojiString 삭제할 이모지 문자열 (이모지 문자 또는 이름)
     * @return 메시지가 속한 채팅방 ID (메시지를 찾을 수 없는 경우 null)
     * @throws MessageReactionNotFoundException 해당 반응이 존재하지 않는 경우
     */
    @Override
    public Long removeReactionWithContext(Long messageId, Long userId, String emojiString) {
        removeReaction(messageId, userId, emojiString);
        return messageRepository.findById(messageId)
                .map(Message::getChatRoomId)
                .orElse(null);
    }
}
