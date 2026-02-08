package com.cotalk.adapter.inbound.rest.dto.message;

import com.cotalk.domain.entity.MessageReaction;

import java.time.LocalDateTime;

/**
 * 메시지 반응 응답 DTO.
 * 개별 반응 레코드를 나타낸다.
 *
 * @param reactionId     반응 ID
 * @param messageId      메시지 ID
 * @param userId         사용자 ID
 * @param emoji          이모지 유니코드 문자 (Flutter 클라이언트와 동일한 형식)
 * @param emojiCharacter 이모지 유니코드 문자 (하위 호환)
 * @param emojiName      이모지 enum 이름 (하위 호환)
 * @param createdAt      생성 일시
 * @author seunggu.lee
 */
public record MessageReactionResponse(
        Long reactionId,
        Long messageId,
        Long userId,
        String emoji,
        String emojiCharacter,
        String emojiName,
        LocalDateTime createdAt
) {

    /**
     * MessageReaction 엔티티로부터 응답 DTO를 생성합니다.
     *
     * @param reaction MessageReaction 엔티티
     * @return MessageReactionResponse 인스턴스
     */
    public static MessageReactionResponse from(MessageReaction reaction) {
        return new MessageReactionResponse(
                reaction.getId(),
                reaction.getMessageId(),
                reaction.getUserId(),
                reaction.getEmoji().getCharacter(),
                reaction.getEmoji().getCharacter(),
                reaction.getEmoji().name(),
                reaction.getCreatedAt()
        );
    }
}
